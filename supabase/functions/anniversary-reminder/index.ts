// anniversary-reminder：纪念日提醒（pg_cron 每天 07:00 北京时间触发）
//
// 三个窗口：今天 / 明天 / 还有 7 天。三个窗口的「月-日」两两不同，
// 同一条纪念日一次运行里最多命中一个窗口 → 天然不会重复推（7 天预告不会和 1 天提醒撞车）。
//
// 覆盖两类：anniversaries（enable_notification=true）+ 恋爱周年（couples.started_date）
//
// 匹配按「月-日」每年复现，与 iOS 主页倒计时同一套语义：date 里存的年份是那次纪念日的起点，
// 不是提醒的年份 —— 拿整条 date 去等值匹配的话，跨年之后的纪念日永远命中不了（恋爱周年更是
// 必然一次都推不出来，因为 started_date 永远在过去）。
//
// 鉴权：Authorization: Bearer <OUR_INTERNAL_SECRET>

import { createClient } from 'npm:@supabase/supabase-js@2';
import { sendToUsers } from '../_shared/onesignal.ts';

const TZ = 'Asia/Shanghai';

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '', // Supabase 平台自动注入
);

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

interface Window {
  days: number;   // 0 = 今天，1 = 明天，7 = 提前一周
  date: string;   // YYYY-MM-DD（北京时间）
  md: string;     // MM-DD
  year: number;
}

/// 北京时间的「N 天后」：没有夏令时，加整天的毫秒数就是准确的日期
function shanghaiDay(offsetDays: number): Window {
  const date = new Date(Date.now() + offsetDays * 86_400_000)
    .toLocaleDateString('en-CA', { timeZone: TZ }); // en-CA 就是 YYYY-MM-DD
  return { days: offsetDays, date, md: date.slice(5), year: Number(date.slice(0, 4)) };
}

const windows: Window[] = [shanghaiDay(0), shanghaiDay(1), shanghaiDay(7)];

/// 「月-日」→ 窗口。三个月日互不相同，所以一个日子只可能落进一个窗口
const windowOf = new Map<string, Window>(windows.map((w) => [w.md, w]));

function monthDayOf(date: string): string {
  return date.slice(5, 10); // 'YYYY-MM-DD' → 'MM-DD'
}

/// 发一条并留下证据：status / 响应片段写进返回体，服务端验证就不必只靠日志
async function push(
  key: string,
  window: Window,
  users: string[],
  title: string,
  body: string,
  data: Record<string, unknown>,
) {
  const res = await sendToUsers(users, { title, body, data });
  const text = await res.text();
  return { key, window: window.days, status: res.status, detail: text.slice(0, 200) };
}

Deno.serve(async (req) => {
  const auth = req.headers.get('Authorization');
  if (auth !== `Bearer ${Deno.env.get('OUR_INTERNAL_SECRET')}`) {
    return json({ error: 'unauthorized' }, 401);
  }

  const sent: string[] = [];
  const pushes: unknown[] = [];

  // 1) 自定义纪念日 / 生日（今天 / 明天 / 还有 7 天）
  const { data: anniversaries } = await supabase
    .from('anniversaries')
    .select('id, name, date, couple_id, couples(user_a_id, user_b_id)')
    .eq('enable_notification', true);

  for (const a of anniversaries ?? []) {
    const window = windowOf.get(monthDayOf(String(a.date)));
    if (!window) continue;
    const users = [a.couples?.user_a_id, a.couples?.user_b_id].filter(Boolean) as string[];
    if (!users.length) continue;

    const title = window.days === 0 ? '今天是纪念日 🎉'
      : window.days === 1 ? '明天是纪念日 🎉'
      : '还有 7 天 ⏰';
    const body = window.days === 0 ? `${a.name}，就是今天！`
      : window.days === 1 ? `${a.name}，记得提前准备哦`
      : `${a.name}，还有 7 天就到了`;

    pushes.push(await push(
      `anniversary:${a.id}`, window, users, title, body,
      { type: 'anniversary', anniversary_id: a.id },
    ));
    sent.push(`anniversary:${a.id}`);
  }

  // 2) 恋爱周年（couples.started_date 的月日每年复现）
  const { data: couples } = await supabase
    .from('couples')
    .select('id, started_date, user_a_id, user_b_id')
    .not('started_date', 'is', null);

  for (const c of couples ?? []) {
    const startedDate = String(c.started_date);
    const window = windowOf.get(monthDayOf(startedDate));
    if (!window) continue;

    // 周年数按「这次落地的那个年份」算：跨年时（今天 12-28、纪念日 01-04）不能拿今天的年份减
    const years = window.year - Number(startedDate.slice(0, 4));
    if (years <= 0) continue; // 在一起还不到一年，没有「N 周年」可庆祝

    const users = [c.user_a_id, c.user_b_id].filter(Boolean) as string[];
    if (!users.length) continue;

    const title = window.days === 0 ? `在一起 ${years} 周年 ❤️`
      : window.days === 1 ? `明天是 ${years} 周年 ❤️`
      : `还有 7 天就是 ${years} 周年 ❤️`;
    const body = window.days === 0 ? '庆祝我们的纪念日吧！'
      : window.days === 1 ? '提前祝我们纪念日快乐！'
      : '提前想想怎么庆祝吧';

    pushes.push(await push(
      `couple:${c.id}`, window, users, title, body,
      { type: 'couple_anniversary', couple_id: c.id },
    ));
    sent.push(`couple:${c.id}`);
  }

  return json({
    windows: windows.map((w) => ({ days: w.days, date: w.date })),
    sent,
    pushes,
  });
});
