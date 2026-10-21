-- =============================================================
-- Ours (LoveDiary) 阶段 4：照片回忆（表结构，不含 OSS 逻辑）
-- 表：albums / photos / tags / photo_tags
-- 设计文档：supabase/PHOTOS_DESIGN.md
-- 设计参考旧 Java 后端 database_setup.sql，改为 Postgres + RLS（uuid + couple_id 隔离 + 软删除）
--
-- 已拍板（2026-10-21）：
--   1. 只做「时间线 + 标签」，albums 表建但暂不上界面
--   2. 每张照片最多三个对象：缩略图(永远) / 标准图 2048(永远) / 原图(开关开启时)
--   3. 展示默认标准图 + 缩略图，只有存在原图时才出现「查看原图」入口
-- =============================================================

-- ---------- albums：相册（暂无界面，结构先留） ----------
create table public.albums (
  id            uuid primary key default gen_random_uuid(),
  couple_id     uuid not null references public.couples(id) on delete cascade,
  name          text not null check (char_length(name) between 1 and 100),
  description   text check (char_length(description) <= 500),
  -- 封面照片：不设外键，避免与 photos.album_id 形成循环依赖
  cover_photo_id uuid,
  created_by    uuid not null references public.profiles(id) on delete cascade,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  is_deleted    boolean not null default false
);

create index albums_couple_idx on public.albums (couple_id) where not is_deleted;

create trigger albums_updated_at
  before update on public.albums
  for each row execute function public.set_updated_at();

create policy "albums_select_members" on public.albums
  for select using (
    exists (
      select 1 from public.couples c
      where c.id = albums.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "albums_insert_members" on public.albums
  for insert with check (
    created_by = auth.uid()
    and exists (
      select 1 from public.couples c
      where c.id = couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "albums_update_members" on public.albums
  for update using (
    exists (
      select 1 from public.couples c
      where c.id = albums.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "albums_delete_members" on public.albums
  for delete using (
    exists (
      select 1 from public.couples c
      where c.id = albums.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

-- ---------- photos：照片元数据（二进制在 OSS，库里只存 key） ----------
create table public.photos (
  id            uuid primary key default gen_random_uuid(),
  couple_id     uuid not null references public.couples(id) on delete cascade,
  album_id      uuid references public.albums(id) on delete set null,
  creator_id    uuid not null references public.profiles(id) on delete cascade,

  -- OSS 对象键（不含 bucket / 域名 / 签名参数，换域名不用改库）
  object_key    text not null unique,      -- 标准图：长边 2048 压缩
  thumb_key     text,                      -- 缩略图：长边 400
  original_key  text,                      -- 原图：仅「保留原图」开关开启时有值

  original_name text not null,
  file_size     bigint not null check (file_size > 0),   -- 标准图字节数
  mime_type     text not null,
  width         int check (width is null or width > 0),
  height        int check (height is null or height > 0),
  taken_at      timestamptz,               -- EXIF 拍摄时间；时间线排序优先用它
  description   text check (char_length(description) <= 500),
  location      text check (char_length(location) <= 200),
  is_favorite   boolean not null default false,

  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  is_deleted    boolean not null default false
);

-- 时间线分页：按拍摄时间倒序（无拍摄时间用上传时间兜底）
create index photos_timeline_idx
  on public.photos (couple_id, taken_at desc nulls last, created_at desc)
  where not is_deleted;

create index photos_album_idx on public.photos (album_id) where not is_deleted and album_id is not null;

create trigger photos_updated_at
  before update on public.photos
  for each row execute function public.set_updated_at();

create policy "photos_select_members" on public.photos
  for select using (
    exists (
      select 1 from public.couples c
      where c.id = photos.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "photos_insert_members" on public.photos
  for insert with check (
    creator_id = auth.uid()
    and exists (
      select 1 from public.couples c
      where c.id = couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "photos_update_members" on public.photos
  for update using (
    exists (
      select 1 from public.couples c
      where c.id = photos.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "photos_delete_members" on public.photos
  for delete using (
    exists (
      select 1 from public.couples c
      where c.id = photos.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

-- ---------- tags：标签 ----------
create table public.tags (
  id         uuid primary key default gen_random_uuid(),
  couple_id  uuid not null references public.couples(id) on delete cascade,
  name       text not null check (char_length(name) between 1 and 32),
  color      text not null default '#FFB300',
  created_by uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

-- 同一对情侣内标签名唯一（软删除的不算，删了可以重建同名）
create unique index tags_couple_name_idx on public.tags (couple_id, name) where not is_deleted;

create policy "tags_select_members" on public.tags
  for select using (
    exists (
      select 1 from public.couples c
      where c.id = tags.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "tags_insert_members" on public.tags
  for insert with check (
    created_by = auth.uid()
    and exists (
      select 1 from public.couples c
      where c.id = couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "tags_update_members" on public.tags
  for update using (
    exists (
      select 1 from public.couples c
      where c.id = tags.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "tags_delete_members" on public.tags
  for delete using (
    exists (
      select 1 from public.couples c
      where c.id = tags.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

-- ---------- photo_tags：照片-标签关系（复合主键天然去重） ----------
create table public.photo_tags (
  photo_id uuid not null references public.photos(id) on delete cascade,
  tag_id   uuid not null references public.tags(id) on delete cascade,
  primary key (photo_id, tag_id)
);

create policy "photo_tags_select_members" on public.photo_tags
  for select using (
    exists (
      select 1 from public.photos p
      join public.couples c on c.id = p.couple_id
      where p.id = photo_tags.photo_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

-- 打标签时同时校验：照片与标签属于同一对情侣，且我是这对情侣的成员
-- （否则客户端可以把自己的照片打上别人家的 tag_id）
create policy "photo_tags_insert_members" on public.photo_tags
  for insert with check (
    exists (
      select 1
      from public.photos p
      join public.tags t on t.id = photo_tags.tag_id
      join public.couples c on c.id = p.couple_id
      where p.id = photo_tags.photo_id
        and t.couple_id = p.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "photo_tags_delete_members" on public.photo_tags
  for delete using (
    exists (
      select 1 from public.photos p
      join public.couples c on c.id = p.couple_id
      where p.id = photo_tags.photo_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );
