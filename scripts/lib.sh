# Shared helpers. Sourced by the other scripts.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$ROOT/state" "$ROOT/shots"
ADB=$(command -v adb || true)
for p in "$HOME/Library/Android/sdk/platform-tools/adb" "$HOME/Android/Sdk/platform-tools/adb" /opt/homebrew/bin/adb /usr/local/bin/adb; do
  [ -z "$ADB" ] && [ -x "$p" ] && ADB="$p"
done
[ -z "$ADB" ] && { echo "!! adb not found. macOS: brew install --cask android-platform-tools   Linux: sudo apt install adb"; exit 1; }
IP="${TV_IP:-$(cat "$ROOT/.tv_ip" 2>/dev/null)}"
[ -z "$IP" ] && { echo "!! No TV IP yet. Run ./scripts/01-connect-and-backup.sh <TV-IP> first (or set TV_IP=...)"; exit 1; }
case "$IP" in *:*) D="$IP";; *) D="$IP:5555";; esac
"$ADB" connect "$D" >/dev/null 2>&1
[ "$("$ADB" -s "$D" get-state 2>/dev/null)" = "device" ] || { echo "!! TV $D not connected/authorised. Check the TV for the 'Allow USB debugging?' prompt."; exit 1; }
A() { "$ADB" -s "$D" "$@"; }
S() { "$ADB" -s "$D" shell "$@" | tr -d '\r'; }
shot() { # shot <name>: screenshot from the TV into shots/<name>.png
  S screencap -p /sdcard/tvshot.png; A pull /sdcard/tvshot.png "$ROOT/shots/$1.png" >/dev/null 2>&1
  echo "   screenshot: shots/$1.png ($(wc -c < "$ROOT/shots/$1.png" | tr -d ' ') bytes)"
}
top() { S dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity' | sed 's/^ *//'; }
k() { S input keyevent "$1"; sleep "${2:-1.2}"; }
