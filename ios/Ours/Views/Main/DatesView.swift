import SwiftUI

/// 重要日期（阶段 2 功能，骨架占位）
struct DatesView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Image(systemName: "calendar.badge.plus")
                    .font(.system(size: 40))
                    .foregroundStyle(Color.oursSecondary)
                Text("重要日期即将上线")
                    .font(.headline)
                Text("纪念日、生日，一个都不错过")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .navigationTitle("纪念日")
        }
    }
}
