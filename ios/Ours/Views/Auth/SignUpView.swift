import SwiftUI

struct SignUpView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var app = AppState.shared
    @State private var nickname = ""
    @State private var email = ""
    @State private var password = ""
    @State private var errorMessage: String?
    @State private var isWorking = false
    @State private var needConfirmEmail = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                VStack(spacing: 6) {
                    Text("加入 Ours")
                        .font(.title2.bold())
                    Text("给自己起个昵称吧")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 40)

                VStack(spacing: 12) {
                    TextField("昵称", text: $nickname)
                        .textFieldStyle(.roundedBorder)
                        .accessibilityIdentifier("signup.nickname")
                    TextField("邮箱", text: $email)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .textFieldStyle(.roundedBorder)
                        .accessibilityIdentifier("signup.email")
                    SecureField("密码", text: $password)
                        .textFieldStyle(.roundedBorder)
                        .accessibilityIdentifier("signup.password")
                }

                if needConfirmEmail {
                    Text("注册成功！请去邮箱点确认链接，再回来登录 ☺️")
                        .font(.footnote)
                        .foregroundStyle(Color.oursPrimary)
                }
                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                Button {
                    Task { await signUp() }
                } label: {
                    Text(isWorking ? "注册中…" : "注册")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.oursPrimary)
                .disabled(isWorking)
                .accessibilityIdentifier("signup.submit")

                Spacer()
            }
            .padding(.horizontal, 32)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") { dismiss() }
                }
            }
        }
    }

    private func signUp() async {
        guard !nickname.isEmpty else {
            errorMessage = "给自己起个昵称吧"
            return
        }
        isWorking = true
        errorMessage = nil
        needConfirmEmail = false
        defer { isWorking = false }

        do {
            try await APIClient.signUp(email: email, password: password, nickname: nickname)
            // signUp 本身不更新 AppState，必须刷新一次才知道有没有拿到 session：
            // 邮箱确认关闭 → 直接有 session → 进绑定页；确认开启 → 无 session → 提示去邮箱确认
            await app.refresh()
            if app.isLoggedIn {
                dismiss()
            } else {
                needConfirmEmail = true
            }
        } catch {
            errorMessage = Self.friendlyMessage(for: error)
        }
    }

    /// 把 Supabase 的英文报错翻成能看懂的中文。
    /// 用文案匹配而不是错误码：SDK 的 AuthError 类型在不同版本里结构会变，文案更稳定。
    private static func friendlyMessage(for error: Error) -> String {
        let raw = error.localizedDescription
        if raw.localizedCaseInsensitiveContains("already registered") {
            return "这个邮箱已经注册过了，直接登录就好"
        }
        if raw.localizedCaseInsensitiveContains("password should be at least") {
            return "密码太短了，至少 6 位"
        }
        if raw.localizedCaseInsensitiveContains("unable to validate email")
            || raw.localizedCaseInsensitiveContains("invalid email") {
            return "邮箱格式好像不对，检查一下"
        }
        if raw.localizedCaseInsensitiveContains("rate limit") {
            return "操作太频繁了，等一会儿再试"
        }
        if raw.localizedCaseInsensitiveContains("network")
            || raw.localizedCaseInsensitiveContains("internet")
            || raw.localizedCaseInsensitiveContains("timed out") {
            return "网络不太顺，检查一下连接再试"
        }
        return "注册失败：\(raw)"
    }
}
