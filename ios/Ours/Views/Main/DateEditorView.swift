import SwiftUI

/// 新增 / 编辑重要日期（写 anniversaries：name / date / notes / enable_notification）
struct DateEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var app = AppState.shared

    let anniversary: Anniversary?
    let onSaved: () -> Void

    @State private var name: String
    @State private var date: Date
    @State private var notes: String
    @State private var enableNotification: Bool
    @State private var isWorking = false
    @State private var errorMessage: String?

    /// API.md 约定：name ≤50，notes ≤500
    private let nameLimit = 50
    private let notesLimit = 500

    init(anniversary: Anniversary?, onSaved: @escaping () -> Void) {
        self.anniversary = anniversary
        self.onSaved = onSaved
        _name = State(initialValue: anniversary?.name ?? "")
        _date = State(
            initialValue: anniversary
                .flatMap { APIClient.dateOnlyFormatter.date(from: $0.date) } ?? Date()
        )
        _notes = State(initialValue: anniversary?.notes ?? "")
        _enableNotification = State(initialValue: anniversary?.enableNotification ?? true)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("名称（如：在一起纪念日）", text: $name)
                        .onChange(of: name) { _, newValue in
                            if newValue.count > nameLimit {
                                name = String(newValue.prefix(nameLimit))
                            }
                        }
                } footer: {
                    Text("\(name.count)/\(nameLimit)")
                        .font(.caption2)
                }

                Section {
                    DatePicker("日期", selection: $date, displayedComponents: .date)
                        .tint(Color.oursPrimary)
                }

                Section {
                    TextField("备注（可选）", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                        .onChange(of: notes) { _, newValue in
                            if newValue.count > notesLimit {
                                notes = String(newValue.prefix(notesLimit))
                            }
                        }
                } footer: {
                    Text("\(notes.count)/\(notesLimit)")
                        .font(.caption2)
                }

                Section {
                    Toggle("开启提醒", isOn: $enableNotification)
                        .tint(Color.oursPrimary)
                } footer: {
                    Text("提醒打开后会写入数据库，推送上线后自动生效")
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
            .navigationTitle(anniversary == nil ? "新增重要日期" : "编辑重要日期")
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
                    .disabled(isWorking || trimmedName.isEmpty)
                }
            }
        }
    }

    private var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var trimmedNotes: String? {
        let trimmed = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    private func save() async {
        guard !trimmedName.isEmpty else {
            errorMessage = "请填写名称"
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
            if let anniversary {
                _ = try await APIClient.updateAnniversary(
                    id: anniversary.id,
                    name: trimmedName,
                    date: date,
                    notes: trimmedNotes,
                    enableNotification: enableNotification
                )
            } else {
                let userId = try await APIClient.currentUserId()
                _ = try await APIClient.createAnniversary(
                    coupleId: couple.id,
                    createdBy: userId,
                    name: trimmedName,
                    date: date,
                    notes: trimmedNotes,
                    enableNotification: enableNotification
                )
            }
            onSaved()
            dismiss()
        } catch {
            errorMessage = "保存失败：\(error.localizedDescription)"
        }
    }
}
