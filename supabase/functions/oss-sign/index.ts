// Supabase Edge Function: oss-sign
// 作用：用登录用户的 JWT 换 OSS 预签名 URL（只签名，不搬文件）
// 设计依据：supabase/PHOTOS_DESIGN.md §4 方案 B + §9 三份对象模型（_t / 标准 / _o）
//
// 安全要点：
// 1. AccessKey 只放在 Edge Function secrets，永不下发客户端
// 2. 对象键前缀强制钉死为 couples/<我的 couple_id>/...，客户端无法为别人的前缀要签名
// 3. 只签 PUT/GET，不给删除、列举权限
//
// 一次响应里同时给两类签名：
// - putUrls：**上传**用。键由服务端按「今天」拼，客户端插库前拿它直传
// - getUrls：**读取**用。键优先用请求里的 keys（就是 photos 行的 object_key/thumb_key/original_key），
//   没给才回落到同一套「今天」拼法。时间线读上个月甚至前年的照片必须靠 keys：
//   只按 today 拼路径会指向一个根本不存在的对象。

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

const ALL_VARIANTS: Variant[] = ["standard", "thumb", "original"];

interface SignItem {
  photoId: string;
  variants?: Variant[];                // 默认 ["standard", "thumb"]（与上传契约一致）
  keys?: Partial<Record<Variant, string>>; // 读已存在对象时给真实键；不给则按今天拼
}

// 每张照片最多三个对象，命名见 PHOTOS_DESIGN §9
const suffixOf: Record<Variant, string> = {
  standard: "",
  thumb: "_t",
  original: "_o",
};

const UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

function objectKey(coupleId: string, photoId: string, variant: Variant, now: Date): string {
  const yyyy = now.getUTCFullYear();
  const mm = String(now.getUTCMonth() + 1).padStart(2, "0");
  return `couples/${coupleId}/${yyyy}/${mm}/${photoId}${suffixOf[variant]}.jpg`;
}

/// 客户端传来的键只放行一种形态：前缀钉在调用者自己的 couple 目录下、uuid 文件名 + 可选 _t/_o 后缀。
/// 这是「不能替别人的照片要签名」的唯一防线，比任何客户端校验都可靠。
function isOwnKey(key: unknown, coupleId: string): key is string {
  if (typeof key !== "string" || key.length > 200) return false;
  return new RegExp(`^couples/${coupleId}/\\d{4}/\\d{2}/${UUID}(_t|_o)?\\.jpg$`, "i").test(key);
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

  for (const it of items) {
    if (!it?.photoId || !new RegExp(`^${UUID}$`, "i").test(it.photoId)) {
      return json({ error: "photoId 必须是 uuid" }, 400);
    }
    for (const v of it.variants ?? []) {
      if (!ALL_VARIANTS.includes(v)) {
        return json({ error: "variants 只能是 standard / thumb / original" }, 400);
      }
    }
    for (const v of ALL_VARIANTS) {
      const key = it.keys?.[v];
      if (key !== undefined && !isOwnKey(key, coupleId)) {
        return json({ error: "keys 不合法：对象键必须在你自己的 couple 目录下" }, 400);
      }
    }
  }

  // 4) 生成预签名 URL
  const client = new OSS({
    region: Deno.env.get("OSS_REGION")!,
    accessKeyId: Deno.env.get("OSS_ACCESS_KEY_ID")!,
    accessKeySecret: Deno.env.get("OSS_ACCESS_KEY_SECRET")!,
    bucket: Deno.env.get("OSS_BUCKET")!,
    secure: true,
  });

  const expiresIn = 900; // 15 分钟：够上传一批、刷一屏；过期客户端重签一次即可
  const now = new Date();
  const signed = items.map((it) => {
    const variants = it.variants?.length ? it.variants : (["standard", "thumb"] as Variant[]);
    const putUrls: Record<string, string> = {};
    const getUrls: Record<string, string> = {};
    for (const v of variants) {
      const freshKey = objectKey(coupleId, it.photoId, v, now);
      putUrls[v] = client.signatureUrl(freshKey, { expires: expiresIn, method: "PUT" });
      // 读：库里的真实键优先（老照片的 yyyy/MM 与今天不同）
      getUrls[v] = client.signatureUrl(it.keys?.[v] ?? freshKey, { expires: expiresIn, method: "GET" });
    }
    const standardReadKey = it.keys?.standard ?? objectKey(coupleId, it.photoId, "standard", now);
    return {
      photoId: it.photoId,
      putUrls,
      getUrls,
      // 老的单一 getUrl 保留：已上线的客户端在用，别让它们 404
      getUrl: client.signatureUrl(standardReadKey, { expires: expiresIn, method: "GET" }),
    };
  });

  return json({ coupleId, expiresIn, items: signed });
});
