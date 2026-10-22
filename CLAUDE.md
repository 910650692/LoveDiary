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
- ✅ **阶段 1 验收达成（2026-10-21）**：两个真实账号在模拟器完成注册 → 建关系 → 邀请码绑定，主页显示真实数据（在一起 1093 天 / 周年倒计时 3 天）——Auth → RLS → 解码 → 计算的第一次真实贯通
- Supabase 已关闭「Confirm email」（原 Site URL 指向 `http://localhost:3000`，确认邮件链接点开必然无效）
- iOS 阶段 3 推送侧完成（`d78e7da`）：权限申请只弹一次、通知点击按 `type` 切 Tab（anniversary/wishlist/couple_anniversary）、登出解绑 `OneSignal.logout()`
- 主页倒计时改读 `anniversaries`（`3e61ee0`）：按年复现取最近一条、今天显示「就是今天」、空/失败回退恋爱周年、闰日归 2/28
- 新增「我们」页 + 登出入口（`26ea455`）：两人昵称、在一起天数、二次确认登出（此前 App 内没有任何登出 UI）
- 阶段 4 照片已开工：设计定稿 + **建表迁移已推送并核验**（`20261021000000_stage4_photos`：4 张表全开 RLS，共 15 条策略，索引齐全，台账已记录）+ iOS 图片管线（压缩/缩略图/EXIF/方向，纯 CoreGraphics，可在命令行实测）
- ✅ OSS 接入完成（2026-10-21）：bucket `ours-photos-jw`（上海，私有）+ RAM 最小权限；`oss-sign` Edge Function 已部署并**真链路验证**（临时用户 JWT → 200 → 预签名 URL 直传 PUT 200 → GET 200 内容一致）
- ✅ iOS 照片上传链路完成（`91cdead`）：第 5 个 Tab + PhotoPipeline → 批量签名（一批一次）→ 并发 2 直传 → 插 `photos` 行；Bundle ID 定 `app.ours.diary`（`35556a5`）
- 剩余：**时间线网格**（缩略图 + 分页 + 看原图，上传页目前只有最简列表）、**真机验证真实上传**（需真机 + 真照片）、**阶段 2 真实数据验收**（纪念日/心愿单 CRUD 未在真库点过）、**「修改在一起日期」入口**（当前只能改库）、**推送真机验证（需 $99 账号 + APNs .p8）**、正式视觉替换

## 本机环境坑（已踩过）
- Xcode 装完**必须打开一次**装附加组件，否则 `xcodebuild` 崩在 `IDESimulatorFoundation` 插件加载（缺 `/Library/Developer/PrivateFrameworks/CoreSimulator.framework`）
- `xcode-select` 可能仍指向 CommandLineTools：`sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`
- 验收命令：`cd ios && xcodegen generate && xcodebuild -project Ours.xcodeproj -scheme Ours -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build`
  - **必须带 `-scheme Ours`**：不带时 xcodebuild 会忽略 destination（`Ignoring provided run destination because no scheme was passed`），SwiftPM 包图降级，报 3 个第三方包 unresolved module（xctest-dynamic-overlay / swift-clocks）——不是代码问题
- UI 测试：`xcodebuild test -project Ours.xcodeproj -scheme Ours -destination 'platform=iOS Simulator,name=iPhone Duo'`（6 条用例：登录页/注册页/四 Tab/我们页/登出/照片自测页）
- **GitHub 推送走 SSH 443**：本机代理会掐断 22 端口上的大流量（`ssh -T` 能过但 `git push` 断）。
  `~/.ssh/config` 已把 `github.com` 指向 `ssh.github.com:443` + 专用密钥 `id_ed25519_github`；
  公司用的 `id_ed25519`（bastion / 内网）不要与个人 GitHub 混用

## 待定事项（未拍板前不要擅自决定）
- 留言板/悄悄话做不做（倾向做）
- app 名称（已定「Ours」，2026-08-04 拍板）
- Bundle ID —— **已定 `app.ours.diary`**（2026-10-21 拍板，注册 App ID 后不能再改）
