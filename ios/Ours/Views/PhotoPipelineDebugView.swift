#if DEBUG
import PhotosUI
import SwiftUI
import UIKit

/// DEBUG 专用：照片管线自测页（正式包整文件不编译）。
/// 选图 → 显示原图/标准图/缩略图三张 + 各自字节数与像素尺寸 + 耗时 + EXIF 拍摄时间 + GPS 自查。
struct PhotoPipelineDebugView: View {
    private struct Preview: Identifiable {
        let id = UUID()
        let title: String
        let image: UIImage
        let byteCount: Int
        let pixels: CGSize
    }

    @State private var selection: PhotosPickerItem?
    @State private var previews: [Preview] = []
    @State private var takenAtText = "—"
    @State private var elapsedText = "—"
    @State private var gpsText = "—"
    @State private var statusText = "选一张照片，看压缩后的尺寸、体积与耗时"
    @State private var isProcessing = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                picker
                summaryCard
                ForEach(previews) { preview in
                    card(preview)
                }
                if previews.isEmpty, !isProcessing {
                    Text("照片只在本地处理，不会上传（OSS 还没开通）。")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(16)
        }
        .background(Color.oursBackground.opacity(0.3))
        .navigationTitle("照片管线自测")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: selection) { _, item in
            guard let item else { return }
            Task { await handle(item) }
        }
    }

    private var picker: some View {
        PhotosPicker(selection: $selection, matching: .images) {
            HStack(spacing: 6) {
                Image(systemName: "photo.on.rectangle.angled")
                Text(isProcessing ? "处理中…" : "选择一张照片")
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
        }
        .buttonStyle(.borderedProminent)
        .tint(Color.oursPrimary)
        .disabled(isProcessing)
    }

    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(statusText)
                .font(.subheadline)
                .foregroundStyle(.primary)
            row("拍摄时间（EXIF）", takenAtText)
            row("管线耗时", elapsedText)
            row("GPS 自查", gpsText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.oursBackground)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func row(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title)
                .font(.footnote)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .font(.footnote.monospacedDigit())
                .foregroundStyle(.primary)
        }
    }

    private func card(_ preview: Preview) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(preview.title)
                .font(.headline)
            Image(uiImage: preview.image)
                .resizable()
                .scaledToFit()
                .frame(maxHeight: 220)
                .frame(maxWidth: .infinity)
                .background(Color.black.opacity(0.05))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            Text("\(Int(preview.pixels.width))×\(Int(preview.pixels.height)) px · \(byteText(preview.byteCount))")
                .font(.footnote.monospacedDigit())
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.oursBackground)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func byteText(_ count: Int) -> String {
        let text = ByteCountFormatter.string(fromByteCount: Int64(count), countStyle: .file)
        return "\(text)（\(count) bytes）"
    }

    // MARK: - 处理

    private struct Measured {
        let photo: PreparedPhoto
        let elapsedMS: Double
        let gpsPresent: Bool
    }

    private static let takenAtFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter
    }()

    @MainActor
    private func handle(_ item: PhotosPickerItem) async {
        isProcessing = true
        previews = []
        takenAtText = "—"
        elapsedText = "—"
        gpsText = "—"
        statusText = "正在读取原图…"
        defer { isProcessing = false }

        guard let data = try? await item.loadTransferable(type: Data.self) else {
            statusText = "读不到这张照片的数据"
            return
        }
        let original = UIImage(data: data)

        do {
            let measured = try await Task.detached(priority: .userInitiated) { () -> Measured in
                let started = CFAbsoluteTimeGetCurrent()
                let photo = try PhotoPipeline.prepare(from: data)
                let elapsed = (CFAbsoluteTimeGetCurrent() - started) * 1000
                return Measured(
                    photo: photo,
                    elapsedMS: elapsed,
                    gpsPresent: PhotoPipeline.containsGPSMetadata(photo.standard)
                )
            }.value

            var items: [Preview] = []
            if let original {
                items.append(Preview(title: "原图", image: original, byteCount: data.count, pixels: pixelSize(of: original)))
            }
            if let image = UIImage(data: measured.photo.standard) {
                items.append(Preview(
                    title: "标准图（长边 ≤2048，质量 0.8）",
                    image: image, byteCount: measured.photo.standard.count, pixels: pixelSize(of: image)
                ))
            }
            if let image = UIImage(data: measured.photo.thumbnail) {
                items.append(Preview(
                    title: "缩略图（长边 ≤400，质量 0.7）",
                    image: image, byteCount: measured.photo.thumbnail.count, pixels: pixelSize(of: image)
                ))
            }
            previews = items
            takenAtText = measured.photo.takenAt.map { Self.takenAtFormatter.string(from: $0) } ?? "无 EXIF 拍摄时间"
            elapsedText = String(format: "%.1f ms", measured.elapsedMS)
            gpsText = measured.gpsPresent ? "输出里还有 GPS（不应该）" : "输出无 GPS"
            statusText = "处理完成"
        } catch {
            statusText = "处理失败：\(error.localizedDescription)"
        }
    }

    /// 像素尺寸取 cgImage（UIImage.size 是点，和像素不是一回事）
    private func pixelSize(of image: UIImage) -> CGSize {
        if let cgImage = image.cgImage {
            return CGSize(width: cgImage.width, height: cgImage.height)
        }
        return CGSize(width: image.size.width * image.scale, height: image.size.height * image.scale)
    }
}
#endif
