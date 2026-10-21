import CoreGraphics
import Foundation
import ImageIO

/// 一张照片经过本地管线后的产物（上传前用，不含网络）
struct PreparedPhoto {
    /// 标准图：长边 ≤ maxLongEdgeStandard，JPEG 质量 0.8（约 300–600KB）
    let standard: Data
    /// 缩略图：长边 ≤ maxLongEdgeThumb，JPEG 质量 0.7（约 30–60KB）
    let thumbnail: Data
    /// EXIF 拍摄时间；源图没有 DateTimeOriginal 时为 nil
    let takenAt: Date?
    /// 标准图"摆正后"的像素宽高（已应用 EXIF orientation）
    let pixelSize: CGSize
}

enum PhotoPipelineError: LocalizedError {
    case emptyData
    case unreadableImage
    case encodeFailed

    var errorDescription: String? {
        switch self {
        case .emptyData: return "图片数据为空"
        case .unreadableImage: return "无法解码这张图片"
        case .encodeFailed: return "JPEG 编码失败"
        }
    }
}

/// 照片处理管线：原图数据 → 标准图 + 缩略图 + 拍摄时间。
///
/// 全程只用 CoreGraphics / ImageIO（不碰 UIImage），因此同一份代码能在 macOS 命令行里跑真实测量。
/// 输出 JPEG 不带源图任何元数据：GPS、设备型号、镜头信息都不会写出去。
/// 纯 CPU 同步调用，不做 UI 与网络；调用方自己决定放在哪个队列。
enum PhotoPipeline {
    /// 标准图 JPEG 质量（PHOTOS_DESIGN §5）
    static let standardQuality: CGFloat = 0.8
    /// 缩略图 JPEG 质量（PHOTOS_DESIGN §5）
    static let thumbnailQuality: CGFloat = 0.7

    static func prepare(
        from data: Data,
        maxLongEdgeStandard: CGFloat = 2048,
        maxLongEdgeThumb: CGFloat = 400
    ) throws -> PreparedPhoto {
        guard !data.isEmpty else { throw PhotoPipelineError.emptyData }
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else {
            throw PhotoPipelineError.unreadableImage
        }

        let takenAt = shotDate(from: properties(of: source))
        let standardImage = try downsampled(source: source, maxPixelSize: maxLongEdgeStandard)
        let thumbImage = try downsampled(source: source, maxPixelSize: maxLongEdgeThumb)

        return PreparedPhoto(
            standard: try encodeJPEG(standardImage, quality: standardQuality),
            thumbnail: try encodeJPEG(thumbImage, quality: thumbnailQuality),
            takenAt: takenAt,
            pixelSize: CGSize(width: standardImage.width, height: standardImage.height)
        )
    }

    /// 输出里是否存在 GPS 元数据（验收自查用；正常产物应为 false）
    static func containsGPSMetadata(_ data: Data) -> Bool {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let all = properties(of: source)
        else { return false }
        return all[kCGImagePropertyGPSDictionary as String] != nil
    }

    // MARK: - 解码（降采样，不先解出全尺寸位图）

    /// ImageIO 直接按长边上限解码：
    /// - kCGImageSourceThumbnailMaxPixelSize 让解码器只解出需要的尺寸，4032×3024 不会占满内存
    /// - kCGImageSourceCreateThumbnailWithTransform 应用 EXIF orientation，输出已经是摆正的像素
    private static func downsampled(source: CGImageSource, maxPixelSize: CGFloat) throws -> CGImage {
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: max(1, maxPixelSize),
            kCGImageSourceShouldCacheImmediately: true,
        ]
        guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else {
            throw PhotoPipelineError.unreadableImage
        }
        return image
    }

    // MARK: - 编码（不传元数据 = 剥离 GPS 与设备信息）

    private static func encodeJPEG(_ image: CGImage, quality: CGFloat) throws -> Data {
        let output = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(
            output, "public.jpeg" as CFString, 1, nil
        ) else { throw PhotoPipelineError.encodeFailed }

        // 只给质量参数、不给任何 properties：EXIF/GPS/设备信息自然不会被写进输出
        CGImageDestinationAddImage(
            destination, image,
            [kCGImageDestinationLossyCompressionQuality: quality] as CFDictionary
        )
        guard CGImageDestinationFinalize(destination) else {
            throw PhotoPipelineError.encodeFailed
        }
        return output as Data
    }

    // MARK: - 拍摄时间

    private static func properties(of source: CGImageSource) -> [String: Any]? {
        CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [String: Any]
    }

    /// EXIF 拍摄时间字段（格式 yyyy:MM:dd HH:mm:ss）；
    /// 带 OffsetTimeOriginal 就用它折算，不带则按本机时区理解
    private static func shotDate(from properties: [String: Any]?) -> Date? {
        guard let exif = properties?[kCGImagePropertyExifDictionary as String] as? [String: Any],
              let text = exif[kCGImagePropertyExifDateTimeOriginal as String] as? String
        else { return nil }

        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        // 每次新建 formatter：管线要能并发调用，不共享可变实例
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy:MM:dd HH:mm:ss"
        formatter.timeZone = (exif[kCGImagePropertyExifOffsetTimeOriginal as String] as? String)
            .flatMap(timeZone(fromEXIFOffset:)) ?? .current
        return formatter.date(from: trimmed)
    }

    /// EXIF 时区偏移（"+08:00" / "-0500" 两种都认），认不出返回 nil
    private static func timeZone(fromEXIFOffset offset: String) -> TimeZone? {
        let trimmed = offset.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let sign = trimmed.first, sign == "+" || sign == "-" else { return nil }
        let digits = trimmed.dropFirst().replacingOccurrences(of: ":", with: "")
        guard digits.count == 4,
              let hours = Int(digits.prefix(2)),
              let minutes = Int(digits.suffix(2)),
              hours <= 23, minutes <= 59
        else { return nil }
        let seconds = (hours * 3600 + minutes * 60) * (sign == "-" ? -1 : 1)
        return TimeZone(secondsFromGMT: seconds)
    }
}
