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
- **Edge Function**（`supabase/functions/`）：
  - `send-push` — 通用推送入口：`POST /functions/v1/send-push` `{userIds:[uuid], title, body, data?}`，verify_jwt=false，靠 `OUR_INTERNAL_SECRET` 鉴权，仅供内部/触发器调用
  - `anniversary-reminder` — 纪念日提醒：pg_cron 每天 23:00 UTC（北京时间 07:00）触发，查**今天 / 明天 / 还有 7 天**的 anniversaries（enable_notification=true）+ 恋爱周年，给双方发推送（同一纪念日一次运行只命中一个窗口）
  - `notify-photos` — 照片批次推送：`POST /functions/v1/notify-photos` `{count}`，**用户 JWT**（verify_jwt），推给另一半。契约见下文「照片上传推送」
- **数据库侧**：
  - `20260809223000_stage3_push.sql`：pg_cron 定时任务 + `wishlist_items` 新增时触发器调 send-push 通知另一半
  - `20261023000000_retention_pushes.sql`：`wishlist_items` 状态 PENDING → COMPLETED 时触发器（只推给对方，不推完成者）+ `push_throttle` 节流表（只有 service_role 能读写）
- **iOS 端**：OneSignal SDK 集成，注册时 `external_user_id = Supabase 用户 UUID`（服务端据此寻址）；APNs 证书需 Apple 开发者账号（$99）后配置

### 推送触发点总表（谁、在什么时刻、推给谁）
| 触发 | 位置 | 收件人 | data.type |
|---|---|---|---|
| 对方新增心愿 | 触发器 `on_wishlist_insert` | 另一半 | `wishlist` |
| 对方完成心愿（PENDING → COMPLETED 且未软删） | 触发器 `on_wishlist_completed` | 另一半（**不是**完成者） | `wishlist` |
| 一批照片上传完成（≥1 张成功） | 函数 `notify-photos`（客户端整批调一次） | 另一半（**不是**上传者） | `photos` |
| 纪念日今天 / 明天 / 还有 7 天 | 函数 `anniversary-reminder`（pg_cron） | 双方 | `anniversary` / `couple_anniversary` |

改标题、改备注、软删除照片/心愿、重复写一次 COMPLETED 都**不会**产生推送：触发器条件写在 `WHEN` 里。

### 客户端接收约定
推送 `data` 字段携带跳转信息，app 收到后按 `type` 导航：
| type | data | 跳转 |
|---|---|---|
| `anniversary` | `anniversary_id` | 重要日期详情 |
| `couple_anniversary` | `couple_id` | 主页 |
| `wishlist` | `wishlist_id` | 心愿单 |
| `photos` | 无 | 照片 Tab |

### `POST /functions/v1/notify-photos`（照片批次推送）

请求头：`Authorization: Bearer <用户 JWT>`（verify_jwt，与 `oss-sign` 同一套认人方式）

请求：
```json
{ "count": 3 }
```

- `count`：**本批成功张数**（不是单张回调次数），必须是 1..20 的整数，越界返回 400
- 一批只调一次：`PhotoUploader` 在整批收尾且 ≥1 张成功时调用；失败静默忽略，不影响上传结果

响应：
```json
{ "sent": true, "coupleId": "…", "partner": "…", "onesignalStatus": 200, "onesignal": { "id": "…", "recipients": 1 } }
```

| 状态 | 含义 |
|---|---|
| 200 `sent: true` | 已推给另一半 |
| 200 `sent: false, reason: "throttled"` | 同一 couple 10 分钟内已推过照片，本次跳过（正常节流，不是错误） |
| 200 `sent: false, reason: "no_partner"` | 关系里找不到另一半，静默跳过 |
| 400 | `count` 不合法 / 请求体不是 JSON |
| 401 | 缺少或失效的用户 JWT |
| 403 | 还没绑定另一半 |
| 502 | OneSignal 没接单（此时**不**记节流，下一批还能推） |

推送内容：标题「新照片」，正文「TA 传了 3 张新照片 📷」，`data = {"type":"photos"}`（客户端点击切到照片 Tab）。
服务端节流状态存在 `push_throttle` 表（`couple_id + kind='photos'` 一行，开 RLS 且无策略 = 客户端碰不到）。

## 尚未实现（后续阶段）

- **阶段 4 照片**：阿里云 OSS，Edge Function 签发 STS 临时凭证，`avatar_url`/相册存 OSS key
- 对应表（albums/photos/tags/photo_tag）旧 Java 后端 `backend/database_setup.sql` 有设计，届时参考改编

## 照片上传（阶段 4，已部署验证）

