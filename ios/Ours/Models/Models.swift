import Foundation

/// 数据模型，与 supabase/API.md 的表结构一一对应
/// 注意：supabase-swift 的 PostgREST 客户端**不做** camelCase/snake_case 自动转换
/// （只有 Auth 模块开了 .convertFromSnakeCase），所以表模型必须用 CodingKeys 显式映射列名

struct Profile: Codable, Identifiable, Hashable {
    let id: UUID
    var nickname: String
    var avatarURL: String?      // DB 列名：avatar_url
    var createdAt: Date?        // DB 列名：created_at

    enum CodingKeys: String, CodingKey {
        case id
        case nickname
        case avatarURL = "avatar_url"
        case createdAt = "created_at"
    }
}

struct Couple: Codable, Identifiable, Hashable {
    let id: UUID
    let userAId: UUID
    let userBId: UUID?          // 绑定前为 nil
    let startedDate: String     // YYYY-MM-DD（date-only，用字符串避免时区歧义）
    let createdAt: Date?

    enum CodingKeys: String, CodingKey {
        case id
        case userAId = "user_a_id"
        case userBId = "user_b_id"
        case startedDate = "started_date"
        case createdAt = "created_at"
    }
}

struct Anniversary: Codable, Identifiable, Hashable {
    let id: UUID
    let coupleId: UUID
    let createdBy: UUID
    var name: String
    var date: String            // YYYY-MM-DD
    var notes: String?
    var enableNotification: Bool
    var createdAt: Date?
    var updatedAt: Date?

    enum CodingKeys: String, CodingKey {
        case id
        case coupleId = "couple_id"
        case createdBy = "created_by"
        case name
        case date
        case notes
        case enableNotification = "enable_notification"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
}

struct WishlistItem: Codable, Identifiable, Hashable {
    let id: UUID
    let coupleId: UUID
    let creatorId: UUID
    var completerId: UUID?
    var title: String
    var description: String?
    var status: String          // PENDING / COMPLETED
    var completedAt: Date?
    var isDeleted: Bool
    var createdAt: Date?
    var updatedAt: Date?

    /// 是否已完成
    var isCompleted: Bool { status == "COMPLETED" }

    enum CodingKeys: String, CodingKey {
        case id
        case coupleId = "couple_id"
        case creatorId = "creator_id"
        case completerId = "completer_id"
        case title
        case description
        case status
        case completedAt = "completed_at"
        case isDeleted = "is_deleted"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
}

/// photos 表一行（阶段 4）。object_key / thumb_key 是 OSS 对象键：不含 bucket、域名与签名参数
struct Photo: Codable, Identifiable, Hashable {
    let id: UUID
    let coupleId: UUID
    let creatorId: UUID
    let objectKey: String       // DB 列名：object_key（标准图）
    let thumbKey: String?       // DB 列名：thumb_key（缩略图）
    let originalKey: String?    // DB 列名：original_key（「保留原图」才有）
    let originalName: String    // DB 列名：original_name
    let fileSize: Int           // DB 列名：file_size（标准图字节数）
    let mimeType: String        // DB 列名：mime_type
    let width: Int?
    let height: Int?
    let takenAt: Date?          // DB 列名：taken_at（EXIF 拍摄时间）
    let createdAt: Date?        // DB 列名：created_at

    enum CodingKeys: String, CodingKey {
        case id
        case coupleId = "couple_id"
        case creatorId = "creator_id"
        case objectKey = "object_key"
        case thumbKey = "thumb_key"
        case originalKey = "original_key"
        case originalName = "original_name"
        case fileSize = "file_size"
        case mimeType = "mime_type"
        case width
        case height
        case takenAt = "taken_at"
        case createdAt = "created_at"
    }
}

/// `oss-sign` 的响应：一次一批的预签名 URL（契约见 supabase/API.md「照片上传（阶段 4）」）
struct PhotoSignBatch: Decodable {
    let coupleId: UUID
    let expiresIn: Int
    let items: [Item]

    struct Item: Decodable {
        let photoId: UUID
        /// variant → 预签名 PUT URL（"standard" / "thumb" / "original"）
        let putUrls: [String: String]
        let getUrl: String

        /// 由预签名 URL 反推 OSS 对象键：path 才是键，query 是签名参数
        func objectKey(for variant: String) -> String? {
            guard let value = putUrls[variant], let url = URL(string: value) else { return nil }
            let path = url.path
            return path.hasPrefix("/") ? String(path.dropFirst()) : path
        }
    }
}
