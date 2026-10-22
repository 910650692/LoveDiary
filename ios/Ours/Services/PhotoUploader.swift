import Foundation
import PhotosUI
import SwiftUI

/// 单张照片的上传结果：成功与失败都留一条，界面才能逐张说清原因
struct PhotoUploadResult: Identifiable {
    /// 客户端生成的 photoId，也是 photos 行的 id（服务端对象键里含它）
    let id: UUID
    let originalName: String
    /// 失败原因；nil 表示这一张整条链路都成功（预处理 + 标准图/缩略图直传 + 插库）
    let failureReason: String?

    var isSuccess: Bool { failureReason == nil }
}

/// 一批上传的逐张汇总（不做整批一刀切）
struct PhotoUploadSummary {
    let results: [PhotoUploadResult]

    var succeeded: [PhotoUploadResult] { results.filter(\.isSuccess) }
    var failed: [PhotoUploadResult] { results.filter { !$0.isSuccess } }
}

enum PhotoUploadError: LocalizedError {
    case unreadableItem
    case missingSignedURL(variant: String)
    case http(status: Int, body: String)

    var errorDescription: String? {
        switch self {
        case .unreadableItem:
            return "读不到这张照片的数据"
        case .missingSignedURL(let variant):
            return "没拿到\(variant)的预签名地址"
        case .http(let status, let body):
            let detail = body.trimmingCharacters(in: .whitespacesAndNewlines)
            let snippet = detail.count > 120 ? String(detail.prefix(120)) + "…" : detail
            if status == 0 { return snippet.isEmpty ? "直传失败" : "直传失败：\(snippet)" }
            return snippet.isEmpty ? "直传失败（HTTP \(status)）" : "直传失败（HTTP \(status)）：\(snippet)"
        }
    }
}

/// 上传链路的硬约束。不放在 @MainActor 类里：非隔离的直传函数也要能读
enum PhotoUploadLimits {
    /// 单批上限（oss-sign 服务端限制）
    static let maxBatchSize = 20
    /// 直传并发上限（PHOTOS_DESIGN §5「同时上传 2 张」）
    static let maxConcurrentUploads = 2
    /// 单张直传尝试次数（PHOTOS_DESIGN §5「失败指数退避重试 3 次」）
    static let putAttemptLimit = 3
}

/// 上传编排：PhotoPipeline → oss-sign（**一批一次**）→ URLSession PUT 直传 OSS → 插 photos 行。
///
/// 约定来自 supabase/API.md「照片上传（阶段 4）」与 PHOTOS_DESIGN §5：
/// - 签名一批一次：服务端一次最多签 20 张，逐张签名把跨境 RTT 乘以张数
/// - 直传并发上限 2；单张失败只记这一张，绝不中断整批
/// - photoId 客户端生成并一路用到底：对象键由服务端拼 `…/<photoId>.jpg`，插库也拿它当 id
@MainActor
final class PhotoUploader: ObservableObject {
    @Published private(set) var isRunning = false
    @Published private(set) var total = 0
    @Published private(set) var completed = 0
    @Published private(set) var summary: PhotoUploadSummary?
    /// 整批级别的失败（还没绑定 / 签名接口挂了 / 登录态失效），逐张失败走 summary
    @Published private(set) var batchFailure: String?

    /// 本地预处理产物
    private struct Prepared {
        let id: UUID
        let originalName: String
        let photo: PreparedPhoto
    }

    // MARK: - 一批

    func upload(items: [PhotosPickerItem]) async {
        guard !items.isEmpty, !isRunning else { return }
        isRunning = true
        summary = nil
        batchFailure = nil
        total = items.count
        completed = 0
        defer { isRunning = false }

        var results: [PhotoUploadResult] = []

        // 1) 本地预处理（压缩 / 缩略图 / EXIF）：单张解码失败只记这一张
        var prepared: [Prepared] = []
        for (index, item) in items.enumerated() {
            do {
                prepared.append(try await Self.prepare(item, index: index))
            } catch {
                results.append(PhotoUploadResult(
                    id: UUID(),
                    originalName: "第 \(index + 1) 张",
                    failureReason: error.localizedDescription
                ))
                completed += 1
            }
        }
        guard !prepared.isEmpty else {
            finish(results)
            return
        }

        // 2) 签名：整批一次请求（这一步是「一批一次」的关键，别挪进下面的循环里）
        let batch: PhotoSignBatch
        do {
            batch = try await APIClient.signPhotoUploads(photoIds: prepared.map(\.id))
        } catch {
            batchFailure = error.localizedDescription
            results.append(contentsOf: prepared.map {
                PhotoUploadResult(id: $0.id, originalName: $0.originalName, failureReason: error.localizedDescription)
            })
            completed += prepared.count
            finish(results)
            return
        }
        let signed = Dictionary(batch.items.map { ($0.photoId, $0) }, uniquingKeysWith: { first, _ in first })

        // 3) creator_id 要写进 photos 行，RLS 要求它等于 auth.uid()
        let creatorId: UUID
        do {
            creatorId = try await APIClient.currentUserId()
        } catch {
            let reason = "登录状态已失效，请重新登录后再上传"
            batchFailure = reason
            results.append(contentsOf: prepared.map {
                PhotoUploadResult(id: $0.id, originalName: $0.originalName, failureReason: reason)
            })
            completed += prepared.count
            finish(results)
            return
        }

        // 4) 直传 + 插库：窗口里始终最多 maxConcurrentUploads 张
        var nextIndex = 0
        func nextJob() -> Prepared? {
            guard nextIndex < prepared.count else { return nil }
            defer { nextIndex += 1 }
            return prepared[nextIndex]
        }

        await withTaskGroup(of: PhotoUploadResult.self) { group in
            for _ in 0..<PhotoUploadLimits.maxConcurrentUploads {
                guard let job = nextJob() else { break }
                let sign = signed[job.id]
                group.addTask {
                    await Self.perform(job, sign: sign, coupleId: batch.coupleId, creatorId: creatorId)
                }
            }
            while let result = await group.next() {
                results.append(result)
                completed += 1
                if let job = nextJob() {
                    let sign = signed[job.id]
                    group.addTask {
                        await Self.perform(job, sign: sign, coupleId: batch.coupleId, creatorId: creatorId)
                    }
                }
            }
        }

        finish(results)
    }

