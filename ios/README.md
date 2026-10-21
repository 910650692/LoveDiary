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

## 跑界面回归测试（UI 测试）

```bash
cd ios
xcodegen generate
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild test \
  -project Ours.xcodeproj -scheme Ours \
  -destination 'platform=iOS Simulator,name=iPhone Duo'
```

当前 3 条用例（`OursUITests/OursUITests.swift`）：

| 用例 | 覆盖 | 是否联网 |
|---|---|---|
| T1 冷启动 | 登录页关键元素（标题/邮箱/密码/登录/注册入口） | 否 |
| T2 注册入口 | 点「注册」进注册页，校验表单元素（**不提交**，不往云端造数据） | 否 |
| T3 四个 Tab | 主页/纪念日/心愿单/我们 存在且能互切 | 否（假 session，不发请求）|

T3 靠 `AppState` 里 **仅在 DEBUG 生效**的启动参数后门进入"已绑定"状态：

- `-UITEST_FAKE_SESSION`：注入本地构造的假 Couple（startedDate 固定 2023-05-20），**不读 session、不发请求**
- `-UITEST_SIGNED_OUT`：强制未登录，避免模拟器残留 session 让登录页用例假失败
- `-push.permissionAsked YES`：走既有「权限只问一次」逻辑，压住系统权限弹窗

这些参数在正常启动（不传参）时整块跳过；Release 产物里不含这些字符串（可用 `strings` 自查）。

> 注意：iOS 27 的 SwiftUI `TabView` 在 iPhone Duo 上渲染成右侧竖排浮动条，`app.tabBars` 为空 —— 测试用 `app.buttons[label]` 匹配，兼容旧版本 TabBar 形态。

### T7 真注册（默认不编译，会写云端数据）

验证「注册成功 → 直接进绑定页」这条真实链路。它会在 Supabase 里**创建一个随机邮箱账号**，所以用编译期开关隔离，默认不参与编译：

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild test \
  -project Ours.xcodeproj -scheme Ours \
  -destination 'platform=iOS Simulator,name=iPhone Duo' \
  OTHER_SWIFT_FLAGS='$(inherited) -D REAL_SIGNUP' \
  -only-testing:OursUITests/OursUITests/testT7_RealSignupLandsOnBind
```

跑完记得去 **Dashboard → Authentication → Users** 删掉 `uitest+…@ourstest.app`。

## 首次使用前要确认的配置

| 项 | 现状 | 说明 |
|---|---|---|
| Bundle ID | 占位 `com.ours.app` | PRD 待定，上架前定；改 `project.yml` 后重新 `xcodegen generate` |
| 邮箱确认 | ⚠️ 需手动关掉 | Supabase 新项目默认 **Confirm email 开启**，而 `Site URL` 默认是 `http://localhost:3000` —— 确认邮件里的链接就指向这个本机空端口，**点开必然打不开**（QQ 邮箱还会包一层 `xmspamcheck` 跳转）。<br>**开发期做法**：Dashboard → Authentication → Providers → Email → **Confirm email OFF**。<br>**注意**：关开关不会确认已存在的用户 —— 已注册的账号要再去 Authentication → Users 手动 **Confirm user** 或删掉重注册，否则登录仍报 `Email not confirmed`。<br>**上架前若要真邮件闭环**：改 `Site URL` + 给 app 加 URL Scheme + 配 Redirect URLs 白名单。 |
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
