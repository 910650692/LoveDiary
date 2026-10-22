import PhotosUI
import SwiftUI
import UIKit

/// 「照片」：时间线缩略图墙（keyset 分页）+ 右上角选图上传（进度 + 逐张结果）
///
/// 契约见 supabase/API.md「照片上传（阶段 4）」：
/// - 列表只拉缩略图（thumb_key），标准图/原图点开才拉
/// - 签名 URL 有效期 900 秒，过期会 403 → PhotoURLStore 重签一次再试
struct PhotosView: View {
    @StateObject private var uploader = PhotoUploader()
    @StateObject private var app = AppState.shared

    @State private var selection: [PhotosPickerItem] = []
    @State private var photos: [Photo] = []
    /// 下一页游标（nil 且 hasMore = 第一页还没拉过）
    @State private var cursor: PhotoCursor?
    @State private var hasMore = true
    @State private var isLoadingPage = false
    @State private var listMessage: String?

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 3)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if uploader.isRunning || uploader.batchFailure != nil {
                        progressCard
                    }
                    if let summary = uploader.summary {
                        resultCard(summary)
                    }
                    timelineSection
                    Spacer(minLength: 24)
                }
                .padding(16)
            }
            .background(Color.oursBackground.opacity(0.3))
            .navigationTitle("照片")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    PhotosPicker(
                        selection: $selection,
                        maxSelectionCount: PhotoUploadLimits.maxBatchSize,
                        matching: .images,
                        photoLibrary: .shared()
                    ) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                    }
                    .tint(Color.oursPrimary)
                    .accessibilityIdentifier("photos.picker")
                    .accessibilityLabel("添加照片")
                    .disabled(uploader.isRunning)
                }
            }
            .task(id: app.couple?.id) { await reload() }
            .refreshable { await reload() }
            .onChange(of: selection) { _, items in
                guard !items.isEmpty else { return }
                Task {
                    await uploader.upload(items: items)
                    // 清空选择：下一批从零开始（onChange 会被空数组再次触发，上面已 guard）
                    selection = []
                    await reload()
                }
            }
        }
    }

    // MARK: - 时间线

    private var timelineSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                // T3 靠这行文案定位照片页，别改
                Text("最近上传")
                    .font(.headline)
                Spacer()
                if !photos.isEmpty {
                    Text("\(photos.count) 张")
                        .font(.footnote.monospacedDigit())
                        .foregroundStyle(.secondary)
                        .accessibilityIdentifier("photos.count")
                }
            }

            if let listMessage {
                Text(listMessage)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else if photos.isEmpty {
                if isLoadingPage {
                    HStack(spacing: 8) {
                        ProgressView().tint(Color.oursPrimary)
                        Text("正在加载…")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                } else {
                    emptyState
                }
            } else {
                grid
                footer
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.oursBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var grid: some View {
        LazyVGrid(columns: columns, spacing: 4) {
            ForEach(photos) { photo in
                NavigationLink {
                    PhotoDetailView(photo: photo)
                } label: {
                    PhotoThumbnail(photo: photo)
                }
                .buttonStyle(.plain)
            }
        }
    }

    /// 到底自动拉下一页：footer 一露出来就说明滚到底了
    private var footer: some View {
        Group {
            if hasMore {
                Button {
                    Task { await loadNextPage() }
                } label: {
                    HStack(spacing: 6) {
                        ProgressView().tint(Color.oursPrimary)
                        Text("正在加载更多…")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .buttonStyle(.plain)
                .onAppear { Task { await loadNextPage() } }
            } else {
                Text("没有更多了")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.largeTitle)
                .foregroundStyle(Color.oursSecondary)
            Text("还没有照片，点点右上角加一张")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier("photos.empty")
            Text("选好的照片先在手机上压成标准图 + 缩略图，再直传 OSS；GPS 与设备信息一并剥掉。")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
    }

    // MARK: - 上传进度 / 结果

    private var progressCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(uploader.isRunning ? "正在上传…" : "上传结束")
                    .font(.headline)
                Spacer()
                Text("已上传 \(uploader.completed)/\(uploader.total)")
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(.secondary)
                    .accessibilityIdentifier("photos.progress")
            }

            ProgressView(value: Double(uploader.completed), total: Double(max(uploader.total, 1)))
                .tint(Color.oursPrimary)

            if let batchFailure = uploader.batchFailure {
                Text(batchFailure)
                    .font(.footnote)
                    .foregroundStyle(Color.oursPrimary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.oursBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func resultCard(_ summary: PhotoUploadSummary) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("成功 \(summary.succeeded.count) 张 / 失败 \(summary.failed.count) 张")
                .font(.headline)
                .accessibilityIdentifier("photos.summary")

            if summary.failed.isEmpty {
                Text("整批都传上来了 🎉")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else {
                ForEach(summary.failed) { item in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.originalName)
                            .font(.footnote)
                        Text(item.failureReason ?? "未知原因")
                            .font(.caption)
                            .foregroundStyle(Color.oursPrimary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.oursBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    // MARK: - 数据

    @MainActor
    private func reload() async {
        guard app.couple?.id != nil else {
            photos = []
            cursor = nil
            hasMore = false
            listMessage = "还没和另一半绑定，绑定后才能上传照片"
            return
        }
        photos = []
        cursor = nil
        hasMore = true
        listMessage = nil
        await loadNextPage()
    }

    @MainActor
    private func loadNextPage() async {
        guard let coupleId = app.couple?.id, hasMore, !isLoadingPage else { return }
        isLoadingPage = true
        defer { isLoadingPage = false }

        let requested = cursor
        do {
            let page = try await APIClient.fetchPhotoPage(coupleId: coupleId, cursor: requested)
            // 游标边界上可能把上一页最后一行再取回来一次，按 id 去重
            let known = Set(photos.map(\.id))
            let fresh = page.filter { !known.contains($0.id) }
            photos.append(contentsOf: fresh)
            cursor = nextCursor(after: page, requested: requested)
            hasMore = cursor != nil
            listMessage = nil
            // 这一页的缩略图签名一次批量拿回来（30 张 = 2 次 HTTP），格子不用一张张等签名
            await PhotoURLStore.shared.prefetch(fresh, variant: "thumb")
        } catch {
            hasMore = false
            cursor = nil
            if photos.isEmpty {
                listMessage = "照片列表没加载出来（\(error.localizedDescription)），下拉可以重试"
            }
        }
    }

    /// 下一页游标；nil = 已经到底
    private func nextCursor(after page: [Photo], requested: PhotoCursor?) -> PhotoCursor? {
        if page.isEmpty {
            // 「有拍摄时间」那段取空不代表尽头：后面还有 taken_at 为 null 的一段
            if case .takenAfter = requested { return .nullHead }
            return nil
        }
        guard let last = page.last, let createdAt = last.createdAt else { return nil }
        let isFullPage = page.count >= PhotoReadLimits.pageSize
        if let takenAt = last.takenAt {
            // 没满一页 = 有拍摄时间的都取完了，下一站是 null 段
            return isFullPage
                ? .takenAfter(takenAt: takenAt, createdAt: createdAt, id: last.id)
                : .nullHead
        }
        return isFullPage ? .nullAfter(createdAt: createdAt, id: last.id) : nil
    }
}

/// 时间线里的一格缩略图：加载中给转圈，失败给图标，不让整屏灰块
private struct PhotoThumbnail: View {
    let photo: Photo

    @State private var image: UIImage?
    @State private var failed = false

    var body: some View {
        Color.oursBackground
            .aspectRatio(1, contentMode: .fit)
            .overlay {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                } else if failed {
                    Image(systemName: "photo")
                        .font(.title3)
                        .foregroundStyle(Color.oursPrimary.opacity(0.4))
                } else {
                    ProgressView().tint(Color.oursPrimary)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .contentShape(Rectangle())
            .task(id: photo.id) { await load() }
    }

    private func load() async {
        if let key = photo.objectKey(for: "thumb"),
           let cached = PhotoURLStore.shared.cachedImage(for: key) {
            image = cached
            return
        }
        let loaded = await PhotoURLStore.shared.image(for: photo, variant: "thumb")
        image = loaded
        failed = loaded == nil
    }
}

/// 签名 URL + 解码后图片的内存缓存（YAGNI：不为这个引第三方图片库）
///
/// 两件事：
/// 1. 签名 URL 按对象键缓存并记下过期时刻（900 秒的 URL 缓存到 840 秒就重签）
/// 2. 拉图 403（缓存的签名过期了）时重签一次再重试，别让用户看到一片灰块
@MainActor
final class PhotoURLStore {
    static let shared = PhotoURLStore()

    /// 提前 60 秒当过期：别让「刚到手的 URL 在路上就死了」
    private static let expiryMargin: TimeInterval = 60

    private var signatures: [String: (url: URL, expiresAt: Date)] = [:]
    private let images = NSCache<NSString, UIImage>()
    private var inFlight: [String: Task<UIImage?, Never>] = [:]

    private init() {
        images.countLimit = 200
    }

    // MARK: 签名

    /// 批量预热：把一页照片的某个变体一次签回来（已缓存且没过期的跳过）
    func prefetch(_ photos: [Photo], variant: String) async {
        let pending = photos.compactMap { photo -> PhotoReadRequest? in
            guard let key = photo.objectKey(for: variant), signature(for: key) == nil else { return nil }
            return PhotoReadRequest(photoId: photo.id, keys: [variant: key])
        }
        guard !pending.isEmpty,
              let signed = try? await APIClient.signPhotoReads(requests: pending, variants: [variant])
        else { return }

        let expiresAt = Date().addingTimeInterval(TimeInterval(signed.expiresIn) - Self.expiryMargin)
        for (photoId, urls) in signed.urls {
            guard let photo = photos.first(where: { $0.id == photoId }),
                  let key = photo.objectKey(for: variant),
                  let url = urls[variant] else { continue }
            signatures[key] = (url, expiresAt)
        }
    }

    /// 单张的 GET URL：缓存优先，缺失或过期就现场签一次
    func signedURL(for photo: Photo, variant: String) async -> URL? {
        guard let key = photo.objectKey(for: variant) else { return nil }
        if let cached = signature(for: key) { return cached }

        guard let signed = try? await APIClient.signPhotoReads(
            requests: [PhotoReadRequest(photoId: photo.id, keys: [variant: key])],
            variants: [variant]
        ), let url = signed.urls[photo.id]?[variant] else { return nil }

        signatures[key] = (url, Date().addingTimeInterval(TimeInterval(signed.expiresIn) - Self.expiryMargin))
        return url
    }

    private func signature(for key: String) -> URL? {
        guard let entry = signatures[key] else { return nil }
        guard entry.expiresAt > Date() else {
            signatures[key] = nil
            return nil
        }
        return entry.url
    }

    // MARK: 图片

    func cachedImage(for key: String) -> UIImage? {
        images.object(forKey: key as NSString)
    }

    /// 拉一张图：内存缓存 → 下载（403 重签一次）→ 缓存
    func image(for photo: Photo, variant: String) async -> UIImage? {
        guard let key = photo.objectKey(for: variant) else { return nil }
        if let cached = cachedImage(for: key) { return cached }
        if let running = inFlight[key] { return await running.value }

        // 同一张图可能被多个格子/详情页同时要，合并成一次下载
        let task = Task { [weak self] in
            await self?.download(photo: photo, variant: variant, key: key)
        }
        inFlight[key] = task
        let image = await task.value
        inFlight[key] = nil
        return image
    }

    private func download(photo: Photo, variant: String, key: String) async -> UIImage? {
        // 最多两轮：第一轮用缓存的签名，403 说明过期了，清掉签名重签一次再试
        for attempt in 0..<2 {
            guard let url = await signedURL(for: photo, variant: variant) else { return nil }
            do {
                let (data, response) = try await URLSession.shared.data(from: url)
                guard let http = response as? HTTPURLResponse else { return nil }
                if http.statusCode == 401 || http.statusCode == 403 {
                    signatures[key] = nil
                    continue
                }
                guard (200..<300).contains(http.statusCode),
                      let image = UIImage(data: data) else { return nil }
                images.setObject(image, forKey: key as NSString)
                return image
            } catch {
                // 网络抖动：第一轮再试一次，第二轮直接放弃（下拉刷新即可）
                if attempt == 1 { return nil }
            }
        }
        return nil
    }
}
