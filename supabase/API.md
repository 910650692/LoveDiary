# Ours · Supabase API 约定（给 iOS 端）

> 项目：LoveDiary（app 名 Ours）
> Project Ref：`prkijbbjrjfmkolkjxsk`
> 基础 URL：`https://prkijbbjrjfmkolkjxsk.supabase.co`
> 版本对应：supabase CLI 2.111.0，Postgres 17

## 总览

没有自建后端。iOS 端用 **supabase-swift SDK** 调三类接口：

1. **Auth**：`/auth/v1/*` — 注册、登录、登出、获取当前用户
2. **REST**：`/rest/v1/*` — 直接操作表（PostgREST 自动生成），RLS 保证两人只见彼此数据
3. **RPC**：`/rest/v1/rpc/*` — 调用数据库函数（业务逻辑，客户端不能直接改表的部分）

请求头统一：`apikey: <anon key>` + `Authorization: Bearer <用户 JWT>`。

## Auth（阶段 1）

| 操作 | 端点 | 说明 |
|---|---|---|
| 注册 | `POST /auth/v1/signup` `{email, password, data:{nickname}}` | 注册后触发器自动建 profile |
| 登录 | `POST /auth/v1/token?grant_type=password` `{email, password}` | 返回 `access_token` |
| 登出 | `POST /auth/v1/logout` | 吊销当前 token |
| 当前用户 | `GET /auth/v1/user` | |

注册/登录后客户端需要：
1. 查自己的 `profiles` 行（应已自动创建）
2. 查 `couples?user_a_id=eq.<自己id> OR user_b_id=eq.<自己id>` → 有记录说明已绑定，进主页；没有 → 引导创建/输入邀请码

## RPC 函数

### `create_couple(p_started_date date)` → text
- 调用方：**未绑定**的用户（成为 user_a，即创建者）
- 行为：校验没在关系中 → 建 couples 行 → 生成 6 位邀请码（有效期 7 天）→ 返回邀请码
- 调用：`POST /rest/v1/rpc/create_couple` `{"p_started_date":"2023-05-20"}` → 响应 `"M9WN8F"`

### `join_couple(p_code text)` → uuid
- 调用方：**未绑定**的用户（成为 user_b）
- 行为：事务内校验邀请码（有效/未过期/未被用）→ 绑定 → 标记已用 → 返回 couple_id
- 调用：`POST /rest/v1/rpc/join_couple` `{"p_code":"M9WN8F"}` → 响应 `"fe9409d1-..."`

错误统一返回 HTTP 4xx + 中文错误信息（`你已经在一段关系里了` / `邀请码无效` / `邀请码已过期` / `邀请码已被使用` / `这段关系已经满员` / `恋爱开始日期无效`）。

## 表

### profiles（用户资料）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | uuid | = auth.users.id |
| nickname | text | 注册时从 user_metadata 带过来 |
| avatar_url | text | 照片功能上线后填 OSS 地址 |
| created_at | timestamptz | |

RLS：能查**自己 + 伴侣**（绑定后）。只有自己能改自己的资料。

### couples（一对情侣）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | uuid | |
| user_a_id | uuid | 创建者 |
| user_b_id | uuid | 邀请码加入者（绑定前为 null） |
| started_date | date | 恋爱开始日期 → 主页算「在一起天数」 |
| created_at | timestamptz | |

主页数据来源：`started_date` 距今天数 = 在一起天数；`started_date` 周年 = 下个纪念日（阶段 2 自定义纪念日上线后，主页倒计时优先取 anniversaries 里最近的一个）。

### anniversaries（重要日期，阶段 2）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | uuid | |
| couple_id | uuid | |
| created_by | uuid | 谁添加的（推送通知判断用） |
| name | text ≤50 | 纪念日/生日名称 |
| date | date | |
| notes | text ≤500 | 备注，可空 |
| enable_notification | bool | 默认 true，阶段 3 推送用 |
| created_at / updated_at | timestamptz | |

### wishlist_items（共同心愿单，阶段 2）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | uuid | |
| couple_id | uuid | |
| creator_id | uuid | |
| completer_id | uuid | 完成者，可空 |
| title | text ≤200 | |
| description | text ≤1000 | 可空 |
| status | text | `PENDING` / `COMPLETED` |
| completed_at | timestamptz | 可空 |
| is_deleted | bool | 软删除，列表查询默认过滤 `is_deleted=eq.false` |
| created_at / updated_at | timestamptz | |

### 通用约定
- 所有表 RLS：只有 couple 双方可读可写，**RLS 是唯一数据隔离手段**，客户端不用自己过滤
- 查询举例（PostgREST 语法，SDK 里等价写法）：
  - `GET /rest/v1/wishlist_items?couple_id=eq.<id>&status=eq.PENDING&is_deleted=eq.false&order=created_at.asc`
  - `PATCH /rest/v1/wishlist_items?id=eq.<id>` 完成时带 `{status:"COMPLETED", completer_id:"<我>", completed_at:"<now>"}`
  - 软删除 = `PATCH {is_deleted:true}`；永久删除才用 `DELETE`

## 环境变量（Mac 构建时用）

```
SUPABASE_URL=https://prkijbbjrjfmkolkjxsk.supabase.co
SUPABASE_ANON_KEY=<anon key>   # supabase projects api-keys 可查
```

## 尚未实现（后续阶段）

- **阶段 3 推送**：OneSignal + APNs；Edge Function 定时触发（纪念日提醒/心愿新增/随机回忆），数据库变更订阅或 pg_cron
- **阶段 4 照片**：阿里云 OSS，Edge Function 签发 STS 临时凭证，`avatar_url`/相册存 OSS key
- 对应表（albums/photos/tags/photo_tag）旧 Java 后端 `backend/database_setup.sql` 有设计，届时参考改编
