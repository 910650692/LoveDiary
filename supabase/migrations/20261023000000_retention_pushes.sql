-- =============================================================
-- Ours (LoveDiary) 留存闭环补丁（不新增表结构，只加一条推送 + 一张节流表）
-- 1) wishlist_items 状态 PENDING → COMPLETED 时，通知**另一半**（不是完成者本人）
-- 2) push_throttle：照片推送的服务端节流（同一 couple 10 分钟一条）
--    notify-photos 只靠它挡重复推送，客户端与其它角色一律无权限（开 RLS 且无策略）
-- 内部鉴权沿用阶段 3：Authorization: Bearer <OUR_INTERNAL_SECRET>
-- =============================================================

-- ---------- 1) 心愿完成 → 通知另一半 ----------
create or replace function public.notify_wishlist_completed()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_actor   uuid;
  v_partner uuid;
begin
  -- 完成者：正常情况下 completer_id 有值；只改了 status 的客户端没写它，退回创建者
  v_actor := coalesce(new.completer_id, new.creator_id);

  -- 找到另一个人（完成者本人不推：自己点完成还有推送，等于自问自答刷屏）
  select case
           when c.user_a_id = v_actor then c.user_b_id
           else c.user_a_id
         end
    into v_partner
    from public.couples c
    where c.id = new.couple_id;

  if v_partner is not null and v_partner <> v_actor then
    perform net.http_post(
      url := 'https://prkijbbjrjfmkolkjxsk.supabase.co/functions/v1/send-push',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer jZZueMa4Gle05Nth9VLoxFEj'
      ),
      body := jsonb_build_object(
        'userIds', jsonb_build_array(v_partner),
        'title', '心愿达成 🎉',
        'body', 'TA 完成了心愿：' || new.title || ' 🎉',
        'data', jsonb_build_object('type', 'wishlist', 'wishlist_id', new.id)
      )
    );
  end if;

  return new;
end;
$$;

-- 「只在状态真正变成 COMPLETED 时触发」这件事写在 WHEN 里，不靠函数体里的 if：
-- 改标题、改备注、软删除、把 COMPLETED 再写一遍（例如只补 completer_id）时
-- 触发器根本不会执行，连函数都进不去，日志里也不会留下一次白跑的 http_post。
create trigger on_wishlist_completed
  after update on public.wishlist_items
  for each row
  when (
    old.status is distinct from new.status
    and new.status = 'COMPLETED'
    and new.is_deleted = false
  )
  execute procedure public.notify_wishlist_completed();

-- ---------- 2) 照片推送节流表 ----------
-- 只有一行/一个 couple/一种 kind：涨不上去的小表，不需要清理任务
create table public.push_throttle (
  couple_id uuid        not null references public.couples (id) on delete cascade,
  kind      text        not null,                    -- 推送种类，目前只有 'photos'
  sent_at   timestamptz not null default now(),      -- 上次真的推出去的时刻
  primary key (couple_id, kind)
);

comment on table public.push_throttle is
  '服务端推送节流：同一 couple 同一 kind 在窗口期内只推一条。只有 service_role 可读写。';

-- 开 RLS 且不给任何策略 = 客户端（anon / authenticated）既读不到也写不了，
-- 只有 Edge Function 用的 service_role 能碰；节流状态属于服务端内部状态，不下发。
alter table public.push_throttle enable row level security;
