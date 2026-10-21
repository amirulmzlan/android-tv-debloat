# Which TVs can do this?

The short version: **if your TV (or box) runs Android TV or Google TV, yes.** If it runs anything else — Samsung
**Tizen**, LG **webOS**, **Roku**, **VIDAA**, Vizio SmartCast, Apple TV — **no**: those aren't Android, there's no ADB,
no packages to disable and no way to install an Android launcher.

How to check: Settings → About (or Device Preferences → About). If it shows an **Android version**, or the home screen
says **Google TV**, you're good.

## ✅ Tested end to end

| Device | Android | Result |
|---|---|---|
| Xiaomi Mi TV MiTV-MSSP2 (global firmware) | 9 | 28 packages disabled, ad-blocking DNS, TvHome as home — [notes](../examples/xiaomi-mitv-mssp2.md) |

## 🟢 Should work — not tested by us yet (Android TV / Google TV, Android 9 or newer)

These run standard Android TV or Google TV with ADB. Debloat works the same way; TvHome needs the stock launcher
disabled. Please report back with an `examples/` file.

- **Xiaomi** global-market Mi TV / Redmi TV / Mi Box S (Android TV, PatchWall)
- **TCL** Android TV and Google TV models
- **Sony Bravia** Android TV / Google TV
- **Philips**, **Sharp**, **Toshiba** (Android TV versions), **Hisense Android TV / Google TV** models (not VIDAA ones)
- Android TV **boxes and sticks** — see the next section
- **Google TV caveat:** the setup wizard can take over Home and owns the Wi-Fi network saved during setup — the agent
  handles this (case D in [examples/README.md](../examples/README.md)), but expect one extra step.

## 📦 Android TV boxes and sticks

A box or stick runs the same Android TV / Google TV as a smart TV, so **the debloat and TvHome work the same way** —
it just has no HDMI inputs or tuner of its own (it *is* the HDMI input), so TvHome hides the **Inputs** button there.
Low-RAM sticks (about 1 GB) benefit the most. Not tested by us yet — please report back.

| Device | System | Debloat | TvHome |
|---|---|---|---|
| **Xiaomi Mi Box S** (1st gen) | Android TV 9 (official update) | 🟢 should work | 🟢 should work |
| **Xiaomi Mi Box** (2016) | Android TV 9 (official update) | 🟢 should work | 🟢 should work |
| **Xiaomi Mi TV Stick** / **Mi TV Stick 4K** | Android TV 9 / newer | 🟢 should work | 🟢 should work |
| **Xiaomi Mi Box S 2nd gen** | Google TV | 🟢 should work | 🟢 should work (Google TV setup-wizard step, case D) |
| **Nvidia Shield TV**, **Chromecast with Google TV**, **Google TV Streamer**, onn. boxes | Android TV / Google TV | 🟢 should work | 🟢 should work |
| No-brand boxes (X96, H96, T95…) | Often plain Android, not certified Android TV | 🟡 works via ADB | 🟡 if Android 9+; may have their own launcher or no Play Store |

Check Settings → About for the Android version first: TvHome needs **Android 9 or newer** (most boxes have had the update).
Some no-brand boxes have been reported to ship with unwanted preinstalled apps; disabling helps but can't guarantee a box is clean.

## 🟡 Partly — debloat yes, launcher no (or not reliably)

| Device | Why |
|---|---|
| Android TV **older than Android 9** | TvHome needs Android 9+ (debloat still works on 4.3+; 4.2 needs the Settings-tapping workaround in TROUBLESHOOTING) |
| **China-market** Xiaomi/other sets with locked firmware | Some reject every sideloaded APK, or any home-screen app not signed by the maker (cases B/C) — reported, varies by firmware |
| **Amazon Fire TV with Fire OS** (older sticks, Lite, 4K, 4K Max, Cube — Fire OS is Android) | ADB works and some apps can be disabled (Amazon blocks many of its own), but Amazon actively blocks replacement home screens; workarounds break with updates. Not supported here |

## ❌ Can't — not Android

| Platform | Brands | What you *can* do instead |
|---|---|---|
| **Tizen** | Samsung | Developer Mode only lets developers install their own Tizen web apps from Tizen Studio — it can't remove preinstalled apps or replace the home screen. In the TV's own Settings, turn off viewing-information / interest-based-ad and voice-data options in the privacy section, and use an ad-blocking DNS on your router. Or plug in a Google TV / Android TV box (see 🟢) and use that as your TV's home screen. |
| **webOS** | LG | Same: privacy toggles in Settings, router DNS, or an Android TV box |
| **Vega OS** | New Amazon Fire TV sticks (Fire TV Stick 4K Select onwards) | Not Android and no sideloading at all (confirmed by Amazon). Use a Google TV / Android TV stick instead |
| **Roku OS**, **VIDAA**, **SmartCast**, **tvOS** | Roku TVs, Hisense/Toshiba VIDAA, Vizio, Apple TV | Same |

> An Android TV / Google TV box on HDMI is the simplest way to get all of this on a Samsung, LG or any other TV:
> set the TV to start on that HDMI input and you'll mostly never see the TV's own home screen.
