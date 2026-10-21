#!/bin/bash
# Quick health check: is the TV reachable, what's on screen, is Wi-Fi up, any crashes.
cd "$(dirname "$0")/.."; source scripts/common.sh; find_adb || exit 1
IP=$(cat .tv_ip 2>/dev/null); [ -z "$IP" ] && { echo "No .tv_ip yet - run scripts/01-connect-and-backup.sh <tv-ip>"; exit 1; }
echo "== port 5555 on $IP: $(port_open "$IP" 5555 && echo open || echo CLOSED)"
echo "== adb:"; "$ADB" disconnect "$IP:5555" >/dev/null 2>&1; "$ADB" connect "$IP:5555"; sleep 2; "$ADB" devices -l
if [ "$("$ADB" -s "$IP:5555" get-state 2>/dev/null)" = device ]; then
  S() { "$ADB" -s "$IP:5555" shell "$@" | tr -d '\r'; }
  echo "== top: $(S dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity')"
  echo "== wifi: $(S dumpsys wifi | grep -m1 -o 'SSID: [^,]*')"
  echo "== uptime: $(S uptime)"
  echo "== recent crashes:"; S logcat -d -b crash | tail -20
  mkdir -p shots; S screencap -p /sdcard/s.png && "$ADB" -s "$IP:5555" pull /sdcard/s.png shots/status.png >/dev/null && echo "screenshot: shots/status.png"
fi
