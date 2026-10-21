# Package reference for agents

A starting point for classifying packages — **not a list to apply blindly**. Always confirm against the TV's own
backup (`backup/<date>/pkg/<package>.txt` shows its activities/services) and ask the person when unsure.

Legend: ✅ verified on a real TV in this repo · ⚠️ common knowledge / reported — verify on the TV first.

## Never disable (any Android TV)

| Package / pattern | Why |
|---|---|
| `com.android.vending`, `com.google.android.gms`, `com.google.android.gsf` | Play Store / Play Services — apps break without them |
| `com.android.tv.settings`, `com.android.settings`, `com.android.systemui`, `com.android.providers.*`, `android` | Core system |
| `com.android.providers.tv` | TV channel/program database used by the TV input framework |
| Packages with **`tvinput`**, **`tvcenter`**, **`livetv`**, **`hdmi`**, **`cec`**, **`tuner`**, **`dtv`** in the name | TV inputs, HDMI, tuner, Live TV |
| `com.google.android.tv.remote.service` | Google TV Remote / remote control service |
| `com.google.android.leanback.ime`, `com.google.android.inputmethod.latin`, any `*ime*`/`*keyboard*` in use | On-screen keyboard (check `settings get secure default_input_method`) |
| Anything with Bluetooth **remote pairing** activities (`*Rc*`, `*Remote*`, `*Pair*` in its activity names) | Remote pairing — e.g. Xiaomi hides it in `com.xiaomi.android.tvsetup.partnercustomizer` ✅ |
| `com.google.android.tungsten.setupwraith` | Setup wizard — may own the Wi-Fi network saved during setup (see AGENTS.md) |

## Google Android TV — common to most brands

| Package | What it is | Usual call |
|---|---|---|
| `com.google.android.tvrecommendations` ✅ | Recommendation rows | Disable |
| `com.google.android.leanbacklauncher.recommendations` ✅ | Recommendations for the old Leanback launcher | Disable |
| `com.google.android.feedback` ✅, `com.google.android.tv.bugreportsender` ✅ | Feedback / bug-report senders | Disable |
| `android.autoinstalls.config.<maker>` ✅ | Play Auto Install — pushes apps onto the TV | Disable |
| `com.google.android.backdrop` ✅ | Ambient mode screensaver | **Keep if it's the active screensaver** — see below |
| `com.google.android.play.games` ✅, `com.google.android.videos` ✅, `com.google.android.youtube.tvmusic` ✅ | Play Games, Google TV/Play Movies, YouTube Music | **Ask** |
| `com.google.android.katniss` ✅, `com.google.android.speech.pumpkin` ✅ | Google Assistant / voice search — remote mic stops working | **Ask** |
| `com.google.android.apps.mediashell` ✅ | Chromecast built-in | **Ask** |
| `com.google.android.syncadapters.calendar` / `.contacts` ✅, `com.android.printspooler` ✅ | Sync / printing, unused on a TV | Disable |
| `com.google.android.tvlauncher` ✅ | Stock Android TV home — disable only after TvHome works | Launcher step |

### ⚠️ Screensavers: check before disabling

Run `settings get secure screensaver_components`. If it names the package you're about to disable (often
`com.google.android.backdrop` = Ambient Mode, or the maker's screensaver such as Xiaomi's `com.mitv.dream`), **don't
disable it** — or set another screensaver first. With no screensaver left, Android goes straight to sleep after
`screen_off_timeout` (often 5 minutes), so the TV seems to switch itself off whenever it's idle. ✅ Hit on the tested Xiaomi.

## Google TV

| Package | What it is | Usual call |
|---|---|---|
| `com.google.android.apps.tv.launcherx` ⚠️ | Google TV home screen — disable only after TvHome works | Launcher step |
| `com.google.android.tungsten.setupwraith` ⚠️ | Setup wizard; may claim HOME and own the Wi-Fi network | See AGENTS.md before touching |

## Xiaomi (PatchWall) ✅

See [examples/xiaomi-mitv-mssp2.md](../examples/xiaomi-mitv-mssp2.md) — telemetry `com.miui.tv.analytics`,
`com.xiaomi.statistic`; ads/content `com.mitv.tvhome.atv`, `com.mitv.tvhome.michannel`, `com.xm.webcontent`;
update service `com.xiaomi.mitv.updateservice` (ask). Keep `com.xiaomi.mitv.tvmanager`, `mitv.service`,
`com.xiaomi.android.tvsetup.partnercustomizer`, `com.mstar.netflixobserver` (Netflix button).

## Other makers — how to tell

No verified lists yet (add yours via `examples/`). Things that held up so far:

- **Usually safe to disable**, once you've read the package's activities/services: names with `analytics`, `statistic`,
  `stat`, `report`, `feedback`, `bugreport`, `tracker`, `ad`/`ads`, `promo`, `recommend`, `channel` (content rows),
  `autoinstall`, `appstore`/`market` (maker's own store), `cleaner`/`booster`, `demo`/`retail`/`shop` mode.
- **Leave alone** unless you've confirmed it: anything with `tv`, `input`, `hdmi`, `cec`, `tuner`, `dtv`, `remote`,
  `rc`, `bluetooth`, `audio`, `picture`/`pq`, `panel`, `factory`, `setup`, `ota`/`update` (ask), `keystone`/`hotkey`
  (remote buttons). A maker's "TV manager" or "system service" is almost always load-bearing.
- **Sony:** `com.sony.dtv.tvx` ⚠️ is the TV/Live TV app — keep.
- **Helper apps in the launcher grid:** built-in apps without a TV entry are hidden automatically. If a maker's helper
  still shows up as a tile, add it to `hidden=` with `scripts/06-config.sh --hidden <package>`.
