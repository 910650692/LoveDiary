import SwiftUI

/// 主界面五个 Tab：主页 / 重要日期 / 心愿单 / 我们 / 照片
struct MainTabView: View {
    /// 选中态提到 PushRouter：通知点击跳到哪个 Tab 由它决定
    @StateObject private var router = PushRouter.shared

    var body: some View {
        TabView(selection: $router.selectedTab) {
            HomeView()
                .tabItem {
                    Label("主页", systemImage: "heart.fill")
                }
                .tag(MainTab.home)

            DatesView()
                .tabItem {
                    Label("纪念日", systemImage: "calendar")
                }
                .tag(MainTab.dates)

            WishlistView()
                .tabItem {
                    Label("心愿单", systemImage: "gift.fill")
                }
                .tag(MainTab.wishlist)

            OursView()
                .tabItem {
                    Label("我们", systemImage: "person.2.fill")
                }
                .tag(MainTab.me)

            PhotosView()
                .tabItem {
                    Label("照片", systemImage: "photo.on.rectangle")
                }
                .tag(MainTab.photos)
        }
        .tint(Color.oursPrimary)
        // 进主界面才申请推送权限（只弹一次，见 PushRouter.requestPermissionIfNeeded）
        .task { router.requestPermissionIfNeeded() }
    }
}
