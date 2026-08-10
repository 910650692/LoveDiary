import SwiftUI

/// 创建关系：选开始日期 → 生成邀请码 → 分享给 Ta
struct CreateCoupleView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var app = AppState.shared
    @State private var startedDate = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()
    @State private var inviteCode: String?
    @State private var errorMessage: String?
    @State private var isWorking = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                VStack(spacing: 6) {
                    Text("从哪天开始，属于彼此？")
                        .font(.title3.bold())
                    Text("这一天会变成主页上「在一起」的起点")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 32)

                DatePicker(
                    "恋爱开始日期",
                    selection: $startedDate,
                    in: ...Date(),
                    displayedComponents: .date
                )
                .datePickerStyle(.graphical)
                .tint(Color.oursPrimary)
                .disabled(isWorking || inviteCode != nil)

                if let inviteCode {
                    // 邀请码展示 + 复制
                    VStack(spacing: 12) {
                        Text("把邀请码给 Ta ✨")
                            .font(.headline)
                        Text(inviteCode)
                            .font(.system(size: 36, weight: .bold, design: .rounded))
                            .tracking(6)
                            .foregroundStyle(Color.oursPrimary)
                        Text("7 天内有效，只能使用一次")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Button("复制邀请码") {
                            UIPasteboard.general.string = inviteCode
                        }
                        .buttonStyle(.bordered)
                        ShareLink(item: "来 Ours 加入我们的空间吧！邀请码：\(inviteCode)") {
                            Label("分享给 Ta", systemImage: "square.and.arrow.up")
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.oursPrimary)
                    }
                    .transition(.scale.combined(with: .opacity))
                } else if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                Spacer()
            }
            .padding(.horizontal, 24)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") { dismiss() }
                }
            }
            .overlay(alignment: .bottom) {
                if inviteCode == nil {
                    Button {
                        Task { await create() }
                    } label: {
                        Text(isWorking ? "生成中…" : "生成邀请码")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.oursPrimary)
                    .disabled(isWorking)
                    .padding(.horizontal, 32)
                    .padding(.bottom, 24)
                }
            }
        }
    }

    private func create() async {
        isWorking = true
        errorMessage = nil
        defer { isWorking = false }

        do {
            let code = try await APIClient.createCouple(startedDate: startedDate)
            withAnimation { inviteCode = code }
            await app.refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
