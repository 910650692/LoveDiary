import SwiftUI

/// 主页：在一起天数 + 下个纪念日倒计时 + 随机回忆卡
struct HomeView: View {
    @StateObject private var app = AppState.shared
    @State private var daysTogether: Int?
    @State private var nextAnniversary: (name: String, date: Date)?

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // 在一起天数
                if let days = daysTogether {
                    VStack(spacing: 6) {
                        Text("\(days)")
                            .font(.system(size: 64, weight: .bold, design: .rounded))
                            .foregroundStyle(Color.oursPrimary)
                        Text("我们在一起的天数")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                    .background(
                        LinearGradient(
                            colors: [Color.oursSecondary.opacity(0.5), Color.oursBackground],
                            startPoint: .top, endPoint: .bottom
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                }

                // 下个纪念日倒计时
                if let next = nextAnniversary {
                    HStack {
                        Image(systemName: "gift.fill")
                            .foregroundStyle(Color.oursGold)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(next.name) 还有")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                            Text("\(daysUntil(next.date)) 天")
                                .font(.title.bold())
                        }
                        Spacer()
                    }
                    .padding()
                    .background(Color.oursBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                // 随机回忆卡（占位，阶段 4 接照片）
                MemoryCardView()

                Spacer(minLength: 24)
            }
            .padding(16)
        }
        .background(Color.oursBackground.opacity(0.3))
        .navigationTitle("Ours")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            computeDays()
            computeNextAnniversary()
        }
    }

    /// 在一起天数
    private func computeDays() {
        guard let couple = app.couple,
              let start = APIClient.dateOnlyFormatter.date(from: couple.startedDate)
        else { return }
        daysTogether = Calendar.current.dateComponents([.day], from: start, to: Date()).day
    }

    /// 下个纪念日：先看「恋爱周年」；阶段 2 上线后可换成自定义纪念日最近的一个
    private func computeNextAnniversary() {
        guard let couple = app.couple,
              let start = APIClient.dateOnlyFormatter.date(from: couple.startedDate)
        else { return }
        let calendar = Calendar.current
        var next = calendar.date(bySettingHour: 0, minute: 0, second: 0, of: start) ?? start
        let thisYear = calendar.date(byAdding: .year, value: calendar.component(.year, from: Date()) - calendar.component(.year, from: next), to: next) ?? next
        if thisYear < Date() {
            next = calendar.date(byAdding: .year, value: 1, to: thisYear) ?? thisYear
        } else {
            next = thisYear
        }
        nextAnniversary = ("在一起周年纪念日", next)
    }

    private func daysUntil(_ date: Date) -> Int {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let target = calendar.startOfDay(for: date)
        return max(0, calendar.dateComponents([.day], from: today, to: target).day ?? 0)
    }
}
