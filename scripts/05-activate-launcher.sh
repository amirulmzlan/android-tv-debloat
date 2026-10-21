#!/bin/bash
# Make TvHome the default home screen. Run only after 04 showed it working.
# Disables any other HOME app that outranks it (the stock launcher) and records it for restore.sh.
source "$(dirname "$0")/lib.sh"
LOG="$ROOT/state/activate.log"; exec > >(tee "$LOG") 2>&1
PKG=net.amirul.tvhome; ACT=$PKG/.HomeActivity
S pidof $PKG >/dev/null || { S am start -n $ACT >/dev/null; sleep 3; }
S pidof $PKG >/dev/null || { echo "!! TvHome isn't running - fix that first (scripts/04)."; exit 1; }
echo "== Apps that claim HOME (priority, package):"
HOMES=$(S cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME | \
  awk '/priority=/{match($0,/priority=-?[0-9]+/); pr=substr($0,RSTART+9,RLENGTH-9); want=1}
       /^ *packageName=/{ if(want){ sub(/.*packageName=/,""); print pr" "$0; want=0 } }' | sort -u)
echo "$HOMES" | sed 's/^/   /'
WIFI_OWNERS=$(S dumpsys wifi | grep -o 'cname=[^ ]*' | sort -u)
echo "$HOMES" | while read -r pr p; do
  [ -z "$p" ] && continue
  case "$p" in $PKG|com.android.tv.settings|com.android.settings) continue;; esac
  [ "$pr" -gt 0 ] || continue
  if [ "$p" = com.google.android.tungsten.setupwraith ] && echo "$WIFI_OWNERS" | grep -q setupwraith; then
    echo "!! The setup wizard claims HOME AND owns your saved Wi-Fi. Disabling it would delete that network."
    echo "   On the TV: Settings > Network > forget your Wi-Fi, join it again, then re-run this script."; exit 2
  fi
  echo "   disabling $p (priority $pr outranks TvHome): $(S pm disable-user --user 0 $p)"
  echo "$p" >> "$ROOT/state/disabled.txt"
done || exit $?
sort -u -o "$ROOT/state/disabled.txt" "$ROOT/state/disabled.txt"
echo "== set-home-activity: $(S cmd package set-home-activity $ACT)"
S am start -a android.settings.SETTINGS >/dev/null 2>&1; sleep 2
k KEYCODE_HOME 4
echo "   after Home key: $(top)"; shot activate-after-home
echo "   wifi: $(S dumpsys wifi | grep -m1 -oE 'SSID: [^,]*')"
echo "== Done. Tell your AI agent 'done'."
