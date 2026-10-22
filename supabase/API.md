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

## 推送（阶段 3，服务端已就绪）

- **服务商**：OneSignal（免费档）。密钥存于 Supabase secrets：`ONESIGNAL_APP_ID` / `ONESIGNAL_API_KEY` / `OUR_INTERNAL_SECRET`（内部函数鉴权，与迁移文件保持一致）
- **Edge Function**（`supabase/functions/`，已部署，verify_jwt=false，靠 `OUR_INTERNAL_SECRET` 鉴权）：
  - `send-push` — 通用推送入口：`POST /functions/v1/send-push` `{userIds:[uuid], title, body, data?}`，内部/触发器调用
  - `anniversary-reminder` — 纪念日提醒：pg_cron 每天 23:00 UTC（北京时间 07:00）触发，查今天/明天的 anniversaries（enable_notification=true）+ 恋爱周年，给双方发推送
- **数据库侧**（迁移 `20260809223000_stage3_push.sql`）：pg_cron 定时任务 + `wishlist_items` 新增时触发器调 send-push 通知另一半
- **iOS 端待做**：OneSignal SDK 集成，注册时 `external_user_id = Supabase 用户 UUID`（服务端据此寻址）；APNs 证书需 Apple 开发者账号（$99）后配置

### 客户端接收约定
推送 `data` 字段携带跳转信息，app 收到后按 `type` 导航：
| type | data | 跳转 |
|---|---|---|
| `anniversary` | `anniversary_id` | 重要日期详情 |
| `couple_anniversary` | `couple_id` | 主页 |
| `wishlist` | `wishlist_id` | 心愿单 |

## 尚未实现（后续阶段）

- **阶段 4 照片**：阿里云 OSS，Edge Function 签发 STS 临时凭证，`avatar_url`/相册存 OSS key
- 对应表（albums/photos/tags/photo_tag）旧 Java 后端 `backend/database_setup.sql` 有设计，届时参考改编

## 照片上传（阶段 4，已部署验证）

### 凭据模型
AccessKey 只存 Supabase Edge Function secrets，**永不下发客户端**。iOS 拿"预签名 URL"直传 OSS，
文件二进制**不经过 Supabase**。

### `POST /functions/v1/oss-sign`
请求头：`Authorization: Bearer <用户 JWT>`（Edge Function 默认 verify_jwt）

```json
{ "items": [ { "photoId": "8f3a…-uuid", "variants": ["standard", "thumb"] } ] }
```

响应：
```json
{
  "coupleId": "34ce59e5-…",
  "expiresIn": 900,
  "items": [{
    "photoId": "8f3a…",
    "putUrls": {
      "standard": "https://ours-photos-jw.oss-cn-shanghai.aliyuncs.com/couples/34ce59e5-…/2026/10/8f3a….jpg?x-oss-…",
      "thumb":    "https://…/8f3a…_t.jpg?x-oss-…"
    },
    "getUrl": "https://…/8f3a….jpg?x-oss-…"
  }]
}
```

约束（服务端强制，客户端别绕）：
- 一次最多 **20 张**（跨境 RTT 成本）
- `photoId` 必须是 uuid；对象键由服务端拼，规则：
  `couples/<我的 couple_id>/<yyyy>/<MM>/<photo_id>{,_t,_o}.jpg`
  （`_t` 缩略图、`_o` 原图；见 `PHOTOS_DESIGN.md` §5 三份对象模型）
- URL 有效期 **900 秒**，过期需重新要一次
- 只签 PUT / GET，**不签删除与列举**

错误：`401` 未登录 / `403` 还没绑定另一半 / `400` 参数不合法（中文提示可直接展示）

### 客户端上传顺序
1. 本地跑 `PhotoPipeline.prepare`（压缩 2048 / 缩略图 400 / 读 EXIF 时间 / 剥离 GPS）
2. 先生成 `photoId`（客户端 uuid），调 `oss-sign` 拿 URL
3. `URLSession` PUT 标准图与缩略图（直传 OSS）
4. 插 `photos` 行（`object_key` / `thumb_key` / `size` / `width` / `height` / `taken_at`），RLS 保证建在自己 couple 下
5. 若有原图（设置里开启）再 PUT `_o.jpg` 并回填 `original_key`

### 已验证（2026-10-21）
真机链路已跑通：临时用户 → 真 JWT → `oss-sign` 200 → 预签名 URL 直传 **PUT 200** →
**GET 200 内容一致** → 临时用户/couple 已清理。bucket 私有策略与 RAM 最小权限均生效。
