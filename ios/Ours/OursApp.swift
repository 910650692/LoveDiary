import SwiftUI
import OneSignalFramework

@main
struct OursApp: App {
    init() {
        // 推送初始化（APNs 证书配好后生效）
        OneSignal.initialize(AppConfig.oneSignalAppID, withLaunchOptions: nil)
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
