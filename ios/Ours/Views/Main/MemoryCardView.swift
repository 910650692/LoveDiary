import SwiftUI

/// 随机回忆卡（占位版）
/// 阶段 4 照片上线后：随机展示一张照片 + 日期 + 相册名
struct MemoryCardView: View {
    private let placeholders = [
        ("回忆一下我们的第一天", "照片功能即将上线，先让回忆在心里发芽 🌱"),
        ("还记得第一次约会吗", "照片功能即将上线，先让回忆在心里发芽 🌱"),
        ("想 Ta 的时候，想想当初", "照片功能即将上线，先让回忆在心里发芽 🌱"),
    ]

    var body: some View {
        let item = placeholders[Int.random(in: 0..<placeholders.count)]
        VStack(spacing: 10) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 36))
                .foregroundStyle(Color.oursSecondary)
            Text(item.0)
                .font(.headline)
            Text(item.1)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .background(Color.white.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}
