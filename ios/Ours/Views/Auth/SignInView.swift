import SwiftUI

struct SignInView: View {
    @StateObject private var app = AppState.shared
    @State private var email = ""
    @State private var password = ""
    @State private var showSignUp = false
    @State private var errorMessage: String?
    @State private var isWorking = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 28) {
                VStack(spacing: 6) {
                    Text("Ours")
                        .font(.system(size: 44, weight: .bold))
                        .foregroundStyle(Color.oursPrimary)
                    Text("我们的空间")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 70)

                VStack(spacing: 12) {
                    TextField("邮箱", text: $email)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .textFieldStyle(.roundedBorder)
                        .accessibilityIdentifier("signin.email")
                    SecureField("密码", text: $password)
                        .textFieldStyle(.roundedBorder)
                        .accessibilityIdentifier("signin.password")
                }

                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                Button {
                    Task { await login() }
                } label: {
                    Text(isWorking ? "登录中…" : "登录")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.oursPrimary)
                .disabled(isWorking)
                .accessibilityIdentifier("signin.login")

                Button("还没有账号？注册") {
                    showSignUp = true
                }
                .font(.footnote)
                .accessibilityIdentifier("signin.signup")

                Spacer()
            }
            .padding(.horizontal, 32)
            .sheet(isPresented: $showSignUp) {
                SignUpView()
            }
        }
    }

    private func login() async {
        isWorking = true
        errorMessage = nil
        defer { isWorking = false }

        do {
            try await APIClient.signIn(email: email, password: password)
            await app.refresh()
        } catch {
            errorMessage = "登录失败：\(error.localizedDescription)"
        }
    }
}
