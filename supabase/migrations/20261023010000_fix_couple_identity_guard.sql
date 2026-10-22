-- =============================================================
-- 修复：freeze_couple_identity 触发器把 join_couple 的绑定静默回滚
--
-- 问题：20261022000000 加的 BEFORE UPDATE 触发器无条件把 user_b_id 写回旧值，
--       而 join_couple（security definer）正是靠 update couples set user_b_id = auth.uid()
--       完成绑定 → 更新被回滚，函数仍因 FOUND 为真返回 couple_id，客户端以为绑定成功。
--       结果：从 2026-10-22 起第二个用户永远绑不上（e2e 8 项红）。
--
-- 正解：不用触发器，用 Postgres 的**列级授权**。
--   - 行级：RLS 策略 couples_update_members（保留）
--   - 列级：authenticated 只能 UPDATE(started_date)
--   join_couple 是 security definer，以 owner 身份执行，不受列授权限制。
-- =============================================================

drop trigger if exists couples_freeze_identity on public.couples;
drop function if exists public.freeze_couple_identity();

-- 收回表级 UPDATE，只放行 started_date 一列
revoke update on public.couples from authenticated;
revoke update on public.couples from anon;
grant update (started_date) on public.couples to authenticated;
