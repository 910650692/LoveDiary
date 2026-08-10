// Ours 端到端验证：阶段1 绑定流程 + 阶段2 CRUD/RLS + 阶段3 推送链路
// 用法：
//   SUPABASE_URL=... ANON_KEY=... SERVICE_ROLE_KEY=... \
//   [ONESIGNAL_APP_ID=... ONESIGNAL_API_KEY=... OUR_INTERNAL_SECRET=...] \
//   node e2e.mjs
const url = process.env.SUPABASE_URL;
const anon = process.env.ANON_KEY;
const service = process.env.SERVICE_ROLE_KEY;
const onesignalAppId = process.env.ONESIGNAL_APP_ID ?? '';
const onesignalKey = process.env.ONESIGNAL_API_KEY ?? '';
const internalSecret = process.env.OUR_INTERNAL_SECRET ?? '';

const api = async (path, { method = 'GET', body, key = anon, token } = {}) => {
  const res = await fetch(url + path, {
    method,
    headers: {
      apikey: key,
      Authorization: `Bearer ${token ?? key}`,
      'Content-Type': 'application/json',
      // POST/PATCH 默认只返回状态码；带上它才能拿回行（supabase-swift 默认行为一致）
      ...(method === 'POST' || method === 'PATCH' ? { Prefer: 'return=representation' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }
  return { status: res.status, data };
};

const check = (name, cond, extra = '') => {
  console.log(`${cond ? '✅' : '❌'} ${name}${extra ? '  →  ' + extra : ''}`);
  if (!cond) process.exitCode = 1;
};

// OneSignal 通知查询（独立域名，不走 Supabase）
const apiOnesignal = async (path) => {
  const res = await fetch(`https://api.onesignal.com${path}`, {
    headers: { Authorization: `Basic ${onesignalKey}` },
  });
  return { status: res.status, data: await res.json().catch(() => null) };
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const shanghaiToday = () => new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Shanghai' });

const rand = Date.now().toString(36);
const emailA = `e2e-a-${rand}@test.local`;
const emailB = `e2e-b-${rand}@test.local`;
const emailC = `e2e-c-${rand}@test.local`;
const password = 'Test123456!';

// ---------- 准备：建三个用户 ----------
const ua = await api('/auth/v1/admin/users', { method: 'POST', key: service, body: { email: emailA, password, email_confirm: true } });
const ub = await api('/auth/v1/admin/users', { method: 'POST', key: service, body: { email: emailB, password, email_confirm: true } });
const uc = await api('/auth/v1/admin/users', { method: 'POST', key: service, body: { email: emailC, password, email_confirm: true } });
check('admin 建用户 A/B/C', [ua, ub, uc].every((r) => r.status < 300), `${ua.status}/${ub.status}/${uc.status}`);
const idA = ua.data.id, idB = ub.data.id, idC = uc.data.id;

const ta = await api('/auth/v1/token?grant_type=password', { method: 'POST', body: { email: emailA, password } });
const tb = await api('/auth/v1/token?grant_type=password', { method: 'POST', body: { email: emailB, password } });
const tc = await api('/auth/v1/token?grant_type=password', { method: 'POST', body: { email: emailC, password } });
check('A/B/C 密码登录', [ta, tb, tc].every((r) => r.status === 200), `${ta.status}/${tb.status}/${tc.status}`);
const tokenA = ta.data.access_token, tokenB = tb.data.access_token, tokenC = tc.data.access_token;

// ---------- 阶段1：绑定流程 ----------
const pa = await api('/rest/v1/profiles?select=id,nickname', { token: tokenA });
check('注册后 profile 自动创建', pa.status === 200 && Array.isArray(pa.data) && pa.data.length >= 1);

const createRes = await api('/rest/v1/rpc/create_couple', { method: 'POST', token: tokenA, body: { p_started_date: '2023-05-20' } });
check('A create_couple 拿到邀请码', createRes.status === 200 && /^[A-Z2-9]{6}$/.test(createRes.data), `code=${createRes.data}`);

const joinRes = await api('/rest/v1/rpc/join_couple', { method: 'POST', token: tokenB, body: { p_code: createRes.data } });
check('B join_couple 绑定成功', joinRes.status === 200, `couple_id=${joinRes.data}`);
const coupleId = joinRes.data;

const couples = await api('/rest/v1/couples?select=id,user_a_id,user_b_id,started_date', { token: tokenA });
check('couples 双方已绑定', couples.status === 200 && couples.data?.[0]?.user_a_id && couples.data?.[0]?.user_b_id);

const pb = await api('/rest/v1/profiles?select=id,nickname', { token: tokenA });
check('绑定后双人 profile 可见', pb.status === 200 && pb.data.length === 2);

const again = await api('/rest/v1/rpc/create_couple', { method: 'POST', token: tokenB, body: { p_started_date: '2024-01-01' } });
check('B 重复建关系被拒', again.status >= 400, `${again.status}`);

// ---------- 阶段2：纪念日 CRUD ----------
const annDate = shanghaiToday();
const ann = await api('/rest/v1/anniversaries', {
  method: 'POST', token: tokenA,
  body: { couple_id: coupleId, created_by: idA, name: 'e2e测试纪念日', date: annDate, enable_notification: true },
});
check('A 添加纪念日', ann.status === 201, `${ann.status}`);
const annId = ann.data?.[0]?.id;

const annB = await api('/rest/v1/anniversaries?select=id,name,date', { token: tokenB });
check('B 能看到纪念日', annB.status === 200 && annB.data.some((a) => a.id === annId));

const annC = await api('/rest/v1/anniversaries?select=id', { token: tokenC });
check('外人 C 看不到纪念日（RLS）', annC.status === 200 && annC.data.length === 0, JSON.stringify(annC.data));

const annUpd = await api(`/rest/v1/anniversaries?id=eq.${annId}`, {
  method: 'PATCH', token: tokenA, body: { name: '改名后的纪念日' },
});
const annB2 = await api('/rest/v1/anniversaries?select=id,name', { token: tokenB });
check('A 改名后 B 能看到新名字', annUpd.status === 200 && annB2.data.find((a) => a.id === annId)?.name === '改名后的纪念日');

// ---------- 阶段2：心愿单 CRUD ----------
const wl = await api('/rest/v1/wishlist_items', {
  method: 'POST', token: tokenB,
  body: { couple_id: coupleId, creator_id: idB, title: '一起去海边', description: '看日落' },
});
check('B 添加心愿单', wl.status === 201, `${wl.status}`);
const wlId = wl.data?.[0]?.id;

const wlA = await api('/rest/v1/wishlist_items?select=id,title', { token: tokenA });
check('A 能看到 B 的心愿', wlA.status === 200 && wlA.data.some((w) => w.id === wlId));

const wlDone = await api(`/rest/v1/wishlist_items?id=eq.${wlId}`, {
  method: 'PATCH', token: tokenB,
  body: { status: 'COMPLETED', completer_id: idB, completed_at: new Date().toISOString() },
});
const wlB = await api('/rest/v1/wishlist_items?select=id,status,completer_id', { token: tokenA });
check('B 完成后状态正确', wlDone.status === 200 && wlB.data.find((w) => w.id === wlId)?.status === 'COMPLETED');

const wlDel = await api(`/rest/v1/wishlist_items?id=eq.${wlId}`, { method: 'PATCH', token: tokenA, body: { is_deleted: true } });
const wlList = await api('/rest/v1/wishlist_items?select=id&is_deleted=eq.false', { token: tokenA });
check('A 软删后列表不再出现', wlDel.status === 200 && !wlList.data.some((w) => w.id === wlId));

const coupleC = await api('/rest/v1/couples?select=id', { token: tokenC });
check('外人 C 看不到关系（RLS）', coupleC.status === 200 && coupleC.data.length === 0);

// ---------- 阶段3：纪念日提醒函数 ----------
if (internalSecret) {
  const reminder = await api('/functions/v1/anniversary-reminder', {
    method: 'POST', token: internalSecret,
    body: {},
  });
  check('anniversary-reminder 查到今天的纪念日并发送', reminder.status === 200 && reminder.data?.sent?.includes(`anniversary:${annId}`), JSON.stringify(reminder.data?.sent ?? []));
} else {
  check('anniversary-reminder 触发', false, '未提供 OUR_INTERNAL_SECRET，跳过');
}

// ---------- 阶段3：推送链路 ----------
if (internalSecret) {
  // 直接调 send-push（与数据库触发器调用的是同一个函数），验证 OneSignal 正常接单
  const push = await api('/functions/v1/send-push', {
    method: 'POST', token: internalSecret,
    body: { userIds: [idA], title: '新心愿 🎁', body: 'TA 新增了一个心愿，去看看', data: { type: 'wishlist' } },
  });
  check('send-push → OneSignal 接单', push.status === 200, `${push.status}`);
}

if (onesignalKey && onesignalAppId) {
  // 心愿插入已触发 DB 触发器 → send-push；无设备注册时 OneSignal 不入账，列表查不到属正常
  await sleep(1500);
  const notif = await apiOnesignal(`/api/v1/notifications?app_id=${onesignalAppId}&limit=3`);
  const found = (notif.data?.notifications ?? []).find((n) => n.headings?.en?.includes('新心愿'));
  console.log(`ℹ️ OneSignal 最近通知${found ? '找到「新心愿」✓' : '未找到「新心愿」——无设备注册时不入账，属预期，真机验证再看'}`);
} else {
  console.log('ℹ️ 未提供 ONESIGNAL_API_KEY/APP_ID，跳过推送入账提示');
}

// ---------- 清理 ----------
const delA = await api(`/auth/v1/admin/users/${idA}`, { method: 'DELETE', key: service });
const delB = await api(`/auth/v1/admin/users/${idB}`, { method: 'DELETE', key: service });
const delC = await api(`/auth/v1/admin/users/${idC}`, { method: 'DELETE', key: service });
check('清理测试用户 A/B/C', [delA, delB, delC].every((r) => r.status === 200), `${delA.status}/${delB.status}/${delC.status}`);

console.log(`\n测试邮箱：${emailA} / ${emailB} / ${emailC}`);
