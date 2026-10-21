<!--
DEVICE:     Xiaomi Mi TV, model MiTV-MSSP2, codename "volver", global firmware (ro.boot.region=sa), MediaTek
ANDROID:    9 (API 28), Android TV (not Google TV), stock home = com.google.android.tvlauncher
CASE:       A - sideload works, no HOME restriction; stock launcher has HOME priority 2 and must be disabled
SETUPWRAITH: owns the Wi-Fi saved during setup, but does NOT claim HOME here -> leave it enabled
RAM:        ~1 GB
RESULT:     28 packages disabled, ad-blocking DNS, TvHome as default home; HDMI/Live TV/remote all working
-->
# Xiaomi Mi TV MiTV-MSSP2 (Android 9, global)

Tested end to end. Use as a template for other Xiaomi/PatchWall TVs, but always confirm against the TV's own backup.

## Disabled (in this order of batches)

| Batch | Packages |
|---|---|
| Telemetry | `com.miui.tv.analytics`, `com.xiaomi.statistic`, `com.google.android.tv.bugreportsender`, `com.google.android.feedback` |
| Ads & rows | `com.mitv.tvhome.atv` (PatchWall), `com.mitv.tvhome.michannel` (Mi Channel row), `com.xm.webcontent`, `com.google.android.tvrecommendations`, `com.google.android.leanbacklauncher.recommendations`, `android.autoinstalls.config.xiaomi.amelie` (Play Auto Install) |
| Background | `com.android.printspooler`, `com.google.android.syncadapters.calendar`, `com.google.android.syncadapters.contacts` |
| Preloaded | `com.mitv.gallery` (incl. WallPaper), `com.xiaomi.mimusic2`, `com.xiaomi.mitv.mediaexplorer`, `com.mitv.dream`, `com.google.android.play.games`, `org.chromium.webview_shell`, `com.google.android.backdrop` |
| Person's choice | streaming apps they didn't use, `com.google.android.katniss` + `com.google.android.speech.pumpkin` (Assistant — remote mic stops working), `com.xiaomi.mitv.updateservice` |
| Launcher step | `com.google.android.tvlauncher` |

## Kept — needed by the TV

`com.mediatek.tvinput`, `com.mediatek.wwtv.tvcenter` (Live TV + HDMI), `com.mediatek.tvinputservice.arbitratorservice`,
`com.mediatek.tv.agent`, `mitv.service`, `com.xiaomi.mitv.tvmanager`, `com.android.providers.tv`,
`com.mediatek.hotkey.dispatcher`, `com.xiaomi.android.tvsetup.partnercustomizer` (**contains the Bluetooth remote
pairing screen** and a global key receiver — looks like bloat, isn't), `com.google.android.tv.remote.service`,
`com.mstar.netflixobserver` (Netflix button), `com.google.android.inputmethod.latin`, `com.android.vending`,
`com.google.android.gms`, `com.google.android.gsf`, `com.google.android.apps.mediashell` (Chromecast — if used),
`com.google.android.tungsten.setupwraith` (owns the Wi-Fi network).

## Notes

- The TV goes to standby on its own during long sessions; that looked like a crash once — `uptime` proved it wasn't.
- Load average sits around 100 on this MediaTek kernel at all times; it's not CPU load.
- HDMI and Live TV screenshots come back empty; `topResumedActivity` showed `com.mediatek.wwtv.tvcenter`.
- Inputs listed by the TV input framework: HDMI 1, HDMI 2 (ARC), HDMI 3, AV; switching via
  `TvContract.buildChannelUriForPassthroughInput` opens `com.mediatek.wwtv.tvcenter` correctly.
