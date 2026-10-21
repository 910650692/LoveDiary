// Supabase Edge Function: oss-sign
// 作用：用登录用户的 JWT 换 OSS 预签名 URL（只签名，不搬文件）
// 设计依据：supabase/PHOTOS_DESIGN.md §4 方案 B
//
// 安全要点：
// 1. AccessKey 只放在 Edge Function secrets，永不下发客户端
// 2. 对象键前缀强制钉死为 couples/<我的 couple_id>/...，客户端无法为别人的前缀要签名
// 3. 只签 PUT/GET，不给删除、列举权限

import { createClient } from "jsr:@supabase/supabase-js@2";
import OSS from "npm:ali-oss@6";

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

type Variant = "standard" | "thumb" | "original";

interface SignItem {
  photoId: string;
  variants?: Variant[];   // 默认 ["standard", "thumb"]
}

// 每张照片最多三个对象，命名见 PHOTOS_DESIGN §5
const suffixOf: Record<Variant, string> = {
  standard: "",
  thumb: "_t",
  original: "_o",
};

function objectKey(coupleId: string, photoId: string, variant: Variant, now: Date): string {
  const yyyy = now.getUTCFullYear();
  const mm = String(now.getUTCMonth() + 1).padStart(2, "0");
  return `couples/${coupleId}/${yyyy}/${mm}/${photoId}${suffixOf[variant]}.jpg`;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (req.method !== "POST") return json({ error: "只支持 POST" }, 405);

  const authHeader = req.headers.get("Authorization") ?? "";
  if (!authHeader.startsWith("Bearer ")) return json({ error: "缺少登录凭证" }, 401);

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } },
  );

  // 1) 认人
  const { data: userData, error: userErr } = await supabase.auth.getUser();
  const userId = userData?.user?.id;
  if (userErr || !userId) return json({ error: "登录已失效，请重新登录" }, 401);

  // 2) 找他的 couple（RLS 保证只能看到自己的）
  const { data: couples, error: coupleErr } = await supabase
    .from("couples")
    .select("id")
    .or(`user_a_id.eq.${userId},user_b_id.eq.${userId}`)
    .limit(1);
  if (coupleErr) return json({ error: "查关系失败", detail: coupleErr.message }, 500);
  const coupleId = couples?.[0]?.id;
  if (!coupleId) return json({ error: "还没绑定另一半，不能上传照片" }, 403);

  // 3) 校验请求
  let items: SignItem[];
  try {
    const body = await req.json();
    items = body?.items ?? [];
  } catch {
    return json({ error: "请求体不是合法 JSON" }, 400);
  }
  if (!Array.isArray(items) || items.length === 0) return json({ error: "items 不能为空" }, 400);
  if (items.length > 20) return json({ error: "一次最多签 20 张（跨境 RTT 成本）" }, 400);

  const uuidRe = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  for (const it of items) {
    if (!it?.photoId || !uuidRe.test(it.photoId)) return json({ error: "photoId 必须是 uuid" }, 400);
  }

  // 4) 生成预签名 URL
  const client = new OSS({
    region: Deno.env.get("OSS_REGION")!,
    accessKeyId: Deno.env.get("OSS_ACCESS_KEY_ID")!,
    accessKeySecret: Deno.env.get("OSS_ACCESS_KEY_SECRET")!,
    bucket: Deno.env.get("OSS_BUCKET")!,
    secure: true,
  });

  const expiresIn = 900; // 15 分钟，够上传一批
  const now = new Date();
  const signed = items.map((it) => {
    const variants = it.variants?.length ? it.variants : (["standard", "thumb"] as Variant[]);
    const urls: Record<string, string> = {};
    for (const v of variants) {
      const key = objectKey(coupleId, it.photoId, v, now);
      urls[v] = client.signatureUrl(key, { expires: expiresIn, method: "PUT" });
    }
    const readKey = objectKey(coupleId, it.photoId, "standard", now);
    return {
      photoId: it.photoId,
      putUrls: urls,
      getUrl: client.signatureUrl(readKey, { expires: expiresIn, method: "GET" }),
    };
  });

  return json({ coupleId, expiresIn, items: signed });
});
