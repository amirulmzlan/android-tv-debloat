#!/bin/bash
# Uninstall apps the person installed themselves (system packages are refused - disable those instead).
# Usage: ./scripts/uninstall.sh <package> [package...]
source "$(dirname "$0")/lib.sh"
[ $# -eq 0 ] && { echo "Usage: $0 <package> [package...]"; exit 1; }
for p in "$@"; do
  if S pm list packages -s | grep -qx "package:$p"; then echo "   $p is a system package - skipped"; continue; fi
  S pm list packages | grep -qx "package:$p" || { echo "   $p not installed"; continue; }
  echo "   uninstall $p: $(A uninstall "$p" 2>&1 | tail -1)"; echo "$p" >> "$ROOT/state/uninstalled.txt"
done
k KEYCODE_HOME 3; shot after-uninstall
