// send-push：通用推送入口（数据库触发器 / 其他函数调用）
// 入参：{ userIds: string[], title: string, body: string, data?: object }
// 鉴权：Authorization: Bearer <OUR_INTERNAL_SECRET>（内部专用，不对外）

import { sendToUsers } from '../_shared/onesignal.ts';

Deno.serve(async (req) => {
  const auth = req.headers.get('Authorization');
  if (auth !== `Bearer ${Deno.env.get('OUR_INTERNAL_SECRET')}`) {
    return new Response(JSON.stringify({ error: 'unauthorized' }), { status: 401 });
  }

  const body = await req.json().catch(() => null);
  if (!body || !Array.isArray(body.userIds) || body.userIds.length === 0 || !body.title || !body.body) {
    return new Response(JSON.stringify({ error: 'invalid payload' }), { status: 400 });
  }

  const res = await sendToUsers(body.userIds, {
    title: body.title,
    body: body.body,
    data: body.data,
  });
  const text = await res.text();
  return new Response(text, { status: res.status, headers: { 'Content-Type': 'application/json' } });
});
