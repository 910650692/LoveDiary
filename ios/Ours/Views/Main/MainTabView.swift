import SwiftUI

/// 主界面三个 Tab：主页 / 重要日期 / 心愿单
struct MainTabView: View {
    @State private var selection = 0

    var body: some View {
        TabView(selection: $selection) {
            HomeView()
                .tabItem {
                    Label("主页", systemImage: "heart.fill")
                }
                .tag(0)

            DatesView()
                .tabItem {
                    Label("纪念日", systemImage: "calendar")
                }
                .tag(1)

            WishlistView()
                .tabItem {
                    Label("心愿单", systemImage: "gift.fill")
                }
                .tag(2)
        }
        .tint(Color.oursPrimary)
    }
}
