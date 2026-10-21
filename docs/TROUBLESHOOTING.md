# Troubleshooting

Problems hit in real runs, and what fixed them.

## Connecting

**Port 5555 closed.** Turn on Developer options (Settings → Device Preferences → About → press OK on *Build* 7 times),
then *USB debugging*, and *Network/ADB debugging* if listed. Google TV sets may only offer **Wireless debugging**:
find it with `adb mdns services` (or `dns-sd -B _adb-tls-connect._tcp`), choose *Pair device with pairing code* on the
TV, then `adb pair <ip>:<pair-port>`, `adb connect <ip>:<port>`, and `adb tcpip 5555` for a stable port.
Android turns Wireless debugging off whenever Wi-Fi drops.

**macOS: "No route to host" although the port is open.** That's macOS *Local Network* privacy blocking the adb
binary. Allow your terminal app in System Settings → Privacy & Security → Local Network. If that's not possible,
use the Python `adb-shell` package in a venv as the ADB client; if its file push hangs on big files, send files
as base64 chunks through the shell and check the md5.

**`adb: device offline` / stale connection** after the TV restarts adbd: `adb disconnect; adb kill-server`, wait a
few seconds, `adb connect <ip>:5555` again.

**"Allow USB debugging?"** appears on the TV the first time a computer connects. Tick *Always allow from this
computer* and press OK. Tell the person *before* you connect.

## Debloating

**`pm disable-user` says "Killed" / "Permission Denial"** — Android 4.2 and older: the ADB shell isn't allowed to
change component state (granted from 4.3). Root ADB is usually locked. Workaround that works: open
`android.settings.APPLICATION_DETAILS_SETTINGS` for the package, find the *Disable* button with
`uiautomator dump`, tap it, confirm *OK*, verify with `pm list packages -d`. For built-in apps that have Play
Store updates, `adb uninstall <pkg>` first (removes only the updates), then Disable.

**Something stopped working after a batch.** `scripts/restore.sh <pkg>` re-enables one package; bisect the
last batch. The TV going into standby during a batch is not a crash — check `uptime` with `scripts/status.sh`.

**Bluetooth dropped after a batch.** Usually the phone, not the batch: reconnect first, then bisect if it repeats.

## Launcher

**Home still opens the old launcher after `set-home-activity` "Success".** The stock launcher has a higher
HOME priority; disable it (`scripts/05-activate-launcher.sh`). Android ignores the default-home preference while a
higher-priority HOME app exists.

**App grid is empty.** Android 11+ package visibility; TvHome already declares `<queries>`. If you build your own,
keep them.

**Wi-Fi pill shows "Wi-Fi" instead of the network name.** Android 9+ needs location permission for the SSID;
the install script runs `pm grant net.amirul.tvhome android.permission.ACCESS_COARSE_LOCATION`.

## Verifying

**Live TV / HDMI screenshots are 0 bytes or black.** Protected video surfaces — that's the TV, not a broken debloat.
Check `dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'` instead.

**Remote buttons that rely on an accessibility helper** (e.g. a remote-mapper app): check the service is enabled
and bound for user 0 (`dumpsys accessibility`), then ask the person to press the real button — an injected key
event doesn't prove the physical button works.

## Time and certificates

**Clock shows a date years in the past** (e.g. 2007): the GPS chip's week-number rollover, or a dead clock battery.
Secure connections fail with a wrong date (Play Store, YouTube, Google sign-in). Set the date by hand and, if the
device syncs time from GPS, turn that off.

**Google sign-in fails with "problem communicating with Google servers"** on very old Android and the log shows
`GLS error: MissingDroidguard`: Google now requires a device-integrity check that Play Services on Android 4.x
can no longer provide. Not fixable without root; apps that don't need a Google account still work.
