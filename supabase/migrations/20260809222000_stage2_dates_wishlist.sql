-- =============================================================
-- Ours (LoveDiary) 阶段 2：重要日期 + 共同心愿单
-- 表：anniversaries / wishlist_items
-- 设计参考旧 Java 后端 database_setup.sql，改为 Postgres + RLS
-- =============================================================

-- 通用：updated_at 自动更新
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ---------- anniversaries：重要日期（纪念日/生日） ----------
create table public.anniversaries (
  id                  uuid primary key default gen_random_uuid(),
  couple_id           uuid not null references public.couples (id) on delete cascade,
  created_by          uuid not null references auth.users (id) on delete cascade, -- 谁添加的（推送通知用）
  name                text not null check (char_length(name) <= 50),
  date                date not null,
  notes               text check (notes is null or char_length(notes) <= 500),
  enable_notification boolean not null default true, -- 是否推送提醒（阶段 3 用）
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now()
);

create index anniversaries_couple_idx on public.anniversaries (couple_id);
create index anniversaries_couple_date_idx on public.anniversaries (couple_id, date);

create trigger anniversaries_updated_at
  before update on public.anniversaries
  for each row execute procedure public.set_updated_at();

alter table public.anniversaries enable row level security;

create policy "anniversaries_select_members" on public.anniversaries
  for select
  using (
    exists (
      select 1 from public.couples c
      where c.id = anniversaries.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "anniversaries_insert_members" on public.anniversaries
  for insert
  with check (
    created_by = auth.uid()
    and exists (
      select 1 from public.couples c
      where c.id = couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "anniversaries_update_members" on public.anniversaries
  for update
  using (
    exists (
      select 1 from public.couples c
      where c.id = anniversaries.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "anniversaries_delete_members" on public.anniversaries
  for delete
  using (
    exists (
      select 1 from public.couples c
      where c.id = anniversaries.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

-- ---------- wishlist_items：共同心愿单 ----------
-- 旧后端叫 todo_items；PRD 定位是「承载对未来的期待」，改叫心愿单
create table public.wishlist_items (
  id           uuid primary key default gen_random_uuid(),
  couple_id    uuid not null references public.couples (id) on delete cascade,
  creator_id   uuid not null references auth.users (id) on delete cascade,
  completer_id uuid references auth.users (id),             -- 谁完成的（完成记录）
  title        text not null check (char_length(title) <= 200),
  description  text check (description is null or char_length(description) <= 1000),
  status       text not null default 'PENDING' check (status in ('PENDING', 'COMPLETED')),
  completed_at timestamptz,
  is_deleted   boolean not null default false,              -- 软删除
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create index wishlist_couple_idx on public.wishlist_items (couple_id);
create index wishlist_couple_status_idx on public.wishlist_items (couple_id, status);

create trigger wishlist_updated_at
  before update on public.wishlist_items
  for each row execute procedure public.set_updated_at();

alter table public.wishlist_items enable row level security;

create policy "wishlist_select_members" on public.wishlist_items
  for select
  using (
    exists (
      select 1 from public.couples c
      where c.id = wishlist_items.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "wishlist_insert_members" on public.wishlist_items
  for insert
  with check (
    creator_id = auth.uid()
    and exists (
      select 1 from public.couples c
      where c.id = couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

-- 完成操作：任一方可改状态/完成人/完成时间
create policy "wishlist_update_members" on public.wishlist_items
  for update
  using (
    exists (
      select 1 from public.couples c
      where c.id = wishlist_items.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create policy "wishlist_delete_members" on public.wishlist_items
  for delete
  using (
    exists (
      select 1 from public.couples c
      where c.id = wishlist_items.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );
