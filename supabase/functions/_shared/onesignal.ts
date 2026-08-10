// OneSignal REST API 封装（共享模块）
// 密钥从 Supabase Edge Function secrets 读取，不进代码仓库

const APP_ID = Deno.env.get('ONESIGNAL_APP_ID') ?? '';
const API_KEY = Deno.env.get('ONESIGNAL_API_KEY') ?? '';

export interface PushPayload {
  title: string;
  body: string;
  data?: Record<string, unknown>; // 透传给 app 的附加数据（如跳转类型）
}

/**
 * 给指定用户发推送。
 * userIds = Supabase 用户 UUID（iOS 端 SDK 注册时 external_user_id 用的就是它）
 */
export async function sendToUsers(userIds: string[], payload: PushPayload): Promise<Response> {
  return await fetch('https://api.onesignal.com/api/v1/notifications', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Basic ${API_KEY}`,
    },
    body: JSON.stringify({
      app_id: APP_ID,
      include_external_user_ids: userIds,
      headings: { en: payload.title },
      contents: { en: payload.body },
      data: payload.data ?? {},
      // iOS 行为：默认声音 + 角标 +1
      ios_sound: 'default',
      ios_badgeType: 'Increase',
      ios_badgeCount: 1,
    }),
  });
}
