#!/usr/bin/env bash
# On-device regression check for the guard. Run it after every Instagram
# update (Play Store updates are exactly what break view-ID based rules).
#
#   tools/device-check.sh [serial]
#
# Safe by construction: it never opens a chat (read receipts), never taps a
# story (view receipts), never likes or follows anything. It only launches
# Instagram (from its icon, and through Sidedoor's shortcuts), taps the covered
# tabs, swipes between tabs, and reads the guard's own log lines.
set -uo pipefail

SERIAL="${1:-${ANDROID_SERIAL:-}}"
IG=com.instagram.android
PKG=com.sidedoor.app
adb() { command adb ${SERIAL:+-s "$SERIAL"} "$@"; }

pass=0; fail=0
result() { if [[ $1 == 0 ]]; then echo "  PASS  $2"; pass=$((pass + 1)); else echo "  FAIL  $2"; fail=$((fail + 1)); fi; }

log_has() { adb logcat -d -s Sidedoor | grep -q "$1"; }
wait_log() { # pattern, seconds
  local tries=$(($2 * 4))
  for ((i = 0; i < tries; i++)); do log_has "$1" && return 0; sleep 0.25; done
  return 1
}
overlays() { adb shell dumpsys window windows | grep -cE "Window\{[0-9a-f]+ u0 $PKG"; }
fresh_log() { adb logcat -c; }

# Centre of the second tab-bar patch from the left (the Reels tab), read from
# Sidedoor's own overlay windows - uiautomator cannot dump the inbox while its
# notes tray animates, and tapping the patch is exactly what a thumb would do.
reels_patch_centre() {
  adb shell dumpsys window windows |
    awk -v pkg="$PKG" '$0 ~ "Window\\{[0-9a-f]+ u0 " pkg { want = 1 } want && /frame=\[/ { print; want = 0 }' |
    grep -oE 'frame=\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]' |
    awk -F'[][,=]' '{ print $3, $4, $6, $7 }' |
    sort -k2,2nr -k1,1n |
    awk 'NR == 1 { bottom = $2 } $2 == bottom { n++; if (n == 2) { printf "%d %d", ($1 + $3) / 2, ($2 + $4) / 2; exit } }'
}

echo "Instagram $(adb shell dumpsys package $IG | grep -m1 versionName | cut -d= -f2 | tr -d '\r')," \
  "Sidedoor $(adb shell dumpsys package $PKG | grep -m1 versionName | cut -d= -f2 | tr -d '\r')"

adb shell settings get secure enabled_accessibility_services | grep -q "$PKG"
result $? "guard is switched on"

launch_from_icon() { adb shell monkey -p $IG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; }

echo "Instagram opened normally is left alone"
adb shell am force-stop $IG; fresh_log
launch_from_icon; sleep 5
! log_has "screen=" && ! log_has "session started"; result $? "the guard does not act or read the screen"
[[ $(overlays) -eq 0 ]]; result $? "no overlays"

echo "Chats shortcut starts a guarded session"
fresh_log
adb shell am start -a $PKG.action.OPEN_CHATS -n $PKG/.ui.MainActivity >/dev/null
wait_log "session started" 8; result $? "session starts"
wait_log "screen=INBOX" 8; result $? "opens the inbox"
sleep 1.5
[[ $(overlays) -ge 3 ]]; result $? "home, reels and search tabs are covered"

echo "Covered Reels tab"
read -r rx ry <<< "$(reels_patch_centre)"
fresh_log
if [[ -n "${rx:-}" ]]; then adb shell input tap "$rx" "$ry"; fi
sleep 2
[[ -n "${rx:-}" ]] && ! log_has "screen=REELS_TAB"; result $? "tapping it does nothing"

echo "Swipe sideways into Reels"
fresh_log
# Start beside the "Messages" label: inbox rows carry story avatars, which are
# covered and would (correctly) eat the swipe.
adb shell input swipe 250 615 700 615 200
wait_log "blocked REELS_TAB" 5; result $? "reels tab is blocked"
wait_log "screen=INBOX" 6; result $? "back in the inbox"
sleep 5
! log_has "Canary failed"; result $? "canary recognises the inbox"

echo "Going home ends the session"
fresh_log
adb shell input keyevent KEYCODE_HOME; sleep 1.5
[[ $(overlays) -eq 0 ]]; result $? "no overlay is left on the home screen"
wait_log "session ended" 3; result $? "session ends"
fresh_log
launch_from_icon; sleep 4
! log_has "screen=" && [[ $(overlays) -eq 0 ]]; result $? "reopening from the icon is not guarded"

echo "Saved shortcut, warm and cold"
for start in warm cold; do
  [[ $start == cold ]] && adb shell am force-stop $IG
  sleep 1; fresh_log
  adb shell am start -a $PKG.action.OPEN_SAVED -n $PKG/.ui.MainActivity >/dev/null
  wait_log "Saved trip arrived" 15; result $? "reaches Saved ($start)"
done

echo "Leaving Instagram"
adb shell input keyevent KEYCODE_HOME; sleep 1.5
[[ $(overlays) -eq 0 ]]; result $? "no overlay is left on the home screen"

echo "$pass passed, $fail failed"
exit "$fail"
