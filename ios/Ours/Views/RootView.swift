import SwiftUI

/// 根视图：按登录/绑定状态切换页面
struct RootView: View {
    @StateObject private var app = AppState.shared

    var body: some View {
        Group {
            if !app.isLoggedIn {
                SignInView()
            } else if app.couple == nil {
                BindView()
            } else {
                MainTabView()
            }
        }
        .animation(.easeInOut(duration: 0.25), value: app.isLoggedIn)
        .animation(.easeInOut(duration: 0.25), value: app.couple != nil)
        .task { await app.refresh() }
    }
}
