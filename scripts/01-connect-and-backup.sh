#!/bin/bash
# Step 1: find the TV, connect over ADB, and back up its state. READ-ONLY: changes nothing on the TV.
# Usage: ./scripts/01-connect-and-backup.sh [TV_IP]
set -u
cd "$(dirname "$0")/.."

# --- locate adb ---
ADB=$(command -v adb || true)
for p in "$HOME/Library/Android/sdk/platform-tools/adb" /opt/homebrew/bin/adb /usr/local/bin/adb; do
  [ -z "$ADB" ] && [ -x "$p" ] && ADB="$p"
done
if [ -z "$ADB" ]; then
  echo "!! adb not found. Install it with:  brew install --cask android-platform-tools   then re-run."; exit 1
fi
echo "== adb: $ADB ($("$ADB" version | head -1))"

# --- find the TV ---
IP="${1:-}"
if [ -z "$IP" ]; then
  echo "== No IP given. Probing devices in the ARP table for an open port 5555..."
  ping -c1 -t1 255.255.255.255 >/dev/null 2>&1
  for h in $(arp -an | sed -n 's/.*(\([0-9.]*\)).*/\1/p' | sort -u); do
    if nc -G1 -z "$h" 5555 2>/dev/null; then echo "   port 5555 open on $h"; IP="$h"; fi
  done
  echo "== ARP table (for reference):"; arp -a
fi
if [ -z "$IP" ]; then
  echo "!! Couldn't find a device with ADB open. On the TV: Settings > Device Preferences > About > tap Build 7 times,"
  echo "   then Developer options > turn on USB debugging (and Network/ADB debugging if listed). Find its IP under"
  echo "   Settings > Network, then run:  ./scripts/01-connect-and-backup.sh <TV_IP>"; exit 1
fi
echo "== TV IP: $IP"; nc -G2 -vz "$IP" 5555

# --- connect ---
echo ""
echo ">>> LOOK AT THE TV NOW: an 'Allow USB debugging?' prompt should appear."
echo ">>> Tick 'Always allow from this computer' and choose OK."
"$ADB" connect "$IP:5555"
for i in $(seq 1 30); do
  st=$("$ADB" -s "$IP:5555" get-state 2>/dev/null)
  [ "$st" = "device" ] && break
  sleep 2; [ $((i%5)) -eq 0 ] && "$ADB" connect "$IP:5555" >/dev/null 2>&1
done
if [ "$("$ADB" -s "$IP:5555" get-state 2>/dev/null)" != "device" ]; then
  echo "!! Not authorised/connected. If you saw 'No route to host' although port 5555 is open, that's macOS Local"
  echo "   Network privacy blocking adb: System Settings > Privacy & Security > Local Network > allow Terminal. Then re-run."
  "$ADB" devices -l; exit 1
fi
A() { "$ADB" -s "$IP:5555" "$@"; }
S() { "$ADB" -s "$IP:5555" shell "$@"; }
echo "$IP" > .tv_ip

# --- back up ---
B="backup/$(date +%Y%m%d-%H%M%S)"; mkdir -p "$B"
echo "== Backing up to $B"
S getprop                         > "$B/getprop.txt"
S pm list packages -f -u          > "$B/packages-all-with-paths.txt"
S pm list packages                > "$B/packages-enabled-or-all.txt"
S pm list packages -d             > "$B/packages-disabled.txt"
S pm list packages -s             > "$B/packages-system.txt"
S pm list packages -3             > "$B/packages-thirdparty.txt"
S pm list packages -e             > "$B/packages-enabled.txt"
for ns in system secure global; do S settings list $ns > "$B/settings-$ns.txt"; done
S cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME > "$B/home-activities.txt" 2>&1
S cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME > "$B/home-current.txt" 2>&1
S dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity' > "$B/top-activity.txt"
S dumpsys meminfo | head -40      > "$B/meminfo.txt"
S dumpsys wifi | grep -o 'cname=[^ ]*' | sort -u > "$B/wifi-network-owners.txt"
S ls /sys/bus/usb/devices         > "$B/usb-devices.txt" 2>&1
S screencap -p /sdcard/tv_shot.png && A pull /sdcard/tv_shot.png "$B/screen-00-before.png" >/dev/null

# per-package detail (activities/services) for categorising, compressed
echo "== Dumping package details (takes a minute)..."
mkdir -p "$B/pkg"
for p in $(sed 's/^package://' "$B/packages-system.txt" | tr -d '\r'); do
  S dumpsys package "$p" | grep -E 'Activity|Service|Receiver|Provider|versionName|enabled=|  [0-9a-f]+ [a-zA-Z]' | head -60 > "$B/pkg/$p.txt"
done
tar czf "$B.tgz" -C backup "$(basename "$B")"

echo ""
echo "== DONE. Key facts:"
echo "   model:   $(grep -E 'ro.product.model]|ro.product.marketname]' "$B/getprop.txt" | tr '\n' ' ')"
echo "   region:  $(grep -E 'ro.miui.region|ro.product.locale|ro.boot.hwc' "$B/getprop.txt" | tr '\n' ' ')"
echo "   android: $(grep -E 'ro.build.version.release]|ro.build.version.sdk]' "$B/getprop.txt" | tr '\n' ' ')"
echo "   adb.secure: $(grep 'ro.adb.secure' "$B/getprop.txt")"
echo "   packages: $(wc -l < "$B/packages-all-with-paths.txt") total, $(wc -l < "$B/packages-disabled.txt") already disabled"
echo "   backup:  $PWD/$B  (+ $B.tgz)"
