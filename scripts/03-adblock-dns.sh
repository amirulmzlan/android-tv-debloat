#!/bin/bash
# Optional: ad-blocking Private DNS (AdGuard). Doesn't block YouTube ads.
# Undo: ./scripts/03-adblock-dns.sh off
source "$(dirname "$0")/lib.sh"
LOG="$ROOT/state/dns.log"; : > "$LOG"
if [ "${1:-}" = off ]; then
  S settings put global private_dns_mode opportunistic; echo "Private DNS back to automatic."; exit 0
fi
echo "== before: mode=$(S settings get global private_dns_mode) host=$(S settings get global private_dns_specifier)" | tee -a "$LOG"
S settings put global private_dns_specifier dns.adguard-dns.com
S settings put global private_dns_mode hostname
sleep 5
echo "== after:  mode=$(S settings get global private_dns_mode) host=$(S settings get global private_dns_specifier)" | tee -a "$LOG"
echo "== DNS checks" | tee -a "$LOG"
for h in www.google.com www.youtube.com www.netflix.com doubleclick.net ads.google.com; do
  r=$(S ping -c1 -W2 $h 2>&1 | head -1)
  echo "   $h -> $r" | tee -a "$LOG"
done
echo "== Launching YouTube to check playback"
S monkey -p com.google.android.youtube.tv -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null 2>&1
sleep 12; echo "   top: $(top)" | tee -a "$LOG"; shot dns-youtube
S input keyevent KEYCODE_DPAD_CENTER; sleep 10; shot dns-youtube-play
echo "   top: $(top)" | tee -a "$LOG"
S input keyevent KEYCODE_HOME
echo "== Done. Tell your AI agent 'done'."
