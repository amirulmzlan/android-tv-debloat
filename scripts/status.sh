#!/bin/bash
# Quick health check: is the TV reachable, what's on screen, is Wi-Fi up.
cd "$(dirname "$0")/.."; IP=$(cat .tv_ip)
ADB=$(command -v adb || echo "$HOME/Library/Android/sdk/platform-tools/adb")
echo "== ping:";  ping -c2 -t3 "$IP" | tail -2
echo "== port 5555:"; nc -G3 -vz "$IP" 5555 2>&1
echo "== adb:"; "$ADB" disconnect "$IP:5555" >/dev/null 2>&1; "$ADB" connect "$IP:5555"; sleep 2; "$ADB" devices -l
if [ "$("$ADB" -s "$IP:5555" get-state 2>/dev/null)" = device ]; then
  S() { "$ADB" -s "$IP:5555" shell "$@" | tr -d '\r'; }
  echo "== top: $(S dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity')"
  echo "== wifi: $(S dumpsys wifi | grep -m1 -o 'SSID: [^,]*')"
  echo "== uptime: $(S uptime)"
  echo "== recent crashes:"; S logcat -d -b crash | tail -20
  S screencap -p /sdcard/s.png && "$ADB" -s "$IP:5555" pull /sdcard/s.png shots/status.png >/dev/null && echo "screenshot: shots/status.png"
fi
