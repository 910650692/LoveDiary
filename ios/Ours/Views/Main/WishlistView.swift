import SwiftUI

/// 共同心愿单（阶段 2 功能，骨架占位）
struct WishlistView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Image(systemName: "gift.fill")
                    .font(.system(size: 40))
                    .foregroundStyle(Color.oursSecondary)
                Text("共同心愿单即将上线")
                    .font(.headline)
                Text("写下想一起做的事，期待未来")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .navigationTitle("心愿单")
        }
    }
}
