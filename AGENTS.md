# AGENTS.md — playbook for AI agents

You are helping a person debloat their Android TV and (optionally) replace its home screen with the TvHome
launcher in `launcher/`. The person may not be technical. Work on the real TV, one small step at a time,
and prove every step with a screenshot pulled from the TV. **Nothing you do should be irreversible.**

## Ground rules

0. **Check the TV is Android TV / Google TV first** ([docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)). Samsung Tizen, LG webOS,
   Roku, VIDAA etc. can't be done: tell the person plainly and suggest the alternatives listed there.

1. **Back up before changing anything** (`scripts/01-connect-and-backup.sh`). Read the backup before planning.
2. **Never uninstall system packages.** Disable with `pm disable-user --user 0` (via `scripts/02-debloat.sh`),
   which records every package in `state/disabled.txt` so `scripts/restore.sh` can undo it.
   Apps the person installed themselves may be uninstalled only if they ask (`scripts/uninstall.sh`).
3. **Ask before anything that depends on preference**: which streaming apps, voice assistant, Chromecast,
   update service, dock apps, columns, football team. Use a checklist question; disable only what they don't use.
4. **Never disable what the TV needs to work**: TV input framework / HDMI / tuner (`*tvinput*`, `*tvcenter*`,
   `*tv.settings*`, vendor TV services), the input/source menu, remote and Bluetooth pairing, the keyboard,
   `com.android.vending`, `com.google.android.gms`, `com.google.android.gsf`, SystemUI, Settings, providers.
   Manufacturer names are often meaningless: run `dumpsys package <pkg>` and look at its activities, services
   and receivers. If it's still unclear, ask the person, or leave it alone.
5. **Small batches** (3-8 packages), one `pm` call per shell call, then Home + screenshot + Settings check.
   A batch can take something load-bearing with it; small batches tell you which.
6. **Verify, don't assume.** After every change: `input keyevent`, `screencap`, look at the image,
   `dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'`. Fix what you see first.
7. **Close ADB at the end** (`scripts/close-adb.sh`): port 5555 is plaintext, and on some firmware
   (`ro.adb.secure=0`) anyone on the network gets a shell.
8. **Talk plainly.** Tell the person when to look at the TV (the "Allow USB debugging?" prompt), what changed,
   and what's next. Don't dump logs on them.

## Phases

| # | Phase | Script | Notes |
|---|---|---|---|
| 0 | Connect | `01-connect-and-backup.sh <ip>` | Finds the TV if no IP is given; saves `.tv_ip` |
| 1 | Backup | (same) | packages, disabled list, getprop, settings, per-package dumps, screenshot |
| 2 | Plan | — | Classify packages (see below). Show the plan. Ask preference questions. |
| 3 | Debloat | `02-debloat.sh lists/<batch>.txt` | One list file per batch, written by you after approval |
| 4 | DNS (optional) | `03-adblock-dns.sh` / `off` | Private DNS → AdGuard; not YouTube ads |
| 5 | Launcher | `04-install-launcher.sh` | Downloads the release APK (or `--build`), installs, opens it, tests keys |
| 6 | Activate | `05-activate-launcher.sh` | Lists HOME apps, handles the stock launcher, sets default home, verifies |
| 7 | Personalise | `06-config.sh` | Writes `home.conf` (dock, columns, hidden apps, football team). Look at the grid screenshot: any maker helper apps showing as tiles go in `--hidden` |
| 8 | Finish | `close-adb.sh` | Summary for the person; restore instructions |

Helpers: `status.sh` (reachable? top app? Wi-Fi? crashes?), `restore.sh [pkg]`, `uninstall.sh <pkg...>`.

## Classifying packages

Sort into: **manufacturer telemetry/analytics** · **manufacturer content & ad services** (custom home screens,
recommendation rows, content providers, auto-installers like `android.autoinstalls.config.*`) ·
**background services they likely don't use** (print spooler, calendar/contacts sync, bug report/feedback) ·
**preloaded apps** (gallery, music, file browser, screensaver, games) · **streaming apps** (ask!) ·
**keep** (everything in rule 4). Check `examples/` for a device that matches — reuse its classification,
but still confirm against this TV's backup. [docs/PACKAGES.md](docs/PACKAGES.md) lists packages
that are common to most Android TVs and name patterns that are usually safe / usually load-bearing.

## Launcher gotchas (read before phase 5-6)

- **Sideload check.** Some China-market sets reject every APK (`INSTALL_FAILED_VERIFICATION_FAILURE`), and
  others refuse any app declaring `CATEGORY_HOME` unless it carries the manufacturer's signature. If you hit
  either: finish the debloat, tell the person plainly the launcher can't be installed on this TV, stop retrying.
- **The stock launcher outranks you.** `com.google.android.tvlauncher` declares priority 2 for HOME; Android
  ignores the "default home" preference when a higher-priority HOME app exists. It must be **disabled**
  (after you've opened TvHome directly and confirmed it works). Google TV: `com.google.android.apps.tv.launcherx`.
- **Google TV setup wizard** (`com.google.android.tungsten.setupwraith`) sometimes also claims HOME. Before
  disabling it, run `dumpsys wifi | grep -o 'cname=[^ ]*'`: if it owns the saved Wi-Fi network, Android deletes
  that network when it's disabled and the TV drops off Wi-Fi (and ADB). Ask the person to forget and re-join the
  network in Settings first. If it doesn't claim HOME, leave it enabled.
- **Android 11+ package visibility**: TvHome declares `<queries>` for LEANBACK_LAUNCHER/LAUNCHER already.
- **Android 14** may wait for an on-screen confirmation during `adb install`; push the APK and `pm install -r`.

## When you can't reach the TV yourself

If your shell can't reach the person's LAN (cloud sandbox, VM, proxy-only network), follow
`docs/NO-LAN-ACCESS.md`: write each step as a script in this repo, have the person run it in Terminal, then read
the log/screenshots it saved into `state/` and `shots/` before the next step. Same phases, same checks.

## Troubleshooting

See `docs/TROUBLESHOOTING.md` — macOS "No route to host", Wireless debugging pairing, 0-byte HDMI screenshots,
"device offline" after reconnects, Android 4.x quirks, and more.
