# The prompt

Copy everything in the box below into Claude (Claude Code, or the Claude desktop app with this folder connected)
or into any AI coding agent that can run shell commands on a computer **on the same network as your TV**.
Fill in your TV model on the first line. The agent will read the rest of this repository before touching anything.

```text
I want to debloat my Android TV and replace its launcher with a clean Apple TV style home screen.
My TV model is: ____________ (e.g. Xiaomi Mi TV P1 43, TCL 50P725, Sony KD-43X80J).

Use this repository as your playbook: read AGENTS.md first (and docs/COMPATIBILITY.md to check my TV can do this), then docs/TROUBLESHOOTING.md and anything in
examples/ that matches my TV. Work on the real device, step by step, and verify everything you do with
screenshots pulled from the TV. Ask me before anything that depends on my preferences.

Connect
- Find the TV on my network (hostname, ARP table, or ask me for the IP) and connect over ADB on port 5555.
  If the TV is in another home, I'll give you its Tailscale address instead (docs/REMOTE.md): use that,
  don't scan my local network.
- If you can't reach my network at all (sandboxed or cloud agent), follow docs/NO-LAN-ACCESS.md: write each
  step as a script in this folder, I run it, you read its log and screenshots before the next step.
- Tell me when the "Allow USB debugging?" prompt will appear on the TV so I can accept it.

Back up first
- Run scripts/01-connect-and-backup.sh before changing anything. Every disabled package is recorded so
  scripts/restore.sh can re-enable it.
- Ask me what is plugged into the TV's USB ports (some TVs cut USB power while debugging is on) and remind
  me to switch debugging off at the end (scripts/close-adb.sh).

Debloat
- Sort the installed packages into: manufacturer telemetry/analytics, manufacturer content and ad services,
  background services I probably don't need, preloaded apps, and streaming apps. Show me the list.
- Never disable what the TV needs to work: TV/HDMI/tuner input services, the input menu, remote and
  Bluetooth pairing, the on-screen keyboard, Play Store and Play Services. If a package name doesn't say
  what it does, check its activities/services with dumpsys package first, and ask me if it's still unclear.
- Before touching streaming apps, voice assistant, Chromecast or the update service, ask me which ones I
  use and disable only what I don't use.
- Disable in small batches with scripts/02-debloat.sh and a list file per batch; check the screenshots
  between batches. Never uninstall system packages.
- Offer me ad-blocking DNS (scripts/03-adblock-dns.sh); tell me it doesn't block YouTube ads.

Launcher
- Check the TV accepts sideloaded apps first (examples/README.md lists TVs where the launcher half is
  impossible). If it's one of those, do the debloat half, say so plainly and stop retrying installs.
- Install the TvHome launcher from this repository's latest release (or build it from launcher/),
  set it as the default home, and disable the stock launcher only after it works (scripts/04, scripts/05).
- Ask me which apps go in the dock, how many columns (4 or 6), and whether I want a football club's next
  match on the home screen; write that to home.conf with scripts/06-config.sh.

Verify
- After each change, inject remote key presses over ADB, pull a screenshot from the TV and look at it.
  Fix what you see before moving on. Live TV and HDMI screenshots come back empty - that's normal.

Finish
- Give me a short summary of what was disabled, how to restore it, and close network ADB
  (scripts/close-adb.sh).
```
