# 照片回忆（阶段 4）设计草案

> 状态：**设计未拍板，未写迁移、未写代码**
> 依据：`PRD.md` §4.2（云相册 / 相册 / 标签 / 时间线）+ 旧 Java 后端的 `backend/database_setup.sql`（albums/photos/tags/photo_tag）
> 目标：把"照片存哪、怎么传、怎么读、花多少钱"四件事定死，等阿里云 OSS 开通即可直接进编码

## 1. 范围

MVP 照片功能 = 上传 → 时间线浏览 → 按相册/标签筛 → 主页随机回忆卡取真实照片。

**明确不做**（YAGNI）：视频、人脸识别、AI 相册分类、多人共享、Web 端。

## 2. 数据模型（Postgres 草案）

与现有表保持同一风格：`uuid` 主键、`couple_id` 隔离、`is_deleted` 软删除、`updated_at` 触发器。

```sql
-- 相册（可选功能，见 §9 待拍板）
create table public.albums (
  id uuid primary key default gen_random_uuid(),
  couple_id uuid not null references public.couples(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 100),
  description text check (char_length(description) <= 500),
  cover_photo_id uuid,                    -- 不设 FK，避免和 photos 循环依赖
  created_by uuid not null references public.profiles(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

-- 照片
create table public.photos (
  id uuid primary key default gen_random_uuid(),
  couple_id uuid not null references public.couples(id) on delete cascade,
  album_id uuid references public.albums(id) on delete set null,  -- null = 未分类
  creator_id uuid not null references public.profiles(id),

  object_key text not null unique,        -- OSS 对象键（不含域名/bucket）
  thumb_key text,                         -- 缩略图对象键，见 §5
  original_name text not null,
  file_size bigint not null check (file_size > 0),
  mime_type text not null,
  width int,
  height int,
  taken_at timestamptz,                   -- EXIF 拍摄时间，时间线排序优先用它
  description text check (char_length(description) <= 500),
  location text check (char_length(location) <= 200),
  is_favorite boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

create index on public.photos (couple_id, taken_at desc nulls last, created_at desc) where not is_deleted;

-- 标签
create table public.tags (
  id uuid primary key default gen_random_uuid(),
  couple_id uuid not null references public.couples(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 32),
  color text not null default '#FFB300',
  created_by uuid not null references public.profiles(id),
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
create unique index on public.tags (couple_id, name) where not is_deleted;

-- 照片-标签
create table public.photo_tags (
  photo_id uuid not null references public.photos(id) on delete cascade,
  tag_id uuid not null references public.tags(id) on delete cascade,
  primary key (photo_id, tag_id)
);
```

RLS 与现有三张表同构：仅 `couple_id` 对应的两人可读写（`is_couple_member(couple_id)`，与 anniversaries/wishlist_items 的做法保持一致）。

### 相对旧 Java 设计的改动及理由

| 旧设计 | 新设计 | 理由 |
|---|---|---|
| `BIGINT AUTO_INCREMENT` | `uuid` | 与现有 couples/profiles/anniversaries 一致；避免跨环境 ID 冲突 |
| `file_path`（含路径） | `object_key`（只存 key） | 换 bucket/域名/加签名参数都不改库；域名永远是配置项 |
| 无缩略图字段 | `thumb_key` | 列表只拉缩略图，流量降一个量级（见 §7） |
| `file_type ENUM(PHOTO,VIDEO)` + `duration` | 去掉 | MVP 不做视频，YAGNI；将来加表或加列都可 |
| 无拍摄时间 | `taken_at` | `created_at` 是上传时间，时间线要按"那天拍的"排 |
| `photo_tag` 自增 id | 复合主键 | 少一列、天然去重 |
| — | `object_key unique` | 防止同一对象重复入库 |

## 3. 存储布局与权限

- **Bucket**：`ours-photos`，**私有**（无公共读、不开 CDN 公共分发）
- **对象键**：
  ```
  couples/{couple_id}/{yyyy}/{MM}/{photo_id}.jpg        原图（压缩后）
  couples/{couple_id}/{yyyy}/{MM}/{photo_id}_t.jpg      缩略图
  ```
- **权限模型**：Edge Function 用 RAM 用户的 AccessKey 生成**短期签名 URL**，iOS 拿 URL 直传/直取；AccessKey 只存在 Supabase secrets，绝不下发客户端
- **越权防护**：签名时把 key 前缀强制写成调用者所属 `couple_id` 的路径，客户端无法为别人的前缀要到签名

## 4. 上传与读取流程（关键取舍）

### 方案 A：STS 临时凭证 + iOS 集成阿里云 OSS SDK

- 优点：一次授权可多次操作；支持分片上传
- 缺点：iOS 要引入阿里云 OSS SDK（Objective-C，SPM 支持差，体积大）；首包体积和集成成本都上去

### 方案 B：Edge Function 签发 OSS 预签名 URL（**推荐**）

- iOS 只做两件事：`POST /functions/v1/oss-sign` 拿 `presignedPutUrl` → `URLSession.upload` 直传 OSS
- 读完拿 `presignedGetUrl`，有效期 1 小时
- 优点：**零 SDK 依赖**（只用 URLSession）、AccessKey 不出服务端、签名逻辑集中在 Edge Function
- 缺点：每张（或每批）要一次 RTT；Edge Function 在境外，大陆访问有跨境延迟 —— 用**批量签发**缓解（一次调用签 20 张）

