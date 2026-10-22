import PhotosUI
import SwiftUI

/// 「照片」：选图上传（进度 + 逐张结果）+ 最近几张（时间线网格是下一单）
struct PhotosView: View {
    @StateObject private var uploader = PhotoUploader()
    @StateObject private var app = AppState.shared

    @State private var selection: [PhotosPickerItem] = []
    @State private var recent: [Photo] = []
    @State private var listMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    pickerCard
                    progressCard
                    if let summary = uploader.summary {
                        resultCard(summary)
                    }
                    recentCard
                    Spacer(minLength: 24)
                }
                .padding(16)
            }
            .background(Color.oursBackground.opacity(0.3))
            .navigationTitle("照片")
            .navigationBarTitleDisplayMode(.inline)
            .task(id: app.couple?.id) { await loadRecent() }
            .refreshable { await loadRecent() }
            .onChange(of: selection) { _, items in
                guard !items.isEmpty else { return }
                Task {
                    await uploader.upload(items: items)
                    // 清空选择：下一批从零开始（onChange 会被空数组再次触发，上面已 guard）
                    selection = []
                    await loadRecent()
                }
            }
        }
    }

    // MARK: - 选图

    private var pickerCard: some View {
        VStack(spacing: 10) {
            PhotosPicker(
                selection: $selection,
                maxSelectionCount: PhotoUploadLimits.maxBatchSize,
                matching: .images,
                photoLibrary: .shared()
            ) {
                HStack(spacing: 6) {
                    Image(systemName: "photo.on.rectangle")
                    Text("选择照片（最多 \(PhotoUploadLimits.maxBatchSize) 张）")
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.oursPrimary)
            .disabled(uploader.isRunning)
            .accessibilityIdentifier("photos.picker")

            Text("先在手机上压成标准图 + 缩略图，再直传 OSS；原图不上传，GPS 与设备信息一并剥掉。")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(Color.oursBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    // MARK: - 进度

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

    // MARK: - 汇总（成功 N 张 / 失败 M 张 + 原因）

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

    // MARK: - 最简列表（只列文件名；时间线网格下一单做）

    private var recentCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("最近上传")
                    .font(.headline)
                Spacer()
                Text("\(recent.count) 张")
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(.secondary)
            }

            if let listMessage {
                Text(listMessage)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else if recent.isEmpty {
                Text("还没有照片，选几张试试")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else {
                ForEach(recent) { photo in
                    HStack(spacing: 8) {
                        Image(systemName: "photo")
                            .foregroundStyle(Color.oursPrimary)
                        Text(photo.originalName)
                            .font(.footnote)
                            .lineLimit(1)
                        Spacer()
                        Text(ByteCountFormatter.string(fromByteCount: Int64(photo.fileSize), countStyle: .file))
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(.secondary)
                    }
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
    private func loadRecent() async {
        guard let coupleId = app.couple?.id else {
            recent = []
            listMessage = "还没和另一半绑定，绑定后才能上传照片"
            return
        }
        do {
            recent = try await APIClient.fetchRecentPhotos(coupleId: coupleId)
            listMessage = nil
        } catch {
            recent = []
            listMessage = "照片列表没加载出来（\(error.localizedDescription)），下拉可以重试"
        }
    }
}
