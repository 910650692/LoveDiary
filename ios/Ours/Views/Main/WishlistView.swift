import SwiftUI

/// 共同心愿单：分「想做的」「已完成」两段，新增、标记完成、软删除
struct WishlistView: View {
    @StateObject private var app = AppState.shared

    @State private var items: [WishlistItem] = []
    @State private var profiles: [Profile] = []
    @State private var isLoading = false
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var isCreating = false
    @State private var pendingDelete: WishlistItem?

    private var pendingItems: [WishlistItem] {
        items.filter { !$0.isCompleted }
    }

    private var completedItems: [WishlistItem] {
        items.filter(\.isCompleted)
    }

    private var nicknameById: [UUID: String] {
        var map: [UUID: String] = [:]
        for profile in profiles {
            map[profile.id] = profile.nickname
        }
        return map
    }

    var body: some View {
        NavigationStack {
            content
                .background(Color.oursBackground.opacity(0.3))
                .navigationTitle("心愿单")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            isCreating = true
                        } label: {
                            Image(systemName: "plus")
                        }
                        .disabled(app.couple == nil)
                    }
                }
                .overlay {
                    if isSubmitting {
                        ProgressView()
                            .tint(Color.oursPrimary)
                            .padding(20)
                            .background(.ultraThinMaterial)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }
                .task(id: app.couple?.id) { await load() }
                .refreshable { await load() }
                .sheet(isPresented: $isCreating) {
                    WishEditorView {
                        Task { await load() }
                    }
                }
                .confirmationDialog(
                    "删除这个心愿？",
                    isPresented: Binding(
                        get: { pendingDelete != nil },
                        set: { if !$0 { pendingDelete = nil } }
                    ),
                    titleVisibility: .visible
                ) {
                    Button("删除", role: .destructive) {
                        guard let item = pendingDelete else { return }
                        pendingDelete = nil
                        Task { await softDelete(item) }
                    }
                    Button("取消", role: .cancel) { pendingDelete = nil }
                }
                .alert(
                    "出错了",
                    isPresented: Binding(
                        get: { errorMessage != nil },
                        set: { if !$0 { errorMessage = nil } }
                    )
                ) {
                    Button("好", role: .cancel) { errorMessage = nil }
                } message: {
                    Text(errorMessage ?? "")
                }
        }
    }

    // MARK: - 内容

    @ViewBuilder
    private var content: some View {
        if app.couple == nil {
            emptyState(
                icon: "link",
                title: "还没有绑定另一半",
                subtitle: "配对成功后，才能一起写下想做的事"
            )
        } else if isLoading && items.isEmpty {
            ProgressView()
                .tint(Color.oursPrimary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if items.isEmpty {
            emptyState(
                icon: "gift.fill",
                title: "心愿单还是空的",
                subtitle: "点右上角 + 写下第一件想一起做的事"
            )
        } else {
            list
        }
    }

    private var list: some View {
        List {
            Section {
                if pendingItems.isEmpty {
                    placeholderRow("想做的事都完成啦 🎉")
                } else {
                    ForEach(pendingItems) { item in
                        pendingRow(item)
                    }
                }
            } header: {
                Text("想做的（\(pendingItems.count)）")
            }

            Section {
                if completedItems.isEmpty {
                    placeholderRow("完成心愿后会出现在这里")
                } else {
                    ForEach(completedItems) { item in
                        completedRow(item)
                    }
                }
            } header: {
                Text("已完成（\(completedItems.count)）")
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    private func pendingRow(_ item: WishlistItem) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(item.title)
                .font(.headline)
            if let description = item.description, !description.isEmpty {
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
            }
        }
        .padding(.vertical, 4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .listRowBackground(Color.white.opacity(0.7))
        .listRowSeparator(.hidden)
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                pendingDelete = item
            } label: {
                Label("删除", systemImage: "trash")
            }
            Button {
                Task { await complete(item) }
            } label: {
                Label("完成", systemImage: "checkmark.circle.fill")
            }
            .tint(Color.oursPrimary)
        }
    }

    private func completedRow(_ item: WishlistItem) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.footnote)
                    .foregroundStyle(Color.oursPrimary)
                Text(item.title)
                    .font(.headline)
                    .foregroundStyle(.secondary)
                    .strikethrough()
            }
            if let description = item.description, !description.isEmpty {
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Text(completedFooter(item))
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .listRowBackground(Color.white.opacity(0.7))
        .listRowSeparator(.hidden)
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                pendingDelete = item
            } label: {
                Label("删除", systemImage: "trash")
            }
        }
    }

    private func placeholderRow(_ text: String) -> some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(.secondary)
            .listRowBackground(Color.white.opacity(0.7))
            .listRowSeparator(.hidden)
    }

    private func emptyState(icon: String, title: String, subtitle: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 40))
                .foregroundStyle(Color.oursSecondary)
            Text(title)
                .font(.headline)
            Text(subtitle)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// 「由 谁 完成 · 5月20日」（completer_id + completed_at）
    private func completedFooter(_ item: WishlistItem) -> String {
        var who = "我们"
        if let completerId = item.completerId,
           let nickname = nicknameById[completerId],
           !nickname.isEmpty {
            who = nickname
        }
        guard let completedAt = item.completedAt else {
            return "由 \(who) 完成"
        }
        return "由 \(who) 完成 · \(Self.dayText(completedAt))"
    }

    private static func dayText(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "M月d日"
        return formatter.string(from: date)
    }

    // MARK: - 数据

    private func load() async {
        guard let couple = app.couple else {
            items = []
            profiles = []
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            items = try await APIClient.fetchWishlist(coupleId: couple.id)
            profiles = (try? await APIClient.fetchProfiles()) ?? []
            errorMessage = nil
        } catch {
            errorMessage = "加载失败：\(error.localizedDescription)"
        }
    }

    private func complete(_ item: WishlistItem) async {
        isSubmitting = true
        defer { isSubmitting = false }

        do {
            let userId = try await APIClient.currentUserId()
            let updated = try await APIClient.completeWishlistItem(id: item.id, completerId: userId)
            if let index = items.firstIndex(where: { $0.id == updated.id }) {
                withAnimation { items[index] = updated }
            } else {
                await load()
            }
        } catch {
            errorMessage = "标记完成失败：\(error.localizedDescription)"
        }
    }

    private func softDelete(_ item: WishlistItem) async {
        isSubmitting = true
        defer { isSubmitting = false }

        do {
            try await APIClient.softDeleteWishlistItem(id: item.id)
            withAnimation { items.removeAll { $0.id == item.id } }
        } catch {
            errorMessage = "删除失败：\(error.localizedDescription)"
        }
    }
}
