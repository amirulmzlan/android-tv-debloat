# TvHome launcher

A small native Android TV home screen (Java, AndroidX RecyclerView, minSdk 28 / Android 9+), ~30-40 MB RAM.
No blur, no video, no polling: the clock and Wi-Fi name update from system broadcasts; the optional football card
fetches once every 3 hours at most, only while the home screen is showing.

- Black background; greeting, large thin clock and date (Outfit font, SIL OFL).
- Frosted pills: **Inputs** (dropdown of HDMI/AV via the TV input framework, plus **Live TV** when the TV has a Live TV app —
  found automatically: MediaTek, Sony, Google Live Channels, AOSP, or whatever handles "view channels"), **Wi-Fi** (network name), **Settings**.
- A dock band of your apps; the focused one's name appears above it with a glow in the app's banner colour.
- All other apps in a grid below (built-in helper apps without a TV entry are left out automatically); **Down** scrolls it up. One glass highlight springs between tiles.
- Deterministic D-pad: Up/Down move exactly one row; Up from the dock goes to the top bar; ends don't fall through;
  Back returns to the dock; Menu opens App info; the last focused tile is remembered.
- Optional **next match** card for a football club (ESPN public schedule, all competitions; hides itself if the feed fails).

## Settings without rebuilding: `home.conf`

Write a text file and push it (or use `scripts/06-config.sh`, which asks and does this for you):

```
columns=6                       # 4-6 look best; dock holds the same number of apps
dock=com.google.android.youtube.tv,com.netflix.ninja,com.amazon.amazonvideo.livingroom,com.disney.disneyplus
hidden=com.example.helperapp    # apps to keep out of the grid
team_id=364                     # ESPN team id (364 Liverpool, 359 Arsenal, 360 Man United, 363 Chelsea, 382 Man City, 367 Tottenham)
team_name=Liverpool FC
```
```
adb push home.conf /sdcard/Android/data/net.amirul.tvhome/files/home.conf
adb shell am force-stop net.amirul.tvhome && adb shell input keyevent KEYCODE_HOME
```

**Wallpaper:** push a JPEG to `/sdcard/Android/data/net.amirul.tvhome/files/wallpaper.jpg` (shown dimmed).

## Build it yourself

Android SDK (platform 33, build-tools 36) + JDK 17: `./scripts/04-install-launcher.sh --build`
(creates a Gradle 8.7 wrapper if needed, then `./gradlew assembleDebug`).
