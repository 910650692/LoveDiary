-- =============================================================
-- 允许两人修改自己关系的「在一起日期」，同时冻结身份列
--
-- 背景：couples 原本只有 SELECT / INSERT 策略（关系只能通过 create_couple /
-- join_couple 函数建立）。App 增加「修改在一起日期」入口后，需要一条 UPDATE 策略。
--
-- 安全考虑：RLS 不能限定「只能改哪一列」，所以用触发器把 id / user_a_id /
-- user_b_id / created_at 全部冻结——否则客户端可以顺手把 user_b_id 改掉（等于解绑）。
-- =============================================================

create policy "couples_update_members" on public.couples
  for update
  using (
    exists (
      select 1 from public.couples c
      where c.id = couples.id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  )
  with check (
    exists (
      select 1 from public.couples c
      where c.id = couples.id
        and (c.user_a_id = auth.uid() or c.user_b_id = auth.uid())
    )
  );

create or replace function public.freeze_couple_identity()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.id := old.id;
  new.user_a_id := old.user_a_id;
  new.user_b_id := old.user_b_id;
  new.created_at := old.created_at;
  return new;
end;
$$;

drop trigger if exists couples_freeze_identity on public.couples;
create trigger couples_freeze_identity
  before update on public.couples
  for each row execute function public.freeze_couple_identity();
