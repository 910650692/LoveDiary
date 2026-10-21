import SwiftUI

/// 重要日期：纪念日/生日列表（按日期升序）+ 倒计时 + 新增/编辑/删除
struct DatesView: View {
    @StateObject private var app = AppState.shared

    @State private var anniversaries: [Anniversary] = []
    @State private var isLoading = false
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var route: EditorRoute?
    @State private var pendingDelete: Anniversary?

    /// 同一个 sheet 承载「新增」和「编辑」，避免两个 .sheet 互相覆盖
    private enum EditorRoute: Identifiable {
        case create
        case edit(Anniversary)

        var id: String {
            switch self {
            case .create: return "create"
            case .edit(let item): return item.id.uuidString
            }
        }

        var anniversary: Anniversary? {
            if case .edit(let item) = self { return item }
            return nil
        }
    }

    var body: some View {
        NavigationStack {
            content
                .background(Color.oursBackground.opacity(0.3))
                .navigationTitle("重要日期")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            route = .create
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
                .sheet(item: $route) { route in
                    DateEditorView(anniversary: route.anniversary) {
                        Task { await load() }
                    }
                }
                .confirmationDialog(
                    "删除这条重要日期？",
                    isPresented: Binding(
                        get: { pendingDelete != nil },
                        set: { if !$0 { pendingDelete = nil } }
                    ),
                    titleVisibility: .visible
                ) {
                    Button("删除", role: .destructive) {
                        guard let item = pendingDelete else { return }
                        pendingDelete = nil
                        Task { await delete(item) }
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
                subtitle: "配对成功后，才能一起记录重要日期"
            )
        } else if isLoading && anniversaries.isEmpty {
            ProgressView()
                .tint(Color.oursPrimary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if anniversaries.isEmpty {
            emptyState(
                icon: "calendar.badge.plus",
                title: "还没有重要日期",
                subtitle: "点右上角 + 记下纪念日、生日，一起数着日子过"
            )
        } else {
            list
        }
    }

    private var list: some View {
        List {
            ForEach(anniversaries) { item in
                Button {
                    route = .edit(item)
                } label: {
                    row(item)
                }
                .buttonStyle(.plain)
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
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    private func row(_ item: Anniversary) -> some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.headline)
                    .foregroundStyle(.primary)
                HStack(spacing: 6) {
                    Text(Self.displayDate(item.date))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    if item.enableNotification {
                        Image(systemName: "bell.badge.fill")
                            .font(.caption2)
                            .foregroundStyle(Color.oursGold)
                    }
                }
                if let notes = item.notes, !notes.isEmpty {
                    Text(notes)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
            }
            Spacer(minLength: 8)
            Text(Self.countdownText(item.date))
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color.oursPrimary)
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
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

    // MARK: - 数据

    private func load() async {
        guard let couple = app.couple else {
            anniversaries = []
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            anniversaries = try await APIClient.fetchAnniversaries(coupleId: couple.id)
            errorMessage = nil
        } catch {
            errorMessage = "加载失败：\(error.localizedDescription)"
        }
    }

    private func delete(_ item: Anniversary) async {
        isSubmitting = true
        defer { isSubmitting = false }

        do {
            try await APIClient.deleteAnniversary(id: item.id)
            withAnimation { anniversaries.removeAll { $0.id == item.id } }
        } catch {
            errorMessage = "删除失败：\(error.localizedDescription)"
        }
    }

    // MARK: - 日期工具

    private static func displayDate(_ dateString: String) -> String {
        guard let date = APIClient.dateOnlyFormatter.date(from: dateString) else { return dateString }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy年M月d日"
        return formatter.string(from: date)
    }

    private static func countdownText(_ dateString: String) -> String {
        guard let days = daysUntilNextOccurrence(dateString) else { return "" }
        return days == 0 ? "就是今天 🎉" : "还有 \(days) 天"
    }

    /// 纪念日每年复现：返回距离最近一次（今天或未来）还有多少天，0 表示今天
    private static func daysUntilNextOccurrence(_ dateString: String) -> Int? {
        guard let date = APIClient.dateOnlyFormatter.date(from: dateString) else { return nil }
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let parts = calendar.dateComponents([.month, .day], from: date)
        let baseYear = calendar.component(.year, from: today)

        for yearOffset in 0...1 {
            let components = DateComponents(year: baseYear + yearOffset, month: parts.month, day: parts.day)
            guard let candidate = calendar.date(from: components) else { continue }
            if candidate >= today {
                return calendar.dateComponents([.day], from: today, to: candidate).day
            }
        }
        return nil
    }
}
