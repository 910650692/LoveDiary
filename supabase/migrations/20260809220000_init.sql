-- =============================================================
-- Ours (LoveDiary) 阶段 1：账号绑定 + 主页基础数据
-- 表：couples / profiles / invite_codes
-- =============================================================

-- ---------- couples：一对情侣 ----------
-- user_a_id = 创建者，user_b_id = 通过邀请码加入的另一半（加入前为 null）
create table public.couples (
  id           uuid primary key default gen_random_uuid(),
  user_a_id    uuid not null references auth.users (id) on delete cascade,
  user_b_id    uuid references auth.users (id) on delete cascade,
  started_date date not null,            -- 恋爱开始日期，主页算「在一起天数」
  created_at   timestamptz not null default now()
);

-- 每人只能属于一段关系（user_b 未加入前为 null，用部分唯一索引兜底）
create unique index couples_user_a_unique on public.couples (user_a_id) where user_a_id is not null;
create unique index couples_user_b_unique on public.couples (user_b_id) where user_b_id is not null;

alter table public.couples enable row level security;

create policy "couples_select_members" on public.couples
  for select
  using (user_a_id = auth.uid() or user_b_id = auth.uid());

create policy "couples_insert_creator" on public.couples
  for insert
  with check (user_a_id = auth.uid());

-- ---------- profiles：用户资料（注册时自动创建） ----------
create table public.profiles (
  id         uuid primary key references auth.users (id) on delete cascade,
  nickname   text not null default '',
  avatar_url text,
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

-- 能看自己的，也能看伴侣的（主页要显示两人）
create policy "profiles_select_own_and_partner" on public.profiles
  for select
  using (
    id = auth.uid()
    or exists (
      select 1 from public.couples c
      where (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
        and (c.user_a_id = profiles.id or c.user_b_id = profiles.id)
    )
  );

create policy "profiles_insert_own" on public.profiles
  for insert
  with check (id = auth.uid());

create policy "profiles_update_own" on public.profiles
  for update
  using (id = auth.uid())
  with check (id = auth.uid());

-- ---------- invite_codes：邀请码 ----------
create table public.invite_codes (
  code       text primary key,            -- 6~8 位随机码
  couple_id  uuid not null references public.couples (id) on delete cascade,
  created_by uuid not null references auth.users (id) on delete cascade,
  used_by    uuid references auth.users (id),
  expires_at timestamptz not null,
  created_at timestamptz not null default now()
);

alter table public.invite_codes enable row level security;

-- 只有 couple 成员能查看自己的邀请码
create policy "invite_codes_select_members" on public.invite_codes
  for select
  using (
    exists (
      select 1 from public.couples c
      where c.id = invite_codes.couple_id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

-- 只有 couple 创建者能生成邀请码
create policy "invite_codes_insert_creator" on public.invite_codes
  for insert
  with check (
    created_by = auth.uid()
    and exists (
      select 1 from public.couples c
      where c.id = couple_id
        and c.user_a_id = auth.uid()
    )
  );

-- ---------- 注册后自动创建 profile ----------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, nickname)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'nickname', '')
  );
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- ---------- join_couple：另一半用邀请码加入 ----------
-- 入参：邀请码；内部事务校验 + 绑定 + 标记已用，一次性完成
create or replace function public.join_couple(p_code text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_couple_id uuid;
  v_code invite_codes%rowtype;
begin
  -- 已在一段关系里的人不能再加入
  if exists (
    select 1 from public.couples
    where user_a_id = auth.uid() or user_b_id = auth.uid()
  ) then
    raise exception '你已经在一段关系里了';
  end if;

  select * into v_code from public.invite_codes where code = p_code;
  if not found then
    raise exception '邀请码无效';
  end if;
  if v_code.expires_at < now() then
    raise exception '邀请码已过期';
  end if;
  if v_code.used_by is not null then
    raise exception '邀请码已被使用';
  end if;

  -- 绑定；user_b 已被占则返回 not found
  update public.couples
  set user_b_id = auth.uid()
  where id = v_code.couple_id and user_b_id is null;

  if not found then
    raise exception '这段关系已经满员';
  end if;

  update public.invite_codes set used_by = auth.uid() where code = p_code;

  return v_code.couple_id;
end;
$$;

grant execute on function public.join_couple(text) to authenticated;
