import Foundation
import Supabase

/// 服务端接口封装（与 supabase/API.md 约定一致）
enum APIClient {
    static var client: SupabaseClient { SupabaseClientProvider.shared }

    // MARK: - Auth

    /// 注册；若项目开了邮箱确认，signUp 后 session 为空，需提示去邮箱确认
    static func signUp(email: String, password: String, nickname: String) async throws {
        _ = try await client.auth.signUp(
            email: email,
            password: password,
            data: ["nickname": .string(nickname)]
        )
    }

    static func signIn(email: String, password: String) async throws {
        try await client.auth.signIn(email: email, password: password)
    }

    static func signOut() async throws {
        try await client.auth.signOut()
    }

    /// 当前登录用户 id（写操作要记 creator_id / completer_id / created_by）
    static func currentUserId() async throws -> UUID {
        try await client.auth.session.user.id
    }

    // MARK: - 情侣绑定

    /// 创建关系，返回 6 位邀请码
    static func createCouple(startedDate: Date) async throws -> String {
        let code: String = try await client.rpc(
            "create_couple",
            params: ["p_started_date": Self.dateOnlyFormatter.string(from: startedDate)]
        )
        .execute()
        .value
        return code
    }

    /// 用邀请码加入，返回 couple_id
    static func joinCouple(code: String) async throws -> UUID {
        let coupleId: UUID = try await client.rpc("join_couple", params: ["p_code": code])
            .execute()
            .value
        return coupleId
    }

    // MARK: - 数据查询

    /// 查我的关系（还没绑定返回 nil）
    static func fetchMyCouple(userId: UUID) async throws -> Couple? {
        let couples: [Couple] = try await client.from("couples")
            .select()
            .or("user_a_id.eq.\(userId.uuidString),user_b_id.eq.\(userId.uuidString)")
            .execute()
            .value
        return couples.first
    }

    /// 我和伴侣的资料
    static func fetchProfiles() async throws -> [Profile] {
        try await client.from("profiles").select().execute().value
    }

    /// 重要日期（按日期升序）
    static func fetchAnniversaries(coupleId: UUID) async throws -> [Anniversary] {
        try await client.from("anniversaries")
            .select()
            .eq("couple_id", value: coupleId.uuidString)
            .order("date")
            .execute()
            .value
    }

    /// 心愿单（不含已软删除）
    static func fetchWishlist(coupleId: UUID) async throws -> [WishlistItem] {
        try await client.from("wishlist_items")
            .select()
            .eq("couple_id", value: coupleId.uuidString)
            .eq("is_deleted", value: false)
            .order("created_at")
            .execute()
            .value
    }

    // MARK: - 重要日期（写）

    /// 新增重要日期 → POST anniversaries，返回新行
    static func createAnniversary(
        coupleId: UUID,
        createdBy: UUID,
        name: String,
        date: Date,
        notes: String?,
        enableNotification: Bool
    ) async throws -> Anniversary {
        var payload: [String: AnyJSON] = [
            "couple_id": .string(coupleId.uuidString),
            "created_by": .string(createdBy.uuidString),
            "name": .string(name),
            "date": .string(Self.dateOnlyFormatter.string(from: date)),
            "enable_notification": .bool(enableNotification),
        ]
        if let notes, !notes.isEmpty {
            payload["notes"] = .string(notes)
        }

        let created: Anniversary = try await client.from("anniversaries")
            .insert(payload)
            .select()
            .single()
            .execute()
            .value
        return created
    }

    /// 修改重要日期 → PATCH anniversaries?id=eq.<id>，返回更新后的行
    static func updateAnniversary(
        id: UUID,
        name: String,
        date: Date,
        notes: String?,
        enableNotification: Bool
    ) async throws -> Anniversary {
        let payload: [String: AnyJSON] = [
            "name": .string(name),
            "date": .string(Self.dateOnlyFormatter.string(from: date)),
            "notes": notes.map { .string($0) } ?? .null,   // 备注清空时显式置 NULL
            "enable_notification": .bool(enableNotification),
        ]

        let updated: Anniversary = try await client.from("anniversaries")
            .update(payload)
            .eq("id", value: id.uuidString)
            .select()
            .single()
            .execute()
            .value
        return updated
    }

    /// 删除重要日期 → DELETE anniversaries?id=eq.<id>（API.md：纪念日走真实删除）
    static func deleteAnniversary(id: UUID) async throws {
        try await client.from("anniversaries")
            .delete(returning: .minimal)
            .eq("id", value: id.uuidString)
            .execute()
    }

    // MARK: - 共同心愿单（写）

    /// 新增心愿 → POST wishlist_items，返回新行
    static func createWishlistItem(
        coupleId: UUID,
        creatorId: UUID,
        title: String,
        description: String?
    ) async throws -> WishlistItem {
        var payload: [String: AnyJSON] = [
            "couple_id": .string(coupleId.uuidString),
            "creator_id": .string(creatorId.uuidString),
            "title": .string(title),
        ]
        if let description, !description.isEmpty {
            payload["description"] = .string(description)
        }

        let created: WishlistItem = try await client.from("wishlist_items")
            .insert(payload)
            .select()
            .single()
            .execute()
            .value
        return created
    }

    /// 标记完成 → PATCH wishlist_items?id=eq.<id>：status + completer_id + completed_at 一起写
    static func completeWishlistItem(id: UUID, completerId: UUID) async throws -> WishlistItem {
        let payload: [String: AnyJSON] = [
            "status": .string("COMPLETED"),
            "completer_id": .string(completerId.uuidString),
            "completed_at": .string(Self.timestampFormatter.string(from: Date())),
        ]

        let updated: WishlistItem = try await client.from("wishlist_items")
            .update(payload)
            .eq("id", value: id.uuidString)
            .select()
            .single()
            .execute()
            .value
        return updated
    }

    /// 软删除心愿 → PATCH wishlist_items?id=eq.<id> {is_deleted:true}（API.md 明文约定）
    static func softDeleteWishlistItem(id: UUID) async throws {
        let payload: [String: AnyJSON] = ["is_deleted": .bool(true)]
        try await client.from("wishlist_items")
            .update(payload, returning: .minimal)
            .eq("id", value: id.uuidString)
            .execute()
    }

    // MARK: - 工具

    /// YYYY-MM-DD（与数据库 date 字段格式一致）
    static let dateOnlyFormatter: DateFormatter = {
        let f = DateFormatter()
        // 固定 POSIX 区域 + 公历，避免设备用佛历/民国历时 yyyy-MM-dd 漂移写错库
        f.locale = Locale(identifier: "en_US_POSIX")
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    /// ISO8601（与数据库 timestamptz 对齐，用于 completed_at）
    static let timestampFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()
}
