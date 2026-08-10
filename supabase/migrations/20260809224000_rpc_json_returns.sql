-- =============================================================
-- create_couple 返回类型 text → json
-- 原因：supabase-swift（iOS 端）对 text/plain 的 RPC 返回解码不友好，
--       改成 json 字符串后客户端 `let code: String = try await rpc(...)` 可正常解码。
-- 对 REST 调用方兼容：PostgREST 返回 "M9WN8F"（带引号），JSON.parse 后值不变。
-- =============================================================

-- Postgres 不允许 create or replace 改变返回类型，先 drop 再建
drop function if exists public.create_couple(date);

create function public.create_couple(p_started_date date)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  v_couple_id uuid;
  v_code      text;
  v_chars     text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
begin
  if p_started_date is null or p_started_date > current_date then
    raise exception '恋爱开始日期无效';
  end if;

  if exists (
    select 1 from public.couples
    where user_a_id = auth.uid() or user_b_id = auth.uid()
  ) then
    raise exception '你已经在一段关系里了';
  end if;

  insert into public.couples (user_a_id, started_date)
  values (auth.uid(), p_started_date)
  returning id into v_couple_id;

  loop
    v_code := '';
    for i in 1..6 loop
      v_code := v_code || substr(v_chars, 1 + floor(random() * length(v_chars))::int, 1);
    end loop;

    begin
      insert into public.invite_codes (code, couple_id, created_by, expires_at)
      values (v_code, v_couple_id, auth.uid(), now() + interval '7 days');
      exit;
    exception when unique_violation then
      null;
    end;
  end loop;

  return to_json(v_code);
end;
$$;

grant execute on function public.create_couple(date) to authenticated;