流程图（方案 B）：

```
iOS                      Supabase Edge Function           阿里云 OSS
 |  ① POST oss-sign         |                              |
 |  (Bearer JWT, count=20)  |                              |
 |------------------------->| 校验 JWT → 取 couple_id       |
 |                          | 生成 20 个预签名 PUT URL      |
 |<-------------------------|                              |
 |  ② PUT 二进制（直传，不经过 Supabase）                    |
 |-------------------------------------------------------->|
 |  ③ POST /rest/v1/photos（写入 object_key 等元数据）        |
 |------------------------->|                              |
 |  ④ GET 列表（RLS 过滤）→ 拿 thumb_key → 批量换签名 URL → 拉缩略图
```

**注意**：文件二进制绝不过 Supabase（跨境、计费、慢），Edge Function 只签发凭证。

## 5. 客户端处理约定

| 项 | 约定 | 理由 |
|---|---|---|
| 上传前压缩 | 长边 ≤2048，JPEG 质量 0.8，约 300–600KB/张 | 存储与流量同时降一个量级 |
| 缩略图 | 长边 400，JPEG 质量 0.7，约 30–60KB | 列表/时间线只加载它 |
| EXIF | 剥离 GPS 与设备信息，但**先读出 `taken_at` 存库** | 隐私优先（PRD §7），又保留时间线排序 |
| 并发 | 同时上传 2 张，失败指数退避重试 3 次 | 大陆网络抖动常见，避免一次失败就丢 |
| 离线 | 不做离线队列（YAGNI），失败即提示重试 | 两人用量小 |

## 6. 查询与分页

- **时间线**：`order by taken_at desc nulls last, created_at desc`，keyset 分页（`taken_at < cursor`），每页 30
- **相册详情 / 标签筛选**：同分页，附加 `album_id` / `photo_tag` 过滤
- **随机回忆卡**：数据量小，直接 `select ... where not is_deleted order by random() limit 1`；主页每次下拉刷新换一张
- **计数**：相册卡片上的 photoCount 用 `count(*) filter`，或建物化缓存列（等真有几千张再优化）

## 7. 成本估算（按量付费参考价）

| 项 | 假设 | 成本 |
|---|---|---|
| 存储 | 2000 张压缩图（0.5MB）+ 2000 张缩略图（50KB）≈ 1.1GB | ≈0.13 元/月 |
| 浏览流量 | 每月翻 300 张缩略图（15MB）+ 30 张原图（15MB）≈ 0.03GB | ≈0.02 元/月（忙时 0.5 元/GB）|
| 请求费 | 上传 2000 次 + 读取数千次 | < 0.05 元/月 |
| **合计** | | **≈ 0.2 元/月** |

对比"不压缩、列表拉原图"：同样 300 次浏览 ≈ 1.5GB → 0.75 元/月，且加载慢。**压缩 + 缩略图是省钱和体验的双赢。**

> 单价来源：阿里云 OSS 标准存储（本地冗余）0.12 元/GB/月；外网流出忙时 0.50 元/GB、闲时 0.25 元/GB；新用户有 3 个月免费额度（20GB 存储 + 2GB 流量）。

## 8. 实施顺序（等 OSS 开通后）

1. 建表迁移 + RLS（不需要 OSS 就能先做，也可以现在做）
2. Edge Function `oss-sign`（签发预签名 URL）+ secrets 配 AccessKey
3. iOS：选图 → 压缩/缩略图 → 签名 → 直传 → 写库（先做上传，能看到自己传的图）
4. iOS：时间线列表（缩略图 + 分页 + 签名 URL 缓存）
5. iOS：相册/标签（若 §9 拍板要做）
6. 主页随机回忆卡接真实照片
7. 删除：软删数据库行 + 异步删 OSS 对象（或直接留下孤儿对象，定期清理）

## 9. 待拍板（不拍板我不动代码）

1. **要不要"多相册"？** 旧设计有 albums 表，但对两个人来说，"一条时间线 + 标签"更轻：少一层组织成本，标签足够表达"旅行/日常/第一张合照"。
   - 推荐：**先只做时间线 + 标签**，albums 表建但不上界面（结构留着，将来要做不用迁移）
2. **原图要不要保留？** 只存压缩图（2048 长边）省一半成本，但将来想印照片/做桌面壁纸就没了。
   - 推荐：**保留原图但默认不传**——设置里给一个"保留原图"开关，默认关
3. **标签要不要预置一批？**（旅行、美食、日常、纪念日…）
   - 推荐：预置 4 个，可增删

## 10. 已知风险

- **跨境延迟**：Edge Function 与 Supabase REST 都在境外，大陆访问会有几百 ms RTT；用批量签发 + 本地缓存签名 URL 缓解
- **签名 URL 过期**：列表里缓存的 URL 超过 1 小时会 403，需要"过期后重新签发"的兜底路径
- **删除语义**：软删数据库行后 OSS 对象仍在（占存储）；需要清理任务，否则删了照片存储费不降
