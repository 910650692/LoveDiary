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
                    let remaining = daysUntil(next.date)
                    HStack {
                        Image(systemName: "gift.fill")
                            .foregroundStyle(Color.oursGold)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(next.name)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                            if remaining == 0 {
                                Text("就是今天 🎉")
                                    .font(.title.bold())
                            } else {
                                Text("还有 \(remaining) 天")
                                    .font(.title.bold())
                            }
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
            await loadNextAnniversary()
        }
    }

    // MARK: - 在一起天数

    /// 在一起天数
    private func computeDays() {
        guard let couple = app.couple,
              let start = APIClient.dateOnlyFormatter.date(from: couple.startedDate)
        else { return }
        daysTogether = Calendar.current.dateComponents([.day], from: start, to: Date()).day
    }

    // MARK: - 下个纪念日

    /// 下个纪念日：优先取 anniversaries 表里最近的一条；表为空或请求失败则回退「恋爱周年」
    private func loadNextAnniversary() async {
        // 先落地「恋爱周年」：加载中不白屏、网络失败仍有内容，也不会闪出异常数字
        nextAnniversary = loveAnniversary()

        guard let couple = app.couple else { return }
        do {
            let items = try await APIClient.fetchAnniversaries(coupleId: couple.id)
            if let nearest = nearestUpcoming(in: items) {
                nextAnniversary = nearest
            }
            // items 为空：保留上面的「恋爱周年」回退
        } catch {
            // 网络失败：保留上面的「恋爱周年」回退
        }
    }

    /// 多条纪念日里取「下一次发生」距今最近的一条（含今天，即 0 天）
    private func nearestUpcoming(in items: [Anniversary]) -> (name: String, date: Date)? {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        var best: (name: String, date: Date)?
        var bestDays = Int.max

        for item in items {
            // date 是 date-only 字符串（YYYY-MM-DD），用与后端约定一致的格式化器解析
            guard let parsed = APIClient.dateOnlyFormatter.date(from: item.date),
                  let next = nextOccurrence(of: parsed, calendar: calendar, today: today)
            else { continue }

            // 统一用 0 点比较，今天正好是那天即 0 天，不会算出负数
            let days = calendar.dateComponents([.day], from: today, to: next).day ?? Int.max
            if days < bestDays {
                bestDays = days
                best = (item.name, next)
            }
        }
        return best
    }

    /// 按年复现：今年的那天若已过，就取明年那天；今天正好是那天则返回今天
    private func nextOccurrence(of date: Date, calendar: Calendar, today: Date) -> Date? {
        let comps = calendar.dateComponents([.month, .day], from: date)
        guard let month = comps.month, let day = comps.day else { return nil }

        let thisYear = calendar.component(.year, from: today)
        for year in [thisYear, thisYear + 1] {
            if let candidate = occurrence(year: year, month: month, day: day, calendar: calendar),
               candidate >= today {
                return candidate
            }
        }
        return nil
    }

    /// 某年的那一天（当天 0 点）。
    /// 闰日：平年没有 2/29，夹到当月最后一天即 2/28（不滚到 3/1，免得和下月的纪念日撞日）
    private func occurrence(year: Int, month: Int, day: Int, calendar: Calendar) -> Date? {
        var comps = DateComponents()
        comps.year = year
        comps.month = month
        comps.day = 1
        guard let firstOfMonth = calendar.date(from: comps),
              let daysInMonth = calendar.range(of: .day, in: .month, for: firstOfMonth)?.count
        else { return nil }

        comps.day = min(day, daysInMonth)
        return calendar.date(from: comps).map { calendar.startOfDay(for: $0) }
    }

    /// 回退用的「恋爱周年」：从 couple.startedDate 算下一个周年
    private func loveAnniversary() -> (name: String, date: Date)? {
        guard let couple = app.couple,
              let start = APIClient.dateOnlyFormatter.date(from: couple.startedDate)
        else { return nil }

        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        var next = calendar.date(bySettingHour: 0, minute: 0, second: 0, of: start) ?? start
        let thisYear = calendar.date(byAdding: .year, value: calendar.component(.year, from: today) - calendar.component(.year, from: next), to: next) ?? next
        // 和上面统一用 0 点比较：周年当天也显示 0 天（就是今天）
        if thisYear < today {
            next = calendar.date(byAdding: .year, value: 1, to: thisYear) ?? thisYear
        } else {
            next = thisYear
        }
        return ("在一起周年纪念日", next)
    }

    private func daysUntil(_ date: Date) -> Int {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let target = calendar.startOfDay(for: date)
        return max(0, calendar.dateComponents([.day], from: today, to: target).day ?? 0)
    }
}
