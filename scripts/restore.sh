#!/bin/bash
# Re-enable everything the scripts disabled (or one package), put DNS back to automatic and the stock home back.
# Usage: ./scripts/restore.sh            everything
#        ./scripts/restore.sh <package>  just one
source "$(dirname "$0")/lib.sh"
if [ -n "${1:-}" ]; then LIST="$1"; else LIST=$(sort -u "$ROOT/state/disabled.txt" 2>/dev/null); fi
[ -z "$LIST" ] && echo "Nothing recorded as disabled."
for p in $LIST; do
  echo "   enable $p: $(S pm enable --user 0 "$p")"
  [ -n "${1:-}" ] && { grep -vx "$p" "$ROOT/state/disabled.txt" > "$ROOT/state/.d" 2>/dev/null; mv "$ROOT/state/.d" "$ROOT/state/disabled.txt"; }
done
if [ -z "${1:-}" ]; then
  S settings put global private_dns_mode opportunistic; echo "   Private DNS back to automatic."
  [ -f "$ROOT/state/stock-home.txt" ] && { H=$(cat "$ROOT/state/stock-home.txt"); S cmd package set-home-activity "$H" >/dev/null; echo "   Home set back to $H"; }
  : > "$ROOT/state/disabled.txt"
fi
echo "Still disabled on the TV:"; S pm list packages -d
