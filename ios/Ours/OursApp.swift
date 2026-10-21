import SwiftUI
import OneSignalFramework

@main
struct OursApp: App {
    init() {
        // 推送初始化（APNs 证书配好后生效）
        OneSignal.initialize(AppConfig.oneSignalAppID, withLaunchOptions: nil)
        // 点击监听要在 initialize 之后立刻注册：冷启动的那次点击也要接住
        PushRouter.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
