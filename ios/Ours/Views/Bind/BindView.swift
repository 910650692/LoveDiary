import SwiftUI

/// 绑定页：创建关系 or 输入邀请码
struct BindView: View {
    @StateObject private var app = AppState.shared
    @State private var showCreate = false
    @State private var showJoin = false

    var body: some View {
        VStack(spacing: 24) {
            VStack(spacing: 6) {
                Text("欢迎加入 Ours 🎈")
                    .font(.title2.bold())
                Text("和一个 Ta 开始两个人的空间")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .padding(.top, 60)

            VStack(spacing: 16) {
                Button {
                    showCreate = true
                } label: {
                    VStack(spacing: 8) {
                        Image(systemName: "heart.circle.fill")
                            .font(.system(size: 40))
                        Text("创建我们的空间")
                            .font(.headline)
                        Text("设置恋爱开始日期，生成邀请码")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .background(Color.oursBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)

                Button {
                    showJoin = true
                } label: {
                    VStack(spacing: 8) {
                        Image(systemName: "link.circle.fill")
                            .font(.system(size: 40))
                        Text("输入邀请码加入")
                            .font(.headline)
                        Text("TA 已经建好了空间，用邀请码进来")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .background(Color.oursSecondary.opacity(0.3))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
            }

            Spacer()
        }
        .padding(.horizontal, 32)
        .sheet(isPresented: $showCreate) { CreateCoupleView() }
        .sheet(isPresented: $showJoin) { JoinCoupleView() }
    }
}
