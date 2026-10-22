// Supabase Edge Function: notify-photos
// 作用：一批照片上传完，通知**另一半**「TA 传了 N 张新照片」，让上传那一方得到回响。
//
// 两条克制规则（推送一旦被用户关掉，这个 app 就死了）：
// 1. 一批一条：客户端整批只调一次，count 是本批成功张数，不是逐张调用
// 2. 服务端再挡一层：同一 couple 10 分钟内已经推过照片就跳过（push_throttle 表）
//
// 鉴权：Authorization: Bearer <用户 JWT>（verify_jwt，认人方式照 oss-sign）
// 入参：{ "count": 3 }
// 出参：{ sent: boolean, reason?, coupleId?, partner?, onesignalStatus?, onesignal? }

import { createClient } from "jsr:@supabase/supabase-js@2";
import { sendToUsers } from "../_shared/onesignal.ts";

const CORS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS, "Content-Type": "application/json" },
  });

/// 与 oss-sign 的单批上限保持一致：一批最多 20 张
const MAX_BATCH = 20;
/// 同一 couple 的照片推送间隔（毫秒）
const THROTTLE_WINDOW_MS = 10 * 60 * 1000;
const THROTTLE_KIND = "photos";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (req.method !== "POST") return json({ error: "只支持 POST" }, 405);

  const authHeader = req.headers.get("Authorization") ?? "";
  if (!authHeader.startsWith("Bearer ")) return json({ error: "缺少登录凭证" }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;

  // 1) 认人：用户 JWT 走 RLS，函数不猜「你是谁」
  const supabase = createClient(supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userErr } = await supabase.auth.getUser();
  const userId = userData?.user?.id;
  if (userErr || !userId) return json({ error: "登录已失效，请重新登录" }, 401);

  // 2) 校验入参（先看请求本身合不合法，再看这个人有没有资格推）
  let count: unknown;
  try {
    count = (await req.json())?.count;
  } catch {
    return json({ error: "请求体不是合法 JSON" }, 400);
  }
  if (typeof count !== "number" || !Number.isInteger(count) || count < 1 || count > MAX_BATCH) {
    return json({ error: `count 必须是 1..${MAX_BATCH} 的整数` }, 400);
  }

  // 3) 找他的 couple（RLS 保证只能看到自己的那条）
  const { data: couples, error: coupleErr } = await supabase
    .from("couples")
    .select("id, user_a_id, user_b_id")
    .or(`user_a_id.eq.${userId},user_b_id.eq.${userId}`)
    .limit(1);
  if (coupleErr) return json({ error: "查关系失败", detail: coupleErr.message }, 500);
  const couple = couples?.[0];
  if (!couple) return json({ error: "还没绑定另一半，先不要发推送" }, 403);

  // 4) 推给另一半，绝不推给自己
  const partner = couple.user_a_id === userId ? couple.user_b_id : couple.user_a_id;
  if (!partner || partner === userId) return json({ sent: false, reason: "no_partner" });

  // 5) 节流：10 分钟内同一 couple 已经推过照片就静默跳过（返回 200，客户端不关心）
  //    节流表是服务端内部状态，只有 service_role 能动
  const admin = createClient(supabaseUrl, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data: throttle, error: throttleErr } = await admin
    .from("push_throttle")
    .select("sent_at")
    .eq("couple_id", couple.id)
    .eq("kind", THROTTLE_KIND)
    .maybeSingle();
  if (throttleErr) return json({ error: "查节流失败", detail: throttleErr.message }, 500);

  if (throttle?.sent_at) {
    const elapsed = Date.now() - new Date(throttle.sent_at).getTime();
    if (elapsed >= 0 && elapsed < THROTTLE_WINDOW_MS) {
      return json({ sent: false, reason: "throttled", coupleId: couple.id, throttledFor: THROTTLE_WINDOW_MS - elapsed });
    }
  }

  // 6) 推
  const res = await sendToUsers([partner], {
    title: "新照片",
    body: `TA 传了 ${count} 张新照片 📷`,
    data: { type: "photos" },
  });
  const text = await res.text();

  if (!res.ok) {
    // 没推成功就不记节流：否则一次 OneSignal 抖动会把「下一批还能推」也吃掉
    return json({ error: "推送发送失败", onesignalStatus: res.status, detail: text.slice(0, 300) }, 502);
  }

  await admin.from("push_throttle").upsert(
    { couple_id: couple.id, kind: THROTTLE_KIND, sent_at: new Date().toISOString() },
    { onConflict: "couple_id,kind" },
  );

  let onesignal: unknown = null;
  try {
    onesignal = JSON.parse(text);
  } catch {
    onesignal = text.slice(0, 300);
  }
  return json({ sent: true, coupleId: couple.id, partner, onesignalStatus: res.status, onesignal });
});
