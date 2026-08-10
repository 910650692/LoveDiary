-- =============================================================
-- Ours (LoveDiary) 阶段 3：推送基础设施
-- 1) pg_cron 每天 23:00 UTC（北京时间 07:00）调 anniversary-reminder
-- 2) wishlist_items 新增时调 send-push 通知另一半
-- 内部鉴权：Authorization: Bearer <OUR_INTERNAL_SECRET>
--   （与 supabase secrets 里的 OUR_INTERNAL_SECRET 保持一致）
-- =============================================================

create extension if not exists pg_cron with schema pg_catalog;
create extension if not exists pg_net;

-- ---------- 1) 纪念日提醒（每天 07:00 北京时间） ----------
select cron.schedule(
  'anniversary-reminder',
  '0 23 * * *', -- UTC 23:00 = 北京时间 07:00
  $$
  select net.http_post(
    url := 'https://prkijbbjrjfmkolkjxsk.supabase.co/functions/v1/anniversary-reminder',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer jZZueMa4Gle05Nth9VLoxFEj'
    ),
    body := '{}'::jsonb
  ) as request_id;
  $$
);

-- ---------- 2) 心愿新增 → 通知另一半 ----------
create or replace function public.notify_wishlist_added()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_partner uuid;
begin
  -- 找到另一半
  select case when c.user_a_id = new.creator_id then c.user_b_id else c.user_a_id end
    into v_partner
    from public.couples c
    where c.id = new.couple_id;

  if v_partner is not null then
    perform net.http_post(
      url := 'https://prkijbbjrjfmkolkjxsk.supabase.co/functions/v1/send-push',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer jZZueMa4Gle05Nth9VLoxFEj'
      ),
      body := jsonb_build_object(
        'userIds', jsonb_build_array(v_partner),
        'title', '新心愿 🎁',
        'body', 'TA 新增了一个心愿，去看看',
        'data', jsonb_build_object('type', 'wishlist', 'wishlist_id', new.id)
      )
    );
  end if;

  return new;
end;
$$;

create trigger on_wishlist_insert
  after insert on public.wishlist_items
  for each row execute procedure public.notify_wishlist_added();
