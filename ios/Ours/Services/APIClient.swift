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

    // MARK: - 照片（阶段 4：oss-sign + 插行）

    /// 一批照片换 OSS 预签名 PUT URL。
    ///
    /// **一批一次请求**：服务端一次最多签 20 张，逐张请一次会把跨境 RTT 乘以张数。
    /// - Parameter variants: 需要签的对象（标准图 "standard"、缩略图 "thumb"）
    static func signPhotoUploads(
        photoIds: [UUID],
        variants: [String] = ["standard", "thumb"]
    ) async throws -> PhotoSignBatch {
        guard !photoIds.isEmpty else { throw APIClientError.emptyBatch }
        guard let url = URL(string: AppConfig.supabaseURL + "/functions/v1/oss-sign") else {
            throw APIClientError.badRequest
        }

        // Edge Function 默认 verify_jwt：必须带用户 access_token；apikey 与 REST 一样照带
        let session = try await client.auth.session
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(session.accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue(AppConfig.supabaseAnonKey, forHTTPHeaderField: "apikey")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(
            SignRequest(items: photoIds.map { SignRequest.Item(photoId: $0.uuidString, variants: variants) })
        )

        let (data, response) = try await URLSession.shared.data(for: request)
        try Self.throwIfEdgeFailed(response, data: data)
        return try JSONDecoder().decode(PhotoSignBatch.self, from: data)
    }

    /// 插一行 photos。
    ///
    /// `id` 用客户端生成的 photoId：服务端拼对象键时用的就是它（`.../<photoId>.jpg`），
    /// 两边必须是同一个 uuid，否则库里的行找不到自己的对象。
    static func insertPhoto(
        id: UUID,
        coupleId: UUID,
        creatorId: UUID,
        objectKey: String,
        thumbKey: String?,
        originalName: String,
        fileSize: Int,
        mimeType: String = "image/jpeg",
        width: Int?,
        height: Int?,
        takenAt: Date?
    ) async throws -> Photo {
        // 字段名显式 snake_case：supabase-swift 的 PostgREST 不做驼峰转换
        var payload: [String: AnyJSON] = [
            "id": .string(id.uuidString),
            "couple_id": .string(coupleId.uuidString),
            "creator_id": .string(creatorId.uuidString),
            "object_key": .string(objectKey),
            "original_name": .string(originalName),
            "file_size": .integer(fileSize),
            "mime_type": .string(mimeType),
            // 没有 EXIF 拍摄时间就显式写 NULL（时间线排序会回退到 created_at）
            "taken_at": takenAt.map { .string(Self.timestampFormatter.string(from: $0)) } ?? .null,
        ]
        if let thumbKey { payload["thumb_key"] = .string(thumbKey) }
        if let width { payload["width"] = .integer(width) }
        if let height { payload["height"] = .integer(height) }

        let created: Photo = try await client.from("photos")
            .insert(payload)
            .select()
            .single()
            .execute()
            .value
        return created
    }

    /// 最近的几张（上传页底部先只用它列文件名；时间线 keyset 分页是下一单）
    static func fetchRecentPhotos(coupleId: UUID, limit: Int = 6) async throws -> [Photo] {
        try await client.from("photos")
            .select()
            .eq("couple_id", value: coupleId.uuidString)
            .eq("is_deleted", value: false)
            .order("created_at", ascending: false)
            .limit(limit)
            .execute()
            .value
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

    // MARK: - Edge Function 请求体 / 错误

    private struct SignRequest: Encodable {
        let items: [Item]

        struct Item: Encodable {
            let photoId: String
            let variants: [String]
        }
    }

    private struct EdgeErrorBody: Decodable {
        let error: String
    }

    /// Edge Function 的错误体是 { "error": "中文提示" }，可以直接展示给用户
    private static func throwIfEdgeFailed(_ response: URLResponse, data: Data) throws {
        guard let http = response as? HTTPURLResponse else { throw APIClientError.badRequest }
        guard (200..<300).contains(http.statusCode) else {
            let message = (try? JSONDecoder().decode(EdgeErrorBody.self, from: data))?.error
            throw APIClientError.edge(
                status: http.statusCode,
                message: message ?? "请求失败（HTTP \(http.statusCode)）"
            )
        }
    }
}

/// 照片上传链路的客户端错误（Edge Function 的中文 error 直接透传）
enum APIClientError: LocalizedError {
    case emptyBatch
    case badRequest
    case edge(status: Int, message: String)

    var errorDescription: String? {
        switch self {
        case .emptyBatch: return "没有照片需要签名"
        case .badRequest: return "请求无法构造（配置或登录态异常）"
        case .edge(_, let message): return message
        }
    }
}
