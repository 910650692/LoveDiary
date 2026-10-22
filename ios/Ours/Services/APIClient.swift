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

    // MARK: - 关系（写）

    /// 修改在一起日期 → PATCH couples?id=eq.<id>，返回更新后的行。
    /// 服务端有触发器冻结 id / user_a_id / user_b_id / created_at，因此这里只能改 started_date
    static func updateCoupleStartedDate(coupleId: UUID, date: Date) async throws -> Couple {
        let payload: [String: AnyJSON] = [
            "started_date": .string(Self.dateOnlyFormatter.string(from: date)),
        ]
        let updated: Couple = try await client.from("couples")
            .update(payload)
            .eq("id", value: coupleId.uuidString)
            .select()
            .single()
            .execute()
            .value
        return updated
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
        return try await postSign(SignRequest(items: photoIds.map {
            SignRequest.Item(photoId: $0.uuidString, variants: variants, keys: nil)
        }))
    }

    /// 读签名：把 photos 行里的**真实对象键**换成预签名 GET URL（时间线缩略图、详情标准图、原图都走它）。
    ///
    /// 服务端一次最多 20 张，这里按 20 分片**依次**请求：时间线一页 30 张 = 2 次 HTTP。
    /// 分片失败整个方法抛错（调用方负责「图上没出来」的兜底），不做半成功。
    static func signPhotoReads(
        requests: [PhotoReadRequest],
        variants: [String]
    ) async throws -> PhotoReadSignatures {
        guard !requests.isEmpty, !variants.isEmpty else {
            return PhotoReadSignatures(expiresIn: PhotoReadLimits.expiresIn, urls: [:])
        }

        var urls: [UUID: [String: URL]] = [:]
        var expiresIn = PhotoReadLimits.expiresIn
        var index = 0
        while index < requests.count {
            let end = min(index + PhotoReadLimits.signBatchSize, requests.count)
            let batch = try await postSign(SignRequest(items: requests[index..<end].map {
                SignRequest.Item(photoId: $0.photoId.uuidString, variants: variants, keys: $0.keys)
            }))
            expiresIn = batch.expiresIn
            for item in batch.items {
                var perVariant: [String: URL] = [:]
                for variant in variants {
                    if let url = item.getURL(for: variant) { perVariant[variant] = url }
                }
                if !perVariant.isEmpty { urls[item.photoId] = perVariant }
            }
            index = end
        }
        return PhotoReadSignatures(expiresIn: expiresIn, urls: urls)
    }

    // MARK: - 照片删除（顺序不能反：先软删库行，再删 OSS 对象）

    /// 删一张照片：① **先软删库行** → ② 再删 OSS 上的标准图 / 缩略图 / 原图。
    ///
    /// **为什么是这个顺序**：反过来（先删对象后删库行）一旦库行没删成功，
    /// 就留下「有记录但文件没了」的坏图——用户点开只有破图，还会以为 app 坏了。
    /// 按现在的顺序，最坏情况只是留下孤儿文件：只影响存储成本，用户完全无感；
    /// 孤儿文件由运维脚本清理，不属于客户端职责。
    ///
    /// OSS 删除失败**不回滚库行、也不向上抛错**：库行已软删，照片在用户视角已经消失，
    /// 这时弹错只会把人拉回「照片还在」的困惑里。失败只静默记一条日志。
    static func deletePhoto(_ photo: Photo) async throws {
        // ① 软删库行：时间线查询本就带 is_deleted=eq.false，下一次刷新就不会再出现
        try await softDeletePhoto(id: photo.id)
        // ② 删对象：有哪个删哪个，失败只记日志（绝不回滚 ①）
        await deletePhotoObjects(photo)
    }

    /// 软删一行 photos → PATCH photos?id=eq.<id> {is_deleted:true}（API.md 明文约定）
    static func softDeletePhoto(id: UUID) async throws {
        let payload: [String: AnyJSON] = ["is_deleted": .bool(true)]
        try await client.from("photos")
            .update(payload, returning: .minimal)
            .eq("id", value: id.uuidString)
            .execute()
    }

    /// 取 DELETE 签名并逐个删对象。
    ///
    /// 整段是「尽力而为」：签名失败、网络失败、OSS 返回非 204 都只记日志，
    /// 因为库行已经软删，用户视角照片早就没了——这里失败只意味着多一个孤儿文件。
    private static func deletePhotoObjects(_ photo: Photo) async {
        // 只点名库里真实存在的键：缩略图/原图没有就没有，
        // 不能像读取那样回退到标准图（那会把同一个对象删两遍）
        var keys: [String: String] = ["standard": photo.objectKey]
        if let thumbKey = photo.thumbKey { keys["thumb"] = thumbKey }
        if let originalKey = photo.originalKey { keys["original"] = originalKey }

        let signed: PhotoSignBatch
        do {
            signed = try await postSign(SignRequest(items: [
                SignRequest.Item(
                    photoId: photo.id.uuidString,
                    variants: keys.keys.sorted(),
                    keys: keys,
                    delete: true
                )
            ]))
        } catch {
            NSLog("[ours] 删除签名失败，OSS 对象留作孤儿待运维清理 photo=%@ err=%@",
                  photo.id.uuidString, String(describing: error))
            return
        }

        guard let item = signed.items.first else { return }
        for variant in keys.keys {
            guard let url = item.deleteURL(for: variant) else { continue }
            var request = URLRequest(url: url)
            request.httpMethod = "DELETE"
            do {
                let (_, response) = try await URLSession.shared.data(for: request)
                let status = (response as? HTTPURLResponse)?.statusCode ?? -1
                // 204 = 删掉了；404 = 本来就不在（上一轮删过），都算达成目的
                if status != 204 && status != 404 {
                    NSLog("[ours] 删除 OSS 对象返回 %d，留作孤儿待运维清理 key=%@",
                          status, keys[variant] ?? variant)
                }
            } catch {
                NSLog("[ours] 删除 OSS 对象失败，留作孤儿待运维清理 key=%@ err=%@",
                      keys[variant] ?? variant, String(describing: error))
            }
        }
    }

    /// 打一次 oss-sign（上传与读取共用：同一个端点、同一套鉴权）
    private static func postSign(_ body: SignRequest) async throws -> PhotoSignBatch {
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
        request.httpBody = try JSONEncoder().encode(body)

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

    /// 一批照片传完，告诉服务端「这批成功了 N 张」→ notify-photos 推给另一半。
    ///
    /// **一批一次**：count 是本批成功张数，不是逐张调用（逐张推 = 选 10 张就轰炸对方 10 条）。
    /// 服务端还会按 couple 再挡一层 10 分钟节流，客户端不必自己算时间。
    /// 调用方要吞掉错误：照片已经进库了，回响丢了不能反过来把上传结果变成失败。
    static func notifyPhotos(count: Int) async throws {
        guard let url = URL(string: AppConfig.supabaseURL + "/functions/v1/notify-photos") else {
            throw APIClientError.badRequest
        }

        // 与 oss-sign 一样：Edge Function 默认 verify_jwt，必须带用户 access_token + apikey
        let session = try await client.auth.session
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(session.accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue(AppConfig.supabaseAnonKey, forHTTPHeaderField: "apikey")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(["count": count])

        let (data, response) = try await URLSession.shared.data(for: request)
        try Self.throwIfEdgeFailed(response, data: data)
    }

    /// 最近的几张（上传完立刻回显用）
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

    // MARK: - 往昔重现（On This Day）

    /// 「往年的今天」：拍摄时间落在往年同月同日 ±windowDays 内的照片，**优先取最近的那一年**。
    ///
    /// 为什么给每个往年拼一个 `taken_at` 区间、而不是拉回来再筛：PostgREST 的 where 里取不到
    /// `month()` / `day()`，只能把「今年以前的全部照片」搬回客户端再比月日——那是随库线性变慢的做法。
    /// 这里按年拼真实日期区间（±windowDays 会跨月甚至跨年，交给 Calendar 算），一次 OR 上去。
    ///
    /// 三条硬规则：
    /// - `taken_at` 为 null **不参与**：没有拍摄时间就无从判断「往年的今天」，
    ///   绝不用 `created_at` 顶替（入库时间只说明哪天传的，不代表哪天拍的）
    /// - 今年拍的不参与：年份必须早于今年，否则这张卡就不是「回忆」了
    /// - `is_deleted` 的不参与（软删的照片在用户视角已经不存在）
    ///
    /// - Returns: 命中里 `taken_at` 最晚的那张——时间戳最大 ⇒ 年份也最大，
    ///   所以「优先最近年份」由倒序自动成立，不需要再排一次序
    static func fetchOnThisDayPhotos(
        coupleId: UUID,
        windowDays: Int = PhotoReadLimits.onThisDayWindowDays
    ) async throws -> Photo? {
        let ranges = onThisDayRanges(windowDays: windowDays)
        guard !ranges.isEmpty else { return nil }

        let photos: [Photo] = try await client.from("photos")
            .select()
            .eq("couple_id", value: coupleId.uuidString)
            .eq("is_deleted", value: false)
            // taken_at.gte 天然挡掉 null（NULL 参与比较的结果是 unknown，不会命中）
            .or(ranges.joined(separator: ","))
            .order("taken_at", ascending: false)
            .limit(PhotoReadLimits.onThisDayLimit)
            .execute()
            .value

        return photos.first
    }

    /// 往年每一年的窗口：该年「今月今日」所在的那一天 ±windowDays，两端取当天 0 点
    private static func onThisDayRanges(
        windowDays: Int,
        calendar: Calendar = .current,
        now: Date = Date()
    ) -> [String] {
        let today = calendar.startOfDay(for: now)
        let month = calendar.component(.month, from: today)
        let day = calendar.component(.day, from: today)
        let thisYear = calendar.component(.year, from: today)

        var ranges: [String] = []
        for year in (thisYear - PhotoReadLimits.onThisDayYearSpan)...(thisYear - 1) {
            guard let base = occurrence(year: year, month: month, day: day, calendar: calendar),
                  let start = calendar.date(byAdding: .day, value: -windowDays, to: base),
                  let end = calendar.date(byAdding: .day, value: windowDays + 1, to: base)
            else { continue }
            // 右端写成「窗口后一天 0 点 + lt」：不用给 lte 手拼 23:59:59.999999 这种边界
            ranges.append("and(taken_at.gte.\(cursorTimestamp(start)),taken_at.lt.\(cursorTimestamp(end)))")
        }
        return ranges
    }

    /// 某年的「今月今日」当天 0 点。闰日 2/29 在平年夹到 2/28（与主页倒计时的处理一致）
    private static func occurrence(year: Int, month: Int, day: Int, calendar: Calendar) -> Date? {
        var comps = DateComponents()
        comps.year = year
        comps.month = month
        comps.day = 1
        guard let firstOfMonth = calendar.date(from: comps),
              let daysInMonth = calendar.range(of: .day, in: .month, for: firstOfMonth)?.count
        else { return nil }

        comps.day = min(day, daysInMonth)
        return calendar.date(from: comps).map { calendar.startOfDay(for: $0) }
    }

    // MARK: - 照片时间线（keyset 分页）

    /// 时间线一页：`taken_at desc nulls last, created_at desc, id desc`
    ///
    /// 为什么不用 offset：翻页期间新传一张就会让后面每一页错位（重复/漏项）。
    /// keyset 用「上一页最后一行的排序键」当锚点，锚点不变，结果就不变。
    /// - Parameter cursor: nil = 第一页
    static func fetchPhotoPage(
        coupleId: UUID,
        cursor: PhotoCursor? = nil,
        limit: Int = PhotoReadLimits.pageSize
    ) async throws -> [Photo] {
        var query = client.from("photos")
            .select()
            .eq("couple_id", value: coupleId.uuidString)
            .eq("is_deleted", value: false)

        switch cursor {
        case nil:
            break
        case .takenAfter(let takenAt, let createdAt, let id):
            // 三段合并：拍摄时间更早 / 同一拍摄时间但入库更早 / 两者都相同再用 id 兜底
            let t = Self.cursorTimestamp(takenAt)
            let c = Self.cursorTimestamp(createdAt)
            query = query.or(
                "taken_at.lt.\(t),and(taken_at.eq.\(t),created_at.lt.\(c)),and(taken_at.eq.\(t),created_at.eq.\(c),id.lt.\(id.uuidString))"
            )
        case .nullHead:
            query = query.is("taken_at", value: nil)
        case .nullAfter(let createdAt, let id):
            let c = Self.cursorTimestamp(createdAt)
            query = query.is("taken_at", value: nil)
                .or("created_at.lt.\(c),and(created_at.eq.\(c),id.lt.\(id.uuidString))")
        }

        return try await query
            .order("taken_at", ascending: false, nullsFirst: false)
            .order("created_at", ascending: false)
            .order("id", ascending: false)
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

    /// 分页游标的时间戳：微秒精度，且只用 `Z` 结尾
    ///
    /// 两个坑都得绕开：
    /// - 精度：Postgres timestamptz 与 supabase-swift 的解码都是微秒；截断到秒/毫秒会让
    ///   同一窗口内入库的行在 keyset 比较里失配（漏项）
    /// - **绝不能输出 `+00:00`**：URL query 里的 `+` 会被服务端解成空格，PostgREST 直接 400
    static func cursorTimestamp(_ date: Date) -> String {
        let micros = (date.timeIntervalSince1970 * 1_000_000).rounded()
        // 1970 年之前（翻拍的老照片）不拼微秒，秒级足够
        guard micros >= 0 else { return Self.timestampFormatter.string(from: date) }
        let total = Int64(micros)
        let base = Self.timestampFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(total / 1_000_000)))
        guard base.hasSuffix("Z") else { return base }
        return String(base.dropLast()) + String(format: ".%06lldZ", total % 1_000_000)
    }

    // MARK: - Edge Function 请求体 / 错误

    private struct SignRequest: Encodable {
        let items: [Item]

        struct Item: Encodable {
            let photoId: String
            let variants: [String]
            /// 读已存在的对象时给库里的真实键；上传/新照片传 nil（服务端按当天拼）
            let keys: [String: String]?
            /// true = 额外要 deleteUrls（删除签名）；nil 时编码器会直接省略这个字段，老契约不变
            var delete: Bool? = nil
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

/// 时间线分页游标。
///
/// `taken_at desc nulls last` 把照片分成两段，两段排序键不同，所以游标也分两种：
/// - 前段：有拍摄时间，键是 `(taken_at, created_at, id)`
/// - 后段：`taken_at is null`，键是 `(created_at, id)`
/// 只用 `created_at` 不够：连拍照片同一个 `taken_at`，光比入库时间会漏掉同毫秒的行。
enum PhotoCursor: Hashable {
    /// 「有拍摄时间」那一段某行之后
    case takenAfter(takenAt: Date, createdAt: Date, id: UUID)
    /// 前段已取完，进入 `taken_at` 为 null 的那一段（等于从这段头部开始，没有上界）
    case nullHead
    /// null 段某行之后
    case nullAfter(createdAt: Date, id: UUID)
}

/// 读链路的硬约束（与 supabase/API.md 对齐）
enum PhotoReadLimits {
    /// 时间线每页张数
    static let pageSize = 30
    /// `oss-sign` 一次最多签 20 张
    static let signBatchSize = 20
    /// 签名 URL 有效期兜底值（服务端响应里的 `expiresIn` 优先）
    static let expiresIn = 900
    /// 「往年的今天」窗口：今月今日 ±3 天
    static let onThisDayWindowDays = 3
    /// 「往年的今天」往回找多少年（再老的照片基本不会出现在两个人的手机里）
    static let onThisDayYearSpan = 30
    /// 「往年的今天」最多取多少行：只要最近年份里最新的一张，50 行足够
    static let onThisDayLimit = 50
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
