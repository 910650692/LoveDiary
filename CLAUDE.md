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
- `ios/` — SwiftUI app 工程（规划中，尚未创建）
- `supabase/` — Supabase 后端（migrations/ 建表迁移、scripts/e2e.mjs 端到端测试、API.md iOS 端接口约定）
- `.claude/settings.local.json` — 旧 WSL 开发遗留权限，已过时可忽略

## 开发进度（2026-08-09）
- Supabase 云端项目已建并连接（ref `prkijbbjrjfmkolkjxsk`），阶段 1/2 建表迁移已推送验证
- 阶段 1 绑定流程端到端验证通过：`supabase/scripts/e2e.mjs`（9 项全绿，可重复跑）
- 阶段 2 表（anniversaries / wishlist_items）已上线
- 剩余：iOS 工程（阶段 1 验收「Mac 上跑通」）、推送（阶段 3，需 OneSignal 账号）、照片（阶段 4，需阿里云 OSS）

## 待定事项（未拍板前不要擅自决定）
- 留言板/悄悄话做不做（倾向做）
- app 名称（已定「Ours」，2026-08-04 拍板）
- Bundle ID
