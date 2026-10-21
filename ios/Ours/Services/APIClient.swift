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

    // MARK: - 工具

    /// YYYY-MM-DD（与数据库 date 字段格式一致）
    static let dateOnlyFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()
}
