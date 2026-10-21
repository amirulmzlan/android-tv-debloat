#!/bin/bash
# Disable one batch of packages listed in a file (one package per line, '#' comments allowed), then check the TV.
# Never uninstalls. Every disabled package is recorded in state/disabled.txt for scripts/restore.sh.
# Usage: ./scripts/02-debloat.sh lists/01-telemetry.txt
source "$(dirname "$0")/lib.sh"
LIST="${1:-}"; [ -f "$LIST" ] || { echo "Usage: $0 <package-list-file>"; exit 1; }
NAME=$(basename "$LIST" .txt)
LOG="$ROOT/state/batch-$NAME.log"; : > "$LOG"
touch "$ROOT/state/disabled.txt"
echo "== Batch $NAME" | tee -a "$LOG"
for p in $(sed 's/#.*//' "$LIST" | tr -s ' \t' '\n' | grep -v '^$'); do
  if ! S pm list packages | grep -qx "package:$p"; then echo "   $p -> not installed, skipped" | tee -a "$LOG"; continue; fi
  r=$(S pm disable-user --user 0 "$p" 2>&1)                       # one pm call per shell call
  if S pm list packages -d | grep -qx "package:$p"; then
    echo "   $p -> disabled" | tee -a "$LOG"; echo "$p" >> "$ROOT/state/disabled.txt"
  else
    echo "!! $p -> NOT disabled: $r" | tee -a "$LOG"
  fi
done
sort -u -o "$ROOT/state/disabled.txt" "$ROOT/state/disabled.txt"
sleep 2
echo "== Checks" | tee -a "$LOG"
k KEYCODE_HOME 4
echo "   top: $(top)" | tee -a "$LOG"
echo "   wifi: $(S dumpsys wifi | grep -m1 -oE 'SSID: [^,]*')" | tee -a "$LOG"
shot "batch-$NAME-home"
S am start -a android.settings.SETTINGS >/dev/null 2>&1; sleep 3
echo "   settings: $(top)" | tee -a "$LOG"; shot "batch-$NAME-settings"
k KEYCODE_HOME 1
echo "== Done. $(wc -l < "$ROOT/state/disabled.txt" | tr -d ' ') packages disabled so far. Tell your AI agent 'done'."