### 凭据模型
AccessKey 只存 Supabase Edge Function secrets，**永不下发客户端**。iOS 拿"预签名 URL"直传 OSS，
文件二进制**不经过 Supabase**。

### `POST /functions/v1/oss-sign`
请求头：`Authorization: Bearer <用户 JWT>`（Edge Function 默认 verify_jwt）

请求（**上传**：只给 photoId + variants，与阶段 4 原契约完全一致，已上线客户端不用改）：
```json
{ "items": [ { "photoId": "8f3a…-uuid", "variants": ["standard", "thumb"] } ] }
```

请求（**读取**：把 photos 行里的真实对象键放进 `keys`）：
```json
{
  "items": [{
    "photoId": "8f3a…-uuid",
    "variants": ["thumb"],
    "keys": {
      "thumb":    "couples/34ce59e5-…/2026/10/8f3a…_t.jpg",
      "standard": "couples/34ce59e5-…/2026/10/8f3a….jpg",
      "original": "couples/34ce59e5-…/2026/10/8f3a…_o.jpg"
    }
  }]
}
```

响应（`putUrls`/`getUrl` 是老字段，只增不改）：
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
    "getUrls": {
      "standard": "https://…/8f3a….jpg?x-oss-…",
      "thumb":    "https://…/8f3a…_t.jpg?x-oss-…"
    },
    "getUrl": "https://…/8f3a….jpg?x-oss-…"
  }]
}
```

字段语义：
- `putUrls`：**上传**用，键由服务端按「当天」拼（客户端插库前拿它直传）
- `getUrls`：**读取**用，键优先取请求里的 `keys`，没给才回落到与 `putUrls` 同一套拼法。
  键与 `variants` 一一对应：只请求 `["thumb"]` 就只有 `getUrls.thumb`，原图只在请求 `"original"` 时出现
- `getUrl`：历史字段，等价于标准图 GET；保留是为了不带 `keys` 的老客户端不炸

约束（服务端强制，客户端别绕）：
- 一次最多 **20 张**（跨境 RTT 成本）→ **时间线一页 30 张必须拆成 2 次调用**（客户端分片）
- `photoId` 必须是 uuid；对象键由服务端拼，规则：
  `couples/<我的 couple_id>/<yyyy>/<MM>/<photo_id>{,_t,_o}.jpg`
  （`_t` 缩略图、`_o` 原图；见 `PHOTOS_DESIGN.md` §9 三份对象模型）
- `keys` 每个值都必须是 **自己 couple 目录下**的 `.jpg`：
  服务端按 `^couples/<我的 couple_id>/<yyyy>/<MM>/<uuid>(_t|_o)?\.jpg$` 校验，别人的前缀或 `..` 穿越一律 `400`
- **读老照片必须带 `keys`**：服务端只会按「当天」拼 yyyy/MM，不给真实键就会签出一个不存在的对象（`404`）
- URL 有效期 **900 秒**，过期需重新要一次
- 只签 PUT / GET / DELETE，**不签列举**（ListBuckets / ListObjects 一律没有）；
  删除签名只对**点名的单个对象键**有效，不存在批量或前缀删除能力（见下节）

错误：`401` 未登录 / `403` 还没绑定另一半 / `400` 参数不合法（中文提示可直接展示）

### 客户端上传顺序
1. 本地跑 `PhotoPipeline.prepare`（压缩 2048 / 缩略图 400 / 读 EXIF 时间 / 剥离 GPS）
2. 先生成 `photoId`（客户端 uuid），调 `oss-sign` 拿 URL
3. `URLSession` PUT 标准图与缩略图（直传 OSS）
4. 插 `photos` 行（`object_key` / `thumb_key` / `size` / `width` / `height` / `taken_at`），RLS 保证建在自己 couple 下
5. 若有原图（设置里开启）再 PUT `_o.jpg` 并回填 `original_key`

### 客户端读取（时间线）顺序
1. PostgREST 查 `photos`（RLS + `is_deleted=eq.false`）：
   `order=taken_at.desc.nullslast,created_at.desc`，**keyset 分页每页 30**，
   游标是上一页最后一行的 `(taken_at, created_at, id)`（`taken_at` 为 null 的那段只用 `(created_at, id)`）
2. 每页 30 张的 `thumb_key`（为空回退 `object_key`）打包成 `keys` 调 `oss-sign`，
   **超过 20 张拆成两次**（服务端上限）
3. 拿 `getUrls.thumb` 用 `URLSession` 直连 OSS 拉图；签名 URL 按**对象键**缓存在内存里并记下过期时刻
4. 点开看大图用 `getUrls.standard`；`original_key` 非空时才显示「查看原图」，点了才要 `getUrls.original`
5. **过期兜底**：缓存的签名 URL 超过 900 秒会 403，按同一对象键**重签一次再重试**，
   仍失败才显示占位，不要直接把破图糊到用户脸上

## 照片删除（阶段 4，已部署验证）

### 请求（同一个端点，加 `delete: true`）
```json
{
  "items": [{
    "photoId": "8f3a…-uuid",
    "delete": true,
    "keys": {
      "standard": "couples/34ce59e5-…/2026/10/8f3a….jpg",
      "thumb":    "couples/34ce59e5-…/2026/10/8f3a…_t.jpg",
      "original": "couples/34ce59e5-…/2026/10/8f3a…_o.jpg"
    }
  }]
}
```

### 响应（只多一个字段，`putUrls`/`getUrls`/`getUrl` 照旧都在）
```json
{ "items": [{ "photoId": "8f3a…", "deleteUrls": { "standard": "https://…?x-oss-…", "thumb": "https://…?x-oss-…" } }] }
```

字段语义：
- `deleteUrls` **只在 `delete: true` 时出现**；不带 `delete` 的老请求响应里没有它，上传与时间线契约零变化
- 键与 `keys` 里给了的几个变体一一对应：`keys` 只有 `standard` 就只有 `deleteUrls.standard`（「有哪个删哪个」）
- 每条 URL 是一个 `DELETE` 预签名，**只对那一个对象键有效**；没有 ListObjects、没有前缀/批量删除

约束（服务端强制）：
- `delete: true` **必须带 `keys`**，且每个键同样要过 `^couples/<我的 couple_id>/<yyyy>/<MM>/<uuid>(_t|_o)?\.jpg$`：
  没有 key 就只能按「今天」猜路径，猜错不报错、只会留下没人清理的孤儿文件 → 直接 `400`
- 别人的 couple 前缀、`..` 穿越、非 `.jpg` 一律 `400`；无 JWT `401`
- 一次最多 20 项（与上传/读取同一上限）
- 删除签名有效期同样 **900 秒**

### 客户端删除顺序（**顺序不能反**）
1. 二次确认：明确告诉用户「照片会从你们的相册里消失，无法恢复」
2. **先软删库行**：`PATCH photos {is_deleted: true}`（`?id=eq.<id>`）→ 用户视角立刻消失，
   时间线查询本来就带 `is_deleted=eq.false`，下一次刷新不会再出现
3. **再删 OSS 对象**：拿 `deleteUrls` 对 `thumb_key` / `object_key` / `original_key` 逐个 `DELETE`（有哪个删哪个）
4. 从网格里移除该项（或重拉第一页），回到时间线

**为什么先删库行、后删对象**：反过来的话，对象删掉、库行没删成功，就留下一张「有记录但文件没了」的坏图，
用户点开只能看到破图；按上面这个顺序，最坏情况只是留下孤儿文件——只影响存储成本，用户完全无感。
孤儿文件由运维脚本清理，不属于客户端职责。

**OSS 删除失败不要回滚库行，也不要弹错**：库行已经软删，用户视角照片已经消失；
删对象失败只静默记一条日志（孤儿文件留待运维清），绝不要把用户拉回「照片还在」的状态。

### 已验证（oss-sign 三条链路：上传 / 读取 / 删除）
- **2026-10-22 删除链路**：临时用户 → 真 JWT → `oss-sign` 200（老字段不变，新增 `deleteUrls`）→
  PUT 标准图/缩略图各 `200` → 用 `deleteUrls` 发 **DELETE 204** → 同一对象再 **GET 404**（证明确实删掉）；
  别人 couple 前缀 / `..` 穿越 / `delete:true` 却不带 keys 全部 `400`，无 JWT `401`，临时用户/couple 已清理。
- **2026-10-21 上传链路**：临时用户 → 真 JWT → `oss-sign` 200 → 预签名 URL 直传 **PUT 200** →
  **GET 200 内容一致** → 临时用户/couple 已清理。bucket 私有策略与 RAM 最小权限均生效。
- **2026-10-22 读签名（getUrls）**：临时用户 → 真 JWT → `oss-sign` 200（`putUrls`/`getUrl` 老字段不变，
  新增 `getUrls`）→ PUT 缩略图与原图各 **200** → 用 `getUrls.thumb` / `getUrls.original` **GET 200 且字节一致**；
  跨月老键（`2026/01/…`）原样透传进签名（该对象本不存在，GET 404 符合预期）；
  别人的 couple 前缀 / `..` 穿越 / 非法变体全部 **400**，无 JWT **401**，裸 GET（无签名）**403**。
