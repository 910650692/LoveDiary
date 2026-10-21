# LoveDiary（情侣空间）

给 npy 的 iOS app 礼物项目。**开始任何工作前，先读 `PRD.md`。**

## 一句话背景
「回忆过去 + 期待未来」的两人空间 app。2025 年中启动（Java 后端 + HTML 原型）后搁置一年，2026-08 重启并重新梳理需求。两人都用 iPhone，常驻大陆。

## 技术栈（已定）
- 前端：**SwiftUI 原生**（iOS 17+），Windows 上写码 → Mac Air 构建运行
- 后端：**Supabase**（Postgres + Auth + Edge Function），零自建后端，旧 Java 后端退役
- 照片存储：**阿里云 OSS**（大陆机房）
- 推送：**APNs + OneSignal**（第一等公民功能）
- 工程生成：**XcodeGen**（`project.yml` → `xcodegen` 生成 `.xcodeproj`）
- 代码托管：git + GitHub 私有仓库

## 目录规划
- `PRD.md` — 产品需求文档（权威依据，改需求先改它）
- `CLAUDE.md` — 本文件：当前项目说明（前端/Supabase 方向）
- `backend/` — 旧 Java 后端（退役留档，含 README、CLAUDE.md、API接口文档.md、uploads）
- `ios_todolist_prototype/` — iOS TodoList 原型（HTML，设计参考）
- `ios/` — SwiftUI app 工程（XcodeGen，阶段 1 骨架已编译通过，含 README）
- `supabase/` — Supabase 后端（migrations/ 建表迁移、scripts/e2e.mjs 端到端测试、API.md iOS 端接口约定）
- `.claude/settings.local.json` — 旧 WSL 开发遗留权限，已过时可忽略

## 开发进度（2026-10-21）
- Supabase 云端项目已建并连接（ref `prkijbbjrjfmkolkjxsk`），阶段 1/2 建表迁移已推送验证
- 阶段 1~3 服务端端到端验证通过：`supabase/scripts/e2e.mjs`（20 项全绿，可重复跑：绑定 + 阶段2 CRUD/RLS 隔离 + 推送链路）
- 阶段 3 推送服务端就绪：OneSignal 密钥已配，send-push / anniversary-reminder 已部署验证，pg_cron 每天 07:00（北京时间）跑纪念日提醒，心愿新增自动通知另一半
- iOS 阶段 1 骨架**首次编译通过**（`xcodebuild` BUILD SUCCEEDED，arm64 真机目标）：此前修掉 3 类必炸编译错误（supabase-swift 2.x 的 rpc 链式取值 / OneSignal 模块名 / project.yml 产品名）
- OneSignal 依赖改为官方预编译 XCFramework + `ios/scripts/fetch-onesignal.sh`（`Vendor/OneSignal/` 不入库），不再依赖那个 3GB 的源码仓库
- iOS 阶段 2 已实装（`6d525db`）：纪念日 / 心愿单全 CRUD + `APIClient` 写接口；同时修掉 PostgREST 不做 snake_case 转换导致的解码 bug（原模型非可选字段会 keyNotFound，`AppState.couple` 会永远 nil）
- iOS **已在模拟器跑起来**（iOS 27.1 / iPhone Duo）：装包、启动、登录页渲染通过；真机 arm64 与模拟器 arm64 双目标构建全绿
- iOS 阶段 3 推送侧完成（`d78e7da`）：权限申请只弹一次、通知点击按 `type` 切 Tab（anniversary/wishlist/couple_anniversary）、登出解绑 `OneSignal.logout()`
- 主页倒计时改读 `anniversaries`（`3e61ee0`）：按年复现取最近一条、今天显示「就是今天」、空/失败回退恋爱周年、闰日归 2/28
- 新增「我们」页 + 登出入口（`26ea455`）：两人昵称、在一起天数、二次确认登出（此前 App 内没有任何登出 UI）
- 剩余：**运行态端到端**（注册→绑定→CRUD→登出真链路，需人点或真机）、推送真机验证（$99 开发者账号 + APNs 证书 + Push capability）、照片（阶段 4，需阿里云 OSS，约 1 元/月）、上架准备（Bundle ID 拍板、图标、启动图）

## 本机环境坑（已踩过）
- Xcode 装完**必须打开一次**装附加组件，否则 `xcodebuild` 崩在 `IDESimulatorFoundation` 插件加载（缺 `/Library/Developer/PrivateFrameworks/CoreSimulator.framework`）
- `xcode-select` 可能仍指向 CommandLineTools：`sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`
- 验收命令：`cd ios && xcodegen generate && xcodebuild -project Ours.xcodeproj -scheme Ours -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build`

## 待定事项（未拍板前不要擅自决定）
- 留言板/悄悄话做不做（倾向做）
- app 名称（已定「Ours」，2026-08-04 拍板）
- Bundle ID
