# Android TV Debloat + TvHome

**Let an AI agent clean up your Android TV — safely, step by step — and give it a clean, Apple TV–style home screen.**

![Android TV](https://img.shields.io/badge/Android%20TV-tested%20%E2%9C%93-3DDC84?logo=androidtv&logoColor=white)
![Google TV](https://img.shields.io/badge/Google%20TV-should%20work-4285F4?logo=googletv&logoColor=white)
![Launcher](https://img.shields.io/badge/launcher-Android%209%2B-555)
![Not Tizen/webOS](https://img.shields.io/badge/Samsung%20Tizen%20%C2%B7%20LG%20webOS-not%20supported-lightgrey)

> ✅ **Works on Android TV** (TVs, boxes and sticks). Tested end to end on a real Android TV — Xiaomi Mi TV (MiTV-MSSP2, Android TV 9):
> 28 bloat packages disabled, ad-blocking DNS on, TvHome set as the home screen, and HDMI inputs, Live TV, the remote,
> Bluetooth, Chromecast and all streaming apps still working. Other Android TV / Google TV brands use the same system
> and should work the same way ([compatibility](docs/COMPATIBILITY.md)). Samsung (Tizen) and LG (webOS) TVs aren't Android,
> so they can't.

![Before and after](docs/img/before-after.png)

Smart TVs ship with tracking services, ad rows, auto-installers and apps you never use, all competing for about
1 GB of RAM. This repository is a **playbook for AI agents** (Claude, or any AI coding agent that can run a terminal)
plus the scripts and launcher they need. You paste one prompt; the agent backs up your TV, shows you what it found,
asks before touching anything personal, disables bloat in small verified batches, installs the launcher, and
leaves everything reversible.

> Nothing is uninstalled. Every change is recorded and `scripts/restore.sh` undoes it.

## What you get

- **Less tracking and fewer ads** — manufacturer analytics, recommendation rows, ad services and auto-installers off;
  optional ad-blocking DNS (not YouTube ads).
- **More free memory** — fewer background services, so apps resume instead of reloading.
- **TvHome** — a light launcher (~30–40 MB): big clock, your apps in a dock, the rest in a grid, a glass focus
  highlight, Inputs menu for HDMI/AV/Live TV, Wi-Fi name, optional **next match for your football club**.

| | |
|---|---|
| ![Home](docs/img/home.png) | ![Inputs](docs/img/inputs.png) |

## How to do it (about 30–60 minutes)

**You need:** a Mac, Linux PC (or Windows with WSL) on the **same Wi-Fi as the TV**, and an AI agent that can run
commands on it — e.g. [Claude Code](https://claude.com/claude-code), or the Claude desktop app with this folder connected.

1. **Get this repository** onto that computer:
   `git clone https://github.com/amirulmzlan/android-tv-debloat.git && cd android-tv-debloat`
   (or *Code → Download ZIP* above and unzip it).
2. **Install adb** — macOS: `brew install --cask android-platform-tools` · Debian/Ubuntu: `sudo apt install adb`.
3. **On the TV:** Settings → Device Preferences → About → press OK on **Build** 7 times. Then Developer options →
   turn on **USB debugging** (and **Network/ADB debugging** or **Wireless debugging** if listed). Note the TV's IP
   address (Settings → Network).
4. **Start your AI agent in this folder** (for Claude Code: run `claude` here) and paste the prompt from
   **[PROMPT.md](PROMPT.md)** — fill in your TV model.
5. Answer its questions (which apps you use, which go in the dock…) and accept the **"Allow USB debugging?"**
   prompt on the TV when it tells you to.

The agent follows **[AGENTS.md](AGENTS.md)**. If it runs in a sandbox that can't reach your TV, it will write
each step as a script for you to run instead ([docs/NO-LAN-ACCESS.md](docs/NO-LAN-ACCESS.md)) — slower, same result.

## Just the launcher (no AI)

Download **[TvHome.apk](https://github.com/amirulmzlan/android-tv-debloat/releases/latest/download/TvHome.apk)** from the [latest release](https://github.com/amirulmzlan/android-tv-debloat/releases/latest) (also in [`dist/`](dist/); SHA-256 in `dist/TvHome.apk.sha256`), then:
```
adb connect <tv-ip>:5555
adb install -r TvHome.apk
adb shell cmd package set-home-activity net.amirul.tvhome/.HomeActivity
```
Most Android TVs also need the stock launcher disabled before Home goes to TvHome (it outranks other launchers):
`adb shell pm disable-user --user 0 com.google.android.tvlauncher` — see [docs/LAUNCHER.md](docs/LAUNCHER.md)
for settings (dock apps, columns, football team, wallpaper) and the Google TV caveats in [AGENTS.md](AGENTS.md).

## Undo

```
./scripts/restore.sh            # re-enable everything, DNS back to automatic, stock home back
./scripts/restore.sh <package>  # just one app
adb uninstall net.amirul.tvhome # remove the launcher
```
Without a computer: Settings → Apps → See all apps → Show system apps → pick the app → **Enable**.

## Is it safe?

- The agent **backs up first**, never uninstalls system apps, and keeps everything the TV needs (TV inputs/HDMI,
  remote and Bluetooth pairing, keyboard, Play Store/Services). Unclear packages are checked or left alone.
- Changes go in **small batches with a screenshot after each**, so a problem is caught immediately and is easy to undo.
- At the end it **switches ADB off** — leaving it on lets devices on your network control the TV.
- Some TVs reject custom launchers (see [examples/README.md](examples/README.md)); then you still get the debloat.
- You're changing your own device's settings at your own risk; this isn't affiliated with Google, Xiaomi or any TV maker.

## Repository

| Path | What |
|---|---|
| [PROMPT.md](PROMPT.md) | The prompt to paste into your AI agent |
| [AGENTS.md](AGENTS.md) | The playbook the agent follows (rules, phases, gotchas) |
| `scripts/` | Backup, debloat-by-batch, DNS, launcher install/activate/config, restore, close ADB |
| `launcher/` | TvHome source (Java, Android 9+) |
| [docs/](docs/) | Compatibility, package reference, troubleshooting, launcher settings, remote TVs via Tailscale, sandboxed agents |
| [examples/](examples/) | Tested devices and what each needed — add yours! |

## Which TVs?

**Android TV and Google TV only** — TVs (Xiaomi, TCL, Sony, Philips, Hisense Android/Google TV models…) and
**boxes and sticks** (Xiaomi Mi Box S, Mi TV Stick, Nvidia Shield, Chromecast with Google TV…). **Samsung (Tizen), LG (webOS), Roku, VIDAA and Apple TV can't** — they aren't
Android. Full list, partial cases (older Android, China-market firmware, Fire TV) and alternatives:
**[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)**.

| Tested | Android | Result |
|---|---|---|
| Xiaomi Mi TV MiTV-MSSP2 (global) | 9 | Debloat (28 packages) + DNS + TvHome ✅ — [notes](examples/xiaomi-mitv-mssp2.md) |

Tried it on another TV? Open an issue or pull request with an `examples/` file — that's how the playbook gets better.

## License

Code: MIT ([LICENSE](LICENSE)). Font: Outfit, SIL Open Font License ([launcher/app/src/main/assets/fonts/OFL.txt](launcher/app/src/main/assets/fonts/OFL.txt)).
