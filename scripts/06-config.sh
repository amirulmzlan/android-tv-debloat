#!/bin/bash
# Personalise TvHome without rebuilding: writes home.conf, pushes it to the TV, restarts the launcher.
# Usage (agent / non-interactive):
#   ./scripts/06-config.sh --columns 6 --dock com.google.android.youtube.tv,com.netflix.ninja \
#                          --team-id 364 --team-name "Liverpool FC" --hidden com.example.app
# Usage (person): ./scripts/06-config.sh          asks questions
source "$(dirname "$0")/lib.sh"
PKG=net.amirul.tvhome
COLS=""; DOCK=""; TID=""; TNAME=""; HID=""
while [ $# -gt 0 ]; do case "$1" in
  --columns) COLS="$2"; shift 2;; --dock) DOCK="$2"; shift 2;; --hidden) HID="$2"; shift 2;;
  --team-id) TID="$2"; shift 2;; --team-name) TNAME="$2"; shift 2;; *) echo "unknown option $1"; exit 1;; esac; done
if [ -z "$COLS$DOCK$TID$HID" ]; then
  echo "== Apps on your TV:"
  APPS=$(S cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER | grep -o 'packageName=[^ ]*' | cut -d= -f2 | sort -u | grep -v "^$PKG$")
  i=0; for a in $APPS; do i=$((i+1)); printf "   %2d) %s\n" $i "$a"; done
  read -r -p "How many columns / dock apps? [6] " COLS; COLS=${COLS:-6}
  read -r -p "Dock apps, by number, left to right (e.g. 3 1 7 2): " NUMS
  for n in $NUMS; do a=$(echo "$APPS" | sed -n "${n}p"); [ -n "$a" ] && DOCK="${DOCK:+$DOCK,}$a"; done
  read -r -p "Football club ESPN team id for a next-match card (364 Liverpool, 359 Arsenal, 360 Man Utd, 363 Chelsea, 382 Man City, 367 Spurs; empty = none): " TID
  [ -n "$TID" ] && read -r -p "Club name to show: " TNAME
fi
CONF="$ROOT/state/home.conf"
{
  echo "# TvHome settings - see docs/LAUNCHER.md"
  [ -n "$COLS" ]  && echo "columns=$COLS"
  [ -n "$DOCK" ]  && echo "dock=$DOCK"
  [ -n "$HID" ]   && echo "hidden=$HID"
  echo "team_id=$TID"
  echo "team_name=$TNAME"
} > "$CONF"
echo "== home.conf:"; sed 's/^/   /' "$CONF"
A push "$CONF" /sdcard/Android/data/$PKG/files/home.conf >/dev/null 2>&1 || {
  S mkdir -p /sdcard/Android/data/$PKG/files; A push "$CONF" /sdcard/Android/data/$PKG/files/home.conf >/dev/null; }
S am force-stop $PKG; k KEYCODE_HOME 5
echo "   top: $(top)"; shot config-home
echo "== Done. Tell your AI agent 'done'."
