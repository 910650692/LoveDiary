import SwiftUI

/// 输入邀请码加入
struct JoinCoupleView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var app = AppState.shared
    @State private var code = ""
    @State private var errorMessage: String?
    @State private var isWorking = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                VStack(spacing: 6) {
                    Text("输入邀请码")
                        .font(.title3.bold())
                    Text("问 Ta 要 6 位邀请码，就能进入我们的空间")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 40)

                TextField("例如：M9WN8F", text: $code)
                    .font(.system(size: 24, weight: .semibold, design: .rounded))
                    .multilineTextAlignment(.center)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .padding()
                    .background(Color.oursBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 32)
                    .disabled(isWorking)

                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                Spacer()
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") { dismiss() }
                }
            }
            .overlay(alignment: .bottom) {
                Button {
                    Task { await join() }
                } label: {
                    Text(isWorking ? "加入中…" : "加入")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.oursPrimary)
                .disabled(isWorking || code.count < 6)
                .padding(.horizontal, 32)
                .padding(.bottom, 24)
            }
        }
    }

    private func join() async {
        isWorking = true
        errorMessage = nil
        defer { isWorking = false }

        do {
            _ = try await APIClient.joinCouple(code: code.uppercased())
            await app.refresh()
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
