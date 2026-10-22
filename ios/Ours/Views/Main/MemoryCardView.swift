import SwiftUI
import UIKit

/// 往昔重现（On This Day）：往年今天前后拍的一张照片，做成每天都会变的一张卡
///
/// 两种状态共用同一张卡片，**加载中也按空态画**：高度不跳、不闪转圈，
/// 拿到数据再整体换成照片——不会先闪一句「还没加载完」之类的中间文案。
struct MemoryCardView: View {
    /// 关系 id；nil（还没绑定）直接空态，不发请求
    let coupleId: UUID?
    /// 主页下拉刷新时自增：靠 `.task(id:)` 重新查询
    var reloadToken: Int = 0

    @State private var memory: Memory?

    /// 命中的那张照片 + 它属于「几年前的今天」
    private struct Memory {
        let photo: Photo
        let yearsAgo: Int
    }

    private var reloadKey: String { "\(coupleId?.uuidString ?? "-")#\(reloadToken)" }

    var body: some View {
        Group {
            if let memory {
                NavigationLink {
                    // 详情页里删掉这张照片后卡片回到空态（详情页先 dismiss 再回调，见它的注释）
                    PhotoDetailView(photo: memory.photo) { _ in self.memory = nil }
                } label: {
                    card(memory)
                }
                .buttonStyle(.plain)
            } else {
                emptyCard
            }
        }
        .task(id: reloadKey) { await load() }
    }

    // MARK: - 有照片

    private func card(_ memory: Memory) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            MemoryThumbnail(photo: memory.photo)
                .frame(height: 180)

            VStack(alignment: .leading, spacing: 4) {
                Text(Self.title(for: memory.yearsAgo))
                    .font(.headline)
                Text(Self.dateText(memory.photo.takenAt))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
        }
        .background(Color.white.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .contentShape(Rectangle())
    }

    // MARK: - 没照片（含加载中 / 离线 / 失败）

    private var emptyCard: some View {
        VStack(spacing: 10) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 36))
                .foregroundStyle(Color.oursSecondary)
            Text("还没有往年的今天")
                .font(.headline)
            Text("从今年开始攒吧，明年的今天这张卡就会亮起来")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .background(Color.white.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    // MARK: - 数据

    @MainActor
    private func load() async {
        guard let coupleId else {
            memory = nil
            return
        }
        do {
            let photo = try await APIClient.fetchOnThisDayPhotos(coupleId: coupleId)
            memory = photo.map { Memory(photo: $0, yearsAgo: Self.yearsAgo(of: $0)) }
        } catch {
            // 离线 / 服务端失败：静默保留当前内容（首次加载时就是空态），不弹错误，
            // 用户下拉刷新即可重试——和主页倒计时的兜底同一个思路
        }
    }

    /// 「几年前的今天」：用**年份差**而不是相差多少个整年
    /// （12/31 的照片与 1/1 的今天差不到一年，按整年会算成 0 年前）
    private static func yearsAgo(of photo: Photo, calendar: Calendar = .current, now: Date = Date()) -> Int {
        guard let takenAt = photo.takenAt else { return 1 }
        return max(calendar.component(.year, from: now) - calendar.component(.year, from: takenAt), 1)
    }

    private static func title(for yearsAgo: Int) -> String {
        yearsAgo <= 1 ? "去年的今天" : "\(yearsAgo) 年前的今天"
    }

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "yyyy年M月d日"
        return f
    }()

    private static func dateText(_ date: Date?) -> String {
        date.map(dateFormatter.string(from:)) ?? ""
    }
}

/// 卡片里的缩略图：已解码的先走内存缓存，其余交给 PhotoURLStore
/// （签名 URL 900 秒缓存 + 过期/403 重签一次都在它里面，这里不重复实现）
private struct MemoryThumbnail: View {
    let photo: Photo

    @State private var image: UIImage?

    var body: some View {
        Color.oursBackground
            .overlay {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                } else {
                    Image(systemName: "photo")
                        .font(.largeTitle)
                        .foregroundStyle(Color.oursPrimary.opacity(0.4))
                }
            }
            .clipped()
            .task(id: photo.id) { await load() }
    }

    private func load() async {
        if let key = photo.objectKey(for: "thumb"),
           let cached = PhotoURLStore.shared.cachedImage(for: key) {
            image = cached
            return
        }
        image = await PhotoURLStore.shared.image(for: photo, variant: "thumb")
    }
}
