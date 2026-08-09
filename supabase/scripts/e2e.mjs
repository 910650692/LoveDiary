// 阶段1 绑定流程端到端验证
// 用法：SUPABASE_URL=... ANON_KEY=... SERVICE_ROLE_KEY=... node e2e.mjs
const url = process.env.SUPABASE_URL;
const anon = process.env.ANON_KEY;
const service = process.env.SERVICE_ROLE_KEY;

const api = async (path, { method = 'GET', body, key = anon, token } = {}) => {
  const res = await fetch(url + path, {
    method,
    headers: {
      apikey: key,
      Authorization: `Bearer ${token ?? key}`,
      'Content-Type': 'application/json',
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; } // RPC 返回纯文本（如邀请码）
  }
  return { status: res.status, data };
};

const check = (name, cond, extra = '') => {
  console.log(`${cond ? '✅' : '❌'} ${name}${extra ? '  →  ' + extra : ''}`);
  if (!cond) process.exitCode = 1;
};

const rand = Date.now().toString(36);
const emailA = `e2e-a-${rand}@test.local`;
const emailB = `e2e-b-${rand}@test.local`;
const password = 'Test123456!';

// 1. 建两个已确认用户（service_role）
const ua = await api('/auth/v1/admin/users', { method: 'POST', key: service, body: { email: emailA, password, email_confirm: true } });
const ub = await api('/auth/v1/admin/users', { method: 'POST', key: service, body: { email: emailB, password, email_confirm: true } });
check('admin 建用户 A/B', ua.status >= 200 && ua.status < 300 && ub.status >= 200 && ub.status < 300, `${ua.status}/${ub.status}`);

// 2. 密码登录拿 token
const ta = await api('/auth/v1/token?grant_type=password', { method: 'POST', body: { email: emailA, password } });
const tb = await api('/auth/v1/token?grant_type=password', { method: 'POST', body: { email: emailB, password } });
check('A/B 密码登录', ta.status === 200 && tb.status === 200, `${ta.status}/${tb.status}`);

// 3. 密码登录后：能看到自己的 profile（触发器已建）
const pa = await api('/rest/v1/profiles?select=id,nickname', { token: ta.data.access_token });
check('注册后 profile 自动创建', pa.status === 200 && Array.isArray(pa.data) && pa.data.length >= 1, JSON.stringify(pa.data));

// 4. A 建关系拿邀请码
const createRes = await api('/rest/v1/rpc/create_couple', { method: 'POST', token: ta.data.access_token, body: { p_started_date: '2023-05-20' } });
check('A create_couple 拿到邀请码', createRes.status === 200 && /^[A-Z2-9]{6}$/.test(createRes.data), `code=${createRes.data}`);

// 5. B 用邀请码加入
const joinRes = await api('/rest/v1/rpc/join_couple', { method: 'POST', token: tb.data.access_token, body: { p_code: createRes.data } });
check('B join_couple 绑定成功', joinRes.status === 200, `couple_id=${joinRes.data}`);

// 6. 验证 couples：A 视角能看到双方
const couples = await api('/rest/v1/couples?select=id,user_a_id,user_b_id,started_date', { token: ta.data.access_token });
check('couples 可见且双方已绑定', couples.status === 200 && couples.data?.[0]?.user_a_id && couples.data?.[0]?.user_b_id, JSON.stringify(couples.data));

// 6b. 绑定后：A 能同时看到两人 profile（RLS 伴侣可见）
const pb = await api('/rest/v1/profiles?select=id,nickname', { token: ta.data.access_token });
check('绑定后双人 profile 可见', pb.status === 200 && Array.isArray(pb.data) && pb.data.length === 2, JSON.stringify(pb.data));

// 7. 反例：B 再创建关系应失败（已在一段关系里）
const again = await api('/rest/v1/rpc/create_couple', { method: 'POST', token: tb.data.access_token, body: { p_started_date: '2024-01-01' } });
check('B 重复建关系被拒', again.status >= 400, again.status);

// 8. 清理测试用户（FK 级联删 couples/profiles/invite_codes）
const delA = await api(`/auth/v1/admin/users/${ua.data.id}`, { method: 'DELETE', key: service });
const delB = await api(`/auth/v1/admin/users/${ub.data.id}`, { method: 'DELETE', key: service });
check('清理测试用户', delA.status === 200 && delB.status === 200, `${delA.status}/${delB.status}`);

console.log(`\n测试邮箱：${emailA} / ${emailB}`);
