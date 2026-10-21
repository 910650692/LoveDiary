# Ours · iOS 工程（SwiftUI）

阶段 1 骨架：登录注册 → 情侣绑定（创建/邀请码）→ 主页（在一起天数/纪念日倒计时/回忆卡占位）。

## 环境要求（Mac Air）

- Xcode 15+（iOS 17 SDK）
- XcodeGen（工程由 `project.yml` 生成，`.xcodeproj` 不入库）

安装 XcodeGen：

```bash
brew install xcodegen
```

> 前置：Xcode 装好后**必须打开一次**，让它装完「附加组件」（CoreSimulator 等）。没打开过的话 `xcodebuild` 会直接崩在 `IDESimulatorFoundation` 插件加载上。

## 跑起来

```bash
cd ios
./scripts/fetch-onesignal.sh   # 拉 OneSignal 预编译框架（约 54MB，只需一次）
xcodegen generate              # 生成 Ours.xcodeproj
open Ours.xcodeproj
```

为什么 OneSignal 不用 SwiftPM：它的源码仓库约 3GB，SwiftPM 全量克隆且不支持断点续传，大陆网络会反复超时。改用官方 release 的分体 XCFramework，二进制放 `Vendor/OneSignal/`（不入库）。

在 Xcode 里：
1. Signing & Capabilities → Team 选你的 Apple ID（个人开发免费签名即可真机跑）
2. 选 iPhone 真机（或模拟器）→ Run

## 首次使用前要确认的配置

| 项 | 现状 | 说明 |
|---|---|---|
| Bundle ID | 占位 `com.ours.app` | PRD 待定，上架前定；改 `project.yml` 后重新 `xcodegen generate` |
| 邮箱确认 | 未处理 | 若 Supabase 后台开了 Email 确认，注册后需点邮件链接；测试期可去后台关掉（Auth → Providers → Email → Confirm email OFF） |
| Push | 已接 OneSignal SDK | 通知要真机生效还需：Xcode 里加 Push Notifications capability + OneSignal 后台配 APNs（需 Apple 开发者账号 $99） |

## 目录结构

```
project.yml                    # XcodeGen 配置（SPM：supabase-swift；二进制：OneSignal XCFramework）
scripts/fetch-onesignal.sh     # 拉取 OneSignal 预编译框架到 Vendor/OneSignal
Vendor/OneSignal/              # 8 个 XCFramework（不入库）
Ours/
├── OursApp.swift              # 入口：OneSignal 初始化
├── AppConfig.swift            # Supabase URL / anon key / OneSignal App ID
├── Theme.swift                # 主题色
├── Models/Models.swift        # Profile / Couple / Anniversary / WishlistItem
├── Services/                  # Supabase client + 接口封装（对应 supabase/API.md）
├── State/AppState.swift       # 登录/绑定状态 + OneSignal external_user_id 绑定
└── Views/                     # 登录/注册 → 绑定 → 主页（Tab：主页/纪念日/心愿单）
```

## 改代码后

`project.yml` 有改动才需要重新 `xcodegen generate`；Swift 文件增删直接改 Xcode 里的文件夹引用即可（或重新 generate）。

## 后台约定

服务端接口全部在 `supabase/API.md`；新增 RPC 函数后同步更新该文档。
