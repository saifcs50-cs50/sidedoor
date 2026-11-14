#!/usr/bin/env bash
# Build, install and switch on Sidedoor over adb - no tapping through Settings.
#   tools/install.sh [serial] [debug|release]
set -euo pipefail
cd "$(dirname "$0")/.."

SERIAL="${1:-${ANDROID_SERIAL:-}}"
VARIANT="${2:-debug}"
PKG=com.sidedoor.app
GUARD="$PKG/$PKG.guard.GuardService"
REELS="$PKG/$PKG.notify.ReelInboxListener"
adb() { command adb ${SERIAL:+-s "$SERIAL"} "$@"; }

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
TASK="assemble$(tr '[:lower:]' '[:upper:]' <<< "${VARIANT:0:1}")${VARIANT:1}"
./gradlew "$TASK" --console=plain -q
adb install -r "app/build/outputs/apk/$VARIANT/app-$VARIANT.apk"

adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS
adb shell cmd notification allow_listener "$REELS"

# Straight after an install the package manager can reset this setting, so
# write it until it sticks. Other enabled services are kept.
for _ in 1 2 3 4 5; do
  current="$(adb shell settings get secure enabled_accessibility_services | tr -d '\r')"
  [[ "$current" == *"$GUARD"* ]] && break
  if [[ -z "$current" || "$current" == "null" ]]; then next="$GUARD"; else next="$current:$GUARD"; fi
  adb shell settings put secure enabled_accessibility_services "$next"
  adb shell settings put secure accessibility_enabled 1
  sleep 2
done

current="$(adb shell settings get secure enabled_accessibility_services | tr -d '\r')"
if [[ "$current" == *"$GUARD"* ]]; then echo "Sidedoor $VARIANT installed; guard and reels reader on."; else echo "Guard did not stick: $current" >&2; exit 1; fi
