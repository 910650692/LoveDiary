// anniversary-reminder：纪念日提醒（pg_cron 每天 07:00 北京时间触发）
// 逻辑：查「今天」和「明天」的纪念日/生日（enable_notification=true），
//       以及恋爱周年的 couples，给双方发推送。
// 鉴权：Authorization: Bearer <OUR_INTERNAL_SECRET>

import { createClient } from 'npm:@supabase/supabase-js@2';
import { sendToUsers } from '../_shared/onesignal.ts';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '', // Supabase 平台自动注入
);

function fmtDate(d: Date): string {
  return d.toLocaleDateString('en-CA', { timeZone: 'Asia/Shanghai' });
}

Deno.serve(async (req) => {
  const auth = req.headers.get('Authorization');
  if (auth !== `Bearer ${Deno.env.get('OUR_INTERNAL_SECRET')}`) {
    return new Response(JSON.stringify({ error: 'unauthorized' }), { status: 401 });
  }

  const today = new Date();
  const tomorrow = new Date(today.getTime() + 86400000);
  const todayStr = fmtDate(today);
  const tomorrowStr = fmtDate(tomorrow);

  const sent: string[] = [];

  // 1) 自定义纪念日/生日（今天 + 明天）
  const { data: anniversaries } = await supabase
    .from('anniversaries')
    .select('id, name, date, couple_id, couples(user_a_id, user_b_id)')
    .in('date', [todayStr, tomorrowStr])
    .eq('enable_notification', true);

  for (const a of anniversaries ?? []) {
    const users = [a.couples?.user_a_id, a.couples?.user_b_id].filter(Boolean) as string[];
    if (!users.length) continue;
    const isToday = a.date === todayStr;
    await sendToUsers(users, {
      title: isToday ? '今天是纪念日 🎉' : '明天是纪念日 🎉',
      body: `${a.name}${isToday ? '，就是今天！' : '，记得提前准备哦'}`,
      data: { type: 'anniversary', anniversary_id: a.id },
    });
    sent.push(`anniversary:${a.id}`);
  }

  // 2) 恋爱周年（couples.started_date）
  const { data: couples } = await supabase
    .from('couples')
    .select('id, started_date, user_a_id, user_b_id')
    .in('started_date', [todayStr, tomorrowStr]);

  for (const c of couples ?? []) {
    const years = new Date(`${c.started_date}T00:00:00+08:00`).getFullYear();
    const currentYear = today.getFullYear();
    const n = currentYear - years;
    if (n <= 0) continue;
    const users = [c.user_a_id, c.user_b_id].filter(Boolean) as string[];
    if (!users.length) continue;
    const isToday = c.started_date === todayStr;
    await sendToUsers(users, {
      title: isToday ? `在一起 ${n} 周年 ❤️` : `明天是 ${n} 周年 ❤️`,
      body: isToday ? '庆祝我们的纪念日吧！' : '提前祝我们纪念日快乐！',
      data: { type: 'couple_anniversary', couple_id: c.id },
    });
    sent.push(`couple:${c.id}`);
  }

  return new Response(JSON.stringify({ sent }), { status: 200, headers: { 'Content-Type': 'application/json' } });
});
