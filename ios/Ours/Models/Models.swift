import Foundation

/// 数据模型，与 supabase/API.md 的表结构一一对应
/// supabase-swift 默认把 snake_case 字段解码为 camelCase，所以这里用 camelCase 属性

struct Profile: Codable, Identifiable, Hashable {
    let id: UUID
    var nickname: String
    var avatarURL: String?
    var createdAt: Date?
}

struct Couple: Codable, Identifiable, Hashable {
    let id: UUID
    let userAId: UUID
    let userBId: UUID?          // 绑定前为 nil
    let startedDate: String     // YYYY-MM-DD（date-only，用字符串避免时区歧义）
    let createdAt: Date?
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
}
