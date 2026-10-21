import SwiftUI

/// 新增心愿（写 wishlist_items：title / description / couple_id / creator_id）
struct WishEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var app = AppState.shared

    let onSaved: () -> Void

    @State private var title = ""
    @State private var details = ""
    @State private var isWorking = false
    @State private var errorMessage: String?

    /// API.md 约定：title ≤200，description ≤1000
    private let titleLimit = 200
    private let detailsLimit = 1000

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("想一起做的事（如：去海边看日出）", text: $title)
                        .onChange(of: title) { _, newValue in
                            if newValue.count > titleLimit {
                                title = String(newValue.prefix(titleLimit))
                            }
                        }
                } footer: {
                    Text("\(title.count)/\(titleLimit)")
                        .font(.caption2)
                }

                Section {
                    TextField("描述（可选）", text: $details, axis: .vertical)
                        .lineLimit(3...8)
                        .onChange(of: details) { _, newValue in
                            if newValue.count > detailsLimit {
                                details = String(newValue.prefix(detailsLimit))
                            }
                        }
                } footer: {
                    Text("\(details.count)/\(detailsLimit)")
                        .font(.caption2)
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("新增心愿")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") { dismiss() }
                        .disabled(isWorking)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(isWorking ? "保存中…" : "保存") {
                        Task { await save() }
                    }
                    .disabled(isWorking || trimmedTitle.isEmpty)
                }
            }
        }
    }

    private var trimmedTitle: String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var trimmedDetails: String? {
        let trimmed = details.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    private func save() async {
        guard !trimmedTitle.isEmpty else {
            errorMessage = "请填写心愿"
            return
        }
        guard let couple = app.couple else {
            errorMessage = "还没有绑定另一半，无法保存"
            return
        }

        isWorking = true
        errorMessage = nil
        defer { isWorking = false }

        do {
            let userId = try await APIClient.currentUserId()
            _ = try await APIClient.createWishlistItem(
                coupleId: couple.id,
                creatorId: userId,
                title: trimmedTitle,
                description: trimmedDetails
            )
            onSaved()
            dismiss()
        } catch {
            errorMessage = "保存失败：\(error.localizedDescription)"
        }
    }
}
