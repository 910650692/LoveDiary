import SwiftUI
import UIKit

/// 照片详情：打开先看**标准图**（长边 2048 / 约 400KB），有原图才给「查看原图」
///
/// 交互约定（PHOTOS_DESIGN §9.2）：
/// - 默认显示标准图：点开即看，不等几 MB 的原图
/// - 只有 original_key 非空才出现「查看原图」，没有就不给假按钮
/// - 原图点了才拉（走 PhotoURLStore 的签名缓存，过期会重签一次）
struct PhotoDetailView: View {
    let photo: Photo
    /// 删成功后通知上一层把这个 id 从网格里摘掉（详情页自己 dismiss 回时间线）
    var onDeleted: ((UUID) -> Void)? = nil

    @Environment(\.dismiss) private var dismiss

    @State private var standard: UIImage?
    @State private var standardFailed = false
    @State private var original: UIImage?
    @State private var isLoadingOriginal = false
    @State private var originalMessage: String?

    @State private var isConfirmingDelete = false
    @State private var isDeleting = false
    @State private var deleteMessage: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                picture
                if photo.originalKey != nil {
                    originalSection
                }
                if let deleteMessage {
                    Text(deleteMessage)
                        .font(.footnote)
                        .foregroundStyle(Color.oursPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                infoCard
                Spacer(minLength: 8)
            }
            .padding(16)
        }
        .background(Color.oursBackground.opacity(0.3))
        .navigationTitle("照片详情")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(role: .destructive) {
                    isConfirmingDelete = true
                } label: {
                    if isDeleting {
                        ProgressView().tint(Color.oursPrimary)
                    } else {
                        Image(systemName: "trash")
                    }
                }
                .tint(Color.oursPrimary)
                .disabled(isDeleting)
                .accessibilityIdentifier("photoDetail.delete")
                .accessibilityLabel("删除照片")
            }
        }
        // 删除不可撤销：先把后果说清楚，再让用户点
        .confirmationDialog("删除这张照片？", isPresented: $isConfirmingDelete, titleVisibility: .visible) {
            Button("删除", role: .destructive) {
                Task { await delete() }
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("照片会从你们的相册里消失，无法恢复")
        }
        .task { await loadStandard() }
    }

    // MARK: - 标准图

    private var picture: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.oursBackground)

            if let standard {
                Image(uiImage: standard)
                    .resizable()
                    .scaledToFit()
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else if standardFailed {
                VStack(spacing: 8) {
                    Image(systemName: "photo")
                        .font(.largeTitle)
                        .foregroundStyle(Color.oursPrimary.opacity(0.4))
                    Text("这张图没加载出来，退出重进再试")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            } else {
                VStack(spacing: 8) {
                    ProgressView().tint(Color.oursPrimary)
                    Text("正在加载…")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(minHeight: 240)
    }

    // MARK: - 原图（仅 original_key 存在时）

    private var originalSection: some View {
        VStack(spacing: 10) {
            if let original {
                Image(uiImage: original)
                    .resizable()
                    .scaledToFit()
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                Text("原图")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else if isLoadingOriginal {
                // 原图 3~8MB，大陆网络要等几秒：给明确进度占位，别让界面像卡死
                VStack(spacing: 8) {
                    ProgressView().tint(Color.oursPrimary)
                    Text("正在加载原图…（大图要等几秒）")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 28)
                .background(Color.oursBackground)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .accessibilityIdentifier("photoDetail.originalLoading")
            } else {
                Button {
                    Task { await loadOriginal() }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.down.circle")
                        Text("查看原图")
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                }
                .buttonStyle(.bordered)
                .tint(Color.oursPrimary)
                .accessibilityIdentifier("photoDetail.original")

                if let originalMessage {
                    Text(originalMessage)
                        .font(.footnote)
                        .foregroundStyle(Color.oursPrimary)
                }
            }
        }
    }

    // MARK: - 信息

    private var infoCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            row("文件名", photo.originalName)
            if let takenAt = photo.takenAt {
                row("拍摄时间", Self.dateText(takenAt))
            }
            if let createdAt = photo.createdAt {
                row("上传时间", Self.dateText(createdAt))
            }
            if let width = photo.width, let height = photo.height {
                row("尺寸", "\(width) × \(height)")
            }
            row("标准图大小", ByteCountFormatter.string(fromByteCount: Int64(photo.fileSize), countStyle: .file))
            row("原图", photo.originalKey == nil ? "未保留" : "已保留")
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.oursBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func row(_ title: String, _ value: String) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(.secondary)
            Spacer(minLength: 12)
            Text(value)
                .font(.footnote.monospacedDigit())
                .multilineTextAlignment(.trailing)
        }
    }

    // MARK: - 数据

    @MainActor
    private func loadStandard() async {
        guard standard == nil else { return }
        standardFailed = false
        let loaded = await PhotoURLStore.shared.image(for: photo, variant: "standard")
        standard = loaded
        standardFailed = loaded == nil
    }

    @MainActor
    private func loadOriginal() async {
        guard !isLoadingOriginal, original == nil else { return }
        isLoadingOriginal = true
        originalMessage = nil
        defer { isLoadingOriginal = false }

        let loaded = await PhotoURLStore.shared.image(for: photo, variant: "original")
        original = loaded
        if loaded == nil {
            originalMessage = "原图没拉下来（可能是网络断了），再点一次试试"
        }
    }

    /// 删除编排：软删库行 → 删 OSS 对象（顺序在 APIClient.deletePhoto 里，注释写死了理由）→ 回时间线。
    ///
    /// OSS 那一步失败不会抛错（只记日志），所以这里 catch 到的只可能是「库行没删成」，
    /// 那种情况什么都没变，让用户重试是安全的。
    @MainActor
    private func delete() async {
        guard !isDeleting else { return }
        isDeleting = true
        deleteMessage = nil
        defer { isDeleting = false }

        do {
            try await APIClient.deletePhoto(photo)
            PhotoURLStore.shared.forget(photo)
            // 先退出详情、再让上层摘掉网格那一格：反过来的话，被推走的页面还在而 ForEach 已少一行，
            // SwiftUI 会自己 pop，容易和这次 dismiss 打架
            dismiss()
            onDeleted?(photo.id)
        } catch {
            deleteMessage = "删除没成功（\(error.localizedDescription)），再试一次"
        }
    }

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "yyyy年M月d日 HH:mm"
        return f
    }()

    private static func dateText(_ date: Date) -> String {
        dateFormatter.string(from: date)
    }
}