    /// 整批收尾：唯一的「批次结束」出口，所以通知也放在这里——一批只可能走一次。
    private func finish(_ results: [PhotoUploadResult]) {
        summary = PhotoUploadSummary(results: results)

        // 一批一条：按本批成功张数通知另一半（不是逐张，进不了下面的逐张链路）。
        // 0 张成功就不推：没东西可看，推了只是噪音。失败静默——照片已经在库里了。
        let succeeded = results.filter(\.isSuccess).count
        guard succeeded > 0 else { return }
        Task {
            try? await APIClient.notifyPhotos(count: succeeded)
        }
    }

    // MARK: - 单张（不碰 UI 状态，跑在后台）

    /// 一张：PUT 标准图 + 缩略图 → 插 photos 行。任何一步失败都只返回这一张的失败
    nonisolated private static func perform(
        _ prepared: Prepared,
        sign: PhotoSignBatch.Item?,
        coupleId: UUID,
        creatorId: UUID
    ) async -> PhotoUploadResult {
        func failure(_ reason: String) -> PhotoUploadResult {
            PhotoUploadResult(id: prepared.id, originalName: prepared.originalName, failureReason: reason)
        }

        guard let sign else { return failure("签名结果里没有这一张") }
        guard let standardKey = sign.objectKey(for: "standard"),
              let standardValue = sign.putUrls["standard"],
              let standardURL = URL(string: standardValue)
        else { return failure(PhotoUploadError.missingSignedURL(variant: "标准图").localizedDescription) }

        let thumbURL = sign.putUrls["thumb"].flatMap(URL.init(string:))

        do {
            try await put(standardURL, data: prepared.photo.standard)
            if let thumbURL {
                try await put(thumbURL, data: prepared.photo.thumbnail)
            }
            _ = try await APIClient.insertPhoto(
                id: prepared.id,
                coupleId: coupleId,
                creatorId: creatorId,
                objectKey: standardKey,
                thumbKey: thumbURL == nil ? nil : sign.objectKey(for: "thumb"),
                originalName: prepared.originalName,
                fileSize: prepared.photo.standard.count,
                width: Int(prepared.photo.pixelSize.width.rounded()),
                height: Int(prepared.photo.pixelSize.height.rounded()),
                takenAt: prepared.photo.takenAt
            )
            return PhotoUploadResult(id: prepared.id, originalName: prepared.originalName, failureReason: nil)
        } catch {
            return failure(error.localizedDescription)
        }
    }

    /// 直传单个对象：4xx 直接失败（签名过期 / 权限不对，重试也一样），网络错误与 5xx 退避重试
    nonisolated private static func put(_ url: URL, data: Data) async throws {
        var lastError: Error?

        for attempt in 1...PhotoUploadLimits.putAttemptLimit {
            if attempt > 1 {
                // 0.5s、1s 两级退避
                try? await Task.sleep(nanoseconds: UInt64(attempt - 1) * 500_000_000)
            }
            do {
                try await putOnce(url, data: data)
                return
            } catch {
                if let uploadError = error as? PhotoUploadError,
                   case .http(let status, _) = uploadError,
                   (400..<500).contains(status) {
                    throw error
                }
                lastError = error
            }
        }
        throw lastError ?? PhotoUploadError.http(status: 0, body: "重试后仍然失败")
    }

    nonisolated private static func putOnce(_ url: URL, data: Data) async throws {
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.timeoutInterval = 60
        // 不设 Content-Type：ali-oss 的 signatureUrl 按空 Content-Type 签名，擅自带上会 SignatureDoesNotMatch
        let (body, response) = try await URLSession.shared.upload(for: request, from: data)
        guard let http = response as? HTTPURLResponse else {
            throw PhotoUploadError.http(status: 0, body: "服务器没有返回 HTTP 响应")
        }
        guard (200..<300).contains(http.statusCode) else {
            throw PhotoUploadError.http(status: http.statusCode, body: String(decoding: body.prefix(500), as: UTF8.self))
        }
    }

    // MARK: - 预处理

    nonisolated private static func prepare(_ item: PhotosPickerItem, index: Int) async throws -> Prepared {
        guard let data = try await item.loadTransferable(type: Data.self) else {
            throw PhotoUploadError.unreadableItem
        }
        // 压缩/缩略图是纯 CPU 活，挪出主线程
        let photo = try await Task.detached(priority: .userInitiated) {
            try PhotoPipeline.prepare(from: data)
        }.value
        return Prepared(id: UUID(), originalName: name(index: index, takenAt: photo.takenAt), photo: photo)
    }

    /// 文件名：PhotosPicker 不给原始文件名，用 EXIF 拍摄时间（没有就当前时间）+ 序号
    nonisolated private static func name(index: Int, takenAt: Date?) -> String {
        // 每次新建 formatter：这里可能并发调用，不共享可变实例
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "yyyyMMdd_HHmmss"
        return "IMG_\(formatter.string(from: takenAt ?? Date()))_\(index + 1).jpg"
    }
}
