import SwiftUI

/// 「我们」：两人的昵称 + 在一起的那天和天数 + 登出入口
struct OursView: View {
    @StateObject private var app = AppState.shared

    @State private var profiles: [Profile] = []
    @State private var myId: UUID?
    @State private var loadFailed = false
    @State private var isSigningOut = false
    @State private var showSignOutConfirm = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    coupleCard
                    togetherCard
                    signOutSection
                    Spacer(minLength: 24)
                }
                .padding(16)
            }
            .background(Color.oursBackground.opacity(0.3))
            .navigationTitle("我们")
            .navigationBarTitleDisplayMode(.inline)
            .task(id: app.couple?.id) { await load() }
            .refreshable { await load() }
            .confirmationDialog("确定要退出登录吗？", isPresented: $showSignOutConfirm, titleVisibility: .visible) {
                Button("退出登录", role: .destructive) {
                    Task { await signOut() }
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("退出后这台手机需要重新登录才能进入我们的空间；回忆不会被删除。")
            }
        }
    }

    // MARK: - 两人昵称

    private var coupleCard: some View {
        VStack(spacing: 16) {
            HStack(alignment: .top, spacing: 12) {
                personSlot(title: "我", nickname: myNickname, tint: Color.oursSecondary)
                Image(systemName: "heart.fill")
                    .font(.title3)
                    .foregroundStyle(Color.oursPrimary)
                    .padding(.top, 22)
                personSlot(title: "另一半", nickname: partnerNickname, tint: Color.oursGold)
            }

            if app.couple == nil {
                Text("还没有和另一半绑定，绑定后这里会显示两个人的名字")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            } else if loadFailed {
                Text("资料没加载出来，下拉可以重试")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .padding(.horizontal, 16)
        .background(Color.oursBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func personSlot(title: String, nickname: String?, tint: Color) -> some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(tint.opacity(0.4))
                    .frame(width: 64, height: 64)
                Image(systemName: "person.fill")
                    .font(.title2)
                    .foregroundStyle(Color.oursPrimary)
            }
            Text(displayName(nickname))
                .font(.headline)
                .foregroundStyle(nickname == nil ? Color.secondary : Color.primary)
                .lineLimit(1)
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    /// 昵称兜底：缺失或空白都显示占位文案，不出现空白或 "nil"
    private func displayName(_ nickname: String?) -> String {
        let trimmed = nickname?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? "未设置昵称" : trimmed
    }

    private var myNickname: String? {
        guard let myId else { return nil }
        return profiles.first { $0.id == myId }?.nickname
    }

    private var partnerId: UUID? {
        guard let couple = app.couple, let myId else { return nil }
        if couple.userAId == myId { return couple.userBId }
        if couple.userBId == myId { return couple.userAId }
        return nil
    }

    private var partnerNickname: String? {
        guard let partnerId else { return nil }
        return profiles.first { $0.id == partnerId }?.nickname
    }

    // MARK: - 在一起那天 / 天数

    private var togetherCard: some View {
        VStack(spacing: 10) {
            if let startedText = startedDateText {
                Text(daysTogether.map(String.init) ?? "—")
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .foregroundStyle(Color.oursPrimary)
                Text("在一起的天数")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                HStack(spacing: 6) {
                    Image(systemName: "calendar")
                        .foregroundStyle(Color.oursGold)
                    Text("从 \(startedText) 开始")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            } else {
                Text("还没有记录在一起的那天")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .padding(.horizontal, 16)
        .background(
            LinearGradient(
                colors: [Color.oursSecondary.opacity(0.5), Color.oursBackground],
                startPoint: .top, endPoint: .bottom
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    /// 在一起天数：与主页同一算法（date-only 字符串解析后取日历天数差）
    private var daysTogether: Int? {
        guard let couple = app.couple,
              let start = APIClient.dateOnlyFormatter.date(from: couple.startedDate)
        else { return nil }
        return Calendar.current.dateComponents([.day], from: start, to: Date()).day
    }

    /// 在一起那天：后端是 YYYY-MM-DD，直接拆年月日，绕开时区和本地化差异
    private var startedDateText: String? {
        guard let couple = app.couple else { return nil }
        let parts = couple.startedDate.split(separator: "-")
        guard parts.count == 3,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              let day = Int(parts[2])
        else { return nil }
        return "\(year)年\(month)月\(day)日"
    }

    // MARK: - 登出

    private var signOutSection: some View {
        VStack(spacing: 10) {
            Button {
                showSignOutConfirm = true
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                    Text(isSigningOut ? "退出中…" : "退出登录")
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.oursPrimary)
            .disabled(isSigningOut)

            Text("退出不会删除任何回忆，下次登录还能看到。")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.top, 8)
    }

    /// 统一走 AppState 的登出：清 session + 解绑推送 + 复位 Tab，
    /// isLoggedIn 变 false 后 RootView 自动切回登录页，这里不另做跳转
    private func signOut() async {
        guard !isSigningOut else { return }
        isSigningOut = true
        await AppState.shared.signOut()
        isSigningOut = false
    }

    // MARK: - 数据

    private func load() async {
        myId = try? await APIClient.currentUserId()

        guard app.couple != nil else {
            profiles = []
            loadFailed = false
            return
        }

        do {
            profiles = try await APIClient.fetchProfiles()
            loadFailed = false
        } catch {
            profiles = []
            loadFailed = true
        }
    }
}
