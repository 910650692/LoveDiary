import Foundation
import OneSignal

/// 全局状态：登录态 + 绑定态
@MainActor
final class AppState: ObservableObject {
    static let shared = AppState()

    @Published var isLoggedIn = false
    @Published var couple: Couple?
    @Published var isLoading = false

    private init() {}

    /// 刷新登录/绑定状态（App 启动、登录、绑定、登出后调用）
    func refresh() async {
        isLoading = true
        defer { isLoading = false }

        guard let session = try? await APIClient.client.auth.session else {
            isLoggedIn = false
            couple = nil
            return
        }
        isLoggedIn = true

        // 推送：把 Supabase 用户 ID 绑定为 OneSignal 的 external_user_id（服务端据此推送）
        OneSignal.login(session.user.id.uuidString)

        couple = try? await APIClient.fetchMyCouple(userId: session.user.id)
    }
}
