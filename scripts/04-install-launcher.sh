#!/bin/bash
# Install the TvHome launcher on the TV and test it (opens it directly; does NOT make it the default yet).
# Usage: ./scripts/04-install-launcher.sh            download the latest release APK from GitHub
#        ./scripts/04-install-launcher.sh --build    build from launcher/ (Android SDK + JDK 17 needed)
#        ./scripts/04-install-launcher.sh <file.apk> install a specific APK
source "$(dirname "$0")/lib.sh"
LOG="$ROOT/state/install-launcher.log"; exec > >(tee "$LOG") 2>&1
PKG=net.amirul.tvhome; ACT=$PKG/.HomeActivity
APK="$ROOT/state/TvHome.apk"
case "${1:-}" in
  --build)
    export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
    [ -d "$ANDROID_HOME" ] || export ANDROID_HOME="$HOME/Android/Sdk"
    command -v /usr/libexec/java_home >/dev/null && export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || echo "${JAVA_HOME:-}")"
    cd "$ROOT/launcher"; echo "sdk.dir=$ANDROID_HOME" > local.properties
    if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
      T=$(mktemp -d); ( cd "$T" && touch settings.gradle && gradle -q wrapper --gradle-version 8.7 --distribution-type all ) || { echo "!! needs 'gradle' once to create the wrapper"; exit 1; }
      cp -R "$T/gradlew" "$T/gradlew.bat" "$T/gradle" . ; rm -rf "$T"
    fi
    chmod +x gradlew; ./gradlew --no-daemon -q assembleDebug || { echo "!! BUILD FAILED"; exit 1; }
    cp app/build/outputs/apk/debug/app-debug.apk "$APK"; cd "$ROOT";;
  "")
    REPO="${REPO:-$(git -C "$ROOT" remote get-url origin 2>/dev/null | sed -E 's#.*github.com[:/]##; s#\.git$##')}"
    [ -z "$REPO" ] && { echo "!! Can't tell which GitHub repo to download from. Set REPO=owner/android-tv-debloat or use --build"; exit 1; }
    echo "== Downloading TvHome.apk from github.com/$REPO (latest release)"
    curl -fL -o "$APK" "https://github.com/$REPO/releases/latest/download/TvHome.apk" || { echo "!! download failed"; exit 1; };;
  *) cp "$1" "$APK";;
esac
echo "== APK: $(wc -c < "$APK" | tr -d ' ') bytes"
[ -f "$ROOT/state/stock-home.txt" ] || S cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 > "$ROOT/state/stock-home.txt"
echo "== Installing (if this hangs on Android 14, look at the TV for a confirmation)"
if ! A install -r "$APK"; then
  echo "   adb install failed; trying push + pm install"
  A push "$APK" /data/local/tmp/tvhome.apk >/dev/null && S pm install -r /data/local/tmp/tvhome.apk
fi
if ! S pm path $PKG | grep -q package:; then
  echo "!! The TV refused the launcher. If the error was INSTALL_FAILED_VERIFICATION_FAILURE or about HOME/signatures,"
  echo "   this TV can't use a custom launcher (examples/README.md, cases B/C). The debloat still stands."; exit 1
fi
S pm grant $PKG android.permission.ACCESS_COARSE_LOCATION 2>/dev/null   # lets the Wi-Fi pill show the network name
S logcat -c -b crash
echo "== Opening TvHome directly"
S am start -W -n $ACT | grep -E 'Status'; sleep 4
echo "   top: $(top)"; shot launcher-01-open
k KEYCODE_DPAD_RIGHT;            shot launcher-02-dock-right
k KEYCODE_DPAD_UP;               shot launcher-03-topbar
k KEYCODE_DPAD_LEFT; k KEYCODE_DPAD_LEFT; k KEYCODE_DPAD_LEFT
k KEYCODE_DPAD_CENTER 2;         shot launcher-04-inputs
k KEYCODE_BACK
k KEYCODE_DPAD_DOWN; k KEYCODE_DPAD_DOWN 1.6; shot launcher-05-grid
k KEYCODE_DPAD_UP 1.6;           shot launcher-06-back-to-dock
echo "   memory: $(S dumpsys meminfo $PKG | grep -m1 TOTAL)"
CR=$(S logcat -d -b crash | grep -c "$PKG"); echo "   crashes: $CR"
[ "$CR" != 0 ] && S logcat -d -b crash | grep -A20 "Process: $PKG" | head -30
echo "== Done. Tell your AI agent 'done'."
