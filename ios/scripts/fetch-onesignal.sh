#!/usr/bin/env bash
# 拉取 OneSignal 预编译 XCFramework 到 ios/Vendor/OneSignal
#
# 为什么不用 SwiftPM 源码包：OneSignal-iOS-SDK 仓库历史约 3GB，
# SwiftPM 做的是全量 --mirror 克隆且不支持断点续传，大陆网络会反复 SSL_ERROR_SYSCALL 重来。
# 官方 release 里的分体 XCFramework 合计约 54MB，按需下载即可。
set -euo pipefail

VERSION="${ONESIGNAL_VERSION:-5.8.0}"
BASE="https://github.com/OneSignal/OneSignal-iOS-SDK/releases/download/${VERSION}"

# 只取 OneSignalFramework 及其动态链接依赖，不取 InAppMessages / Location
FRAMEWORKS=(
  OneSignalCore
  OneSignalOSCore
  OneSignalNotifications
  OneSignalExtension
  OneSignalOutcomes
  OneSignalUser
  OneSignalLiveActivities
  OneSignalFramework
)

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/Vendor/OneSignal"
CACHE="$HOME/Library/Caches/LoveDiary/onesignal-${VERSION}"
mkdir -p "$DEST" "$CACHE"

for fw in "${FRAMEWORKS[@]}"; do
  if [ -d "$DEST/$fw.xcframework" ]; then
    echo "✓ $fw 已就位"
    continue
  fi
  zip="$CACHE/$fw.xcframework.zip"
  if [ ! -s "$zip" ]; then
    echo "↓ $fw"
    curl -fL --retry 5 --retry-all-errors --retry-delay 2 --max-time 600 \
      -o "$zip" "${BASE}/$fw.xcframework.zip"
  fi
  unzip -oq "$zip" -d "$DEST"
  echo "✓ $fw"
done

echo
echo "完成：$(ls -1 "$DEST" | wc -l | tr -d ' ') 个框架 → $DEST"
echo "接着跑：cd ios && xcodegen generate"
