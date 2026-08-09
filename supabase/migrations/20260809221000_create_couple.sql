-- =============================================================
-- Ours (LoveDiary) 阶段 1：create_couple —— 建关系 + 生成邀请码
-- 与 join_couple 配套：A 建关系拿码 → B 输码加入
-- =============================================================

create or replace function public.create_couple(p_started_date date)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_couple_id uuid;
  v_code      text;
  -- 去掉易混字符：O/0、I/1
  v_chars     text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
begin
  if p_started_date is null or p_started_date > current_date then
    raise exception '恋爱开始日期无效';
  end if;

  -- 已在一段关系里的人不能再创建
  if exists (
    select 1 from public.couples
    where user_a_id = auth.uid() or user_b_id = auth.uid()
  ) then
    raise exception '你已经在一段关系里了';
  end if;

  insert into public.couples (user_a_id, started_date)
  values (auth.uid(), p_started_date)
  returning id into v_couple_id;

  -- 生成 6 位邀请码，撞码则重试
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
      null; -- 撞码，重新生成
    end;
  end loop;

  return v_code;
end;
$$;

grant execute on function public.create_couple(date) to authenticated;
