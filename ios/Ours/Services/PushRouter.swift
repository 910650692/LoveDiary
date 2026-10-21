import Foundation
import OneSignalFramework

/// 主界面 Tab：集中式选中态（TabView 绑定这里，推送点击也只改这里）
enum MainTab: Int, Hashable {
    case home = 0
    case dates = 1
    case wishlist = 2
    case me = 3
}

/// 推送点击路由：按 supabase/API.md「客户端接收约定」把通知 data["type"] 映射到主界面 Tab
final class PushRouter: NSObject, ObservableObject {
    static let shared = PushRouter()

    @Published var selectedTab: MainTab = .home

    /// 推送权限是否已经问过（只问一次，用户拒绝后不再骚扰）
    private static let askedPermissionKey = "push.permissionAsked"
    private var isListening = false

    private override init() {
        super.init()
    }

    // MARK: - 点击监听

    /// 注册点击监听。必须在 OneSignal.initialize 之后调用（App 启动时），否则冷启动那次点击会漏
    func start() {
        guard !isListening else { return }
        isListening = true
        OneSignal.Notifications.addClickListener(self)
    }

    // MARK: - 权限申请

    /// 首次进入主界面时申请一次推送权限：已问过 / 已有结论都不再弹
    func requestPermissionIfNeeded() {
        let defaults = UserDefaults.standard
        guard !defaults.bool(forKey: Self.askedPermissionKey) else { return }

        // canRequestPermission 只在系统还没问过（notDetermined）时为 true
        guard OneSignal.Notifications.canRequestPermission else {
            // 系统层面已有结论（授权或拒绝）：记下标记，之后不再问
            defaults.set(true, forKey: Self.askedPermissionKey)
            return
        }

        // 先落标记再弹：用户点允许或拒绝都只弹这一次
        defaults.set(true, forKey: Self.askedPermissionKey)
        OneSignal.Notifications.requestPermission { _ in }
    }

    // MARK: - 路由

    /// data["type"] → Tab；未知或缺失返回 nil（保持当前页面，不提示不崩）
    static func tab(for data: [AnyHashable: Any]?) -> MainTab? {
        guard let type = data?["type"] as? String else { return nil }
        switch type {
        case "anniversary": return .dates          // 重要日期
        case "wishlist": return .wishlist          // 心愿单
        case "couple_anniversary": return .home    // 主页
        default: return nil
        }
    }

    /// 处理一次点击。未登录时只是改选中态，不假设任何界面存在，所以不会崩
    func handle(data: [AnyHashable: Any]?) {
        guard let tab = Self.tab(for: data) else { return }
        DispatchQueue.main.async { [weak self] in
            self?.selectedTab = tab
        }
    }
}

extension PushRouter: OSNotificationClickListener {
    func onClick(event: OSNotificationClickEvent) {
        handle(data: event.notification.additionalData)
    }
}
