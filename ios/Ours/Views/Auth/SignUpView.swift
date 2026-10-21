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
            // 若项目开启了邮箱确认，signUp 后不会自动登录
            if app.isLoggedIn {
                dismiss()
            } else {
                needConfirmEmail = true
            }
        } catch {
            errorMessage = "注册失败：\(error.localizedDescription)"
        }
    }
}
