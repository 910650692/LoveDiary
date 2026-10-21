import Foundation
import OneSignalFramework

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
            syncPushIdentity(userId: nil)
            return
        }
        isLoggedIn = true

        // 推送：把 Supabase 用户 ID 绑定为 OneSignal 的 external_user_id（服务端据此推送）
        syncPushIdentity(userId: session.user.id)

        couple = try? await APIClient.fetchMyCouple(userId: session.user.id)
    }

    /// 登出：清 Supabase session + 解绑 OneSignal 的 external_user_id + 清空本地登录态
    /// 不解绑的话，换账号后上一个人的推送还会发到这台机器
    func signOut() async {
        try? await APIClient.signOut()
        syncPushIdentity(userId: nil)
        isLoggedIn = false
        couple = nil
        // 下一次登录从主页开始，不残留上一任停在的 Tab
        PushRouter.shared.selectedTab = .home
    }

    /// OneSignal 身份与登录用户一一对应：有用户就 login，没有就 logout
    private func syncPushIdentity(userId: UUID?) {
        if let userId {
            OneSignal.login(userId.uuidString)
        } else {
            OneSignal.logout()
        }
    }
}
