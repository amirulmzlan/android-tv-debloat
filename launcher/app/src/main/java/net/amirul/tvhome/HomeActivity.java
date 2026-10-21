package net.amirul.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import static net.amirul.tvhome.Ui.dp;
import static net.amirul.tvhome.Ui.withAlpha;

/**
 * Apple-TV-style home screen. Focus is handled entirely here (no Android focus search),
 * so D-pad movement is deterministic: TOP bar, DOCK row, GRID rows, and the Inputs MENU.
 */
public class HomeActivity extends Activity {
    static final int TOP = 0, DOCK = 1, GRID = 2, MENU = 3;
    int COLS = 6;                       // grid columns = dock size (home.conf: columns=)
    Config conf;
    /** Live TV apps by maker, tried in order; otherwise whatever app handles "view TV channels". */
    static final String[] LIVE_TV_APPS = {
            "com.mediatek.wwtv.tvcenter",   // MediaTek-based sets (Xiaomi, many TCL/Philips/Sharp)
            "com.sony.dtv.tvx",             // Sony Bravia
            "com.google.android.tv",        // Google "Live Channels"
            "com.android.tv"};              // AOSP Live TV
    /** System helpers that expose a launcher icon but aren't apps you'd open. */
    static final java.util.Set<String> HIDDEN = new java.util.HashSet<>(Arrays.asList(
            "com.mstar.netflixobserver"));

    int W, H, pad, gap, tileW, tileH, bandH, heroH;
    FrameLayout root;
    ImageView wall;
    long wallStamp = -1;
    PageLayout page;
    TextView greeting, clock, date, hero;
    GradientDrawable glowShape;
    View glow;
    int glowColor = 0xFF8E8E93;
    ValueAnimator glowAnim, hlAnim;
    final View[] pills = new View[3];
    TextView wifiText;
    // football fixtures card
    LinearLayout matchCard;
    TextView mLabel, mTitle, mComp, mWhen, mCountdown;
    List<Fixtures.Match> matches = new ArrayList<>();
    String teamId;
    long lastFetch;
    boolean fetching;
    LinearLayout dockRow;
    RecyclerView grid;
    GridLayoutManager glm;
    final AppAdapter adapter = new AppAdapter();
    HighlightView hl;
    LinearLayout menu;
    final List<View> menuRows = new ArrayList<>();
    final List<Runnable> menuActions = new ArrayList<>();

    final List<Ui.AppEntry> dockApps = new ArrayList<>();
    final List<Ui.AppEntry> gridApps = new ArrayList<>();
    final List<TileView> dockTiles = new ArrayList<>();
    TileView focusedTile;

    int region = DOCK, topIdx = 0, dockIdx = 0, gridIdx = 0, menuIdx = 0, minTop = 0;
    String restoreGridPkg;
    boolean loaded, restored;
    SharedPreferences prefs;
    Ui.Fonts fonts;
    final Handler ui = new Handler(Looper.getMainLooper());

    // ---------------------------------------------------------------- lifecycle

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        Ui.density = dm.density;
        W = dm.widthPixels; H = dm.heightPixels;
        conf = Config.load(this);
        COLS = conf.columns;
        pad = dp(64); gap = dp(COLS <= 4 ? 28 : 22);
        tileW = (W - 2 * pad - (COLS - 1) * gap) / COLS;
        tileH = Math.round(tileW * 9f / 16f);
        int bandPad = dp(20), bandBottom = dp(36);
        bandH = tileH + 2 * bandPad;
        heroH = H - bandH - bandBottom;

        prefs = getSharedPreferences("home", MODE_PRIVATE);
        fonts = new Ui.Fonts(this);
        region = prefs.getInt("region", DOCK);
        if (region == MENU) region = TOP;
        topIdx = prefs.getInt("top", 0);
        dockIdx = prefs.getInt("dock", 0);
        restoreGridPkg = prefs.getString("gridPkg", null);
        teamId = conf.teamId;
        lastFetch = prefs.getLong("fixFetched", 0);
        String cached = prefs.getString("fixJson", null);
        if (cached != null && !teamId.isEmpty()) matches = Fixtures.parse(cached, teamId);

        buildUi(bandBottom);
        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addDataScheme("package");
        registerReceiver(pkgReceiver, pf);
        loadApps();
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        f.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(sysReceiver, f);
        tick();
        updateWifi();
        loadWallpaper();
        refreshMatchCard();
        maybeFetchFixtures();
        updateInputsPill();
        if (loaded) afterLayout(() -> applyFocus(false));
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(sysReceiver); } catch (Exception ignored) {}
        saveFocus();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(pkgReceiver); } catch (Exception ignored) {}
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home: back to the dock.
        if (menu.getVisibility() == View.VISIBLE) menu.setVisibility(View.GONE);
        if (loaded && !dockApps.isEmpty()) { region = DOCK; applyFocus(true); }
    }

    @Override public void onBackPressed() { /* launcher: never finish */ }

    final BroadcastReceiver sysReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            if (a.startsWith("android.intent.action.TIME")) tick(); else updateWifi();
        }
    };

    final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { loadApps(); }
    };

    // ---------------------------------------------------------------- layout

    void buildUi(int bandBottom) {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setClipChildren(false);

        wall = new ImageView(this);
        wall.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wall.setAlpha(0.55f);
        root.addView(wall, new FrameLayout.LayoutParams(-1, -1));

        page = new PageLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipChildren(false);
        page.setClipToPadding(false);
        root.addView(page, new FrameLayout.LayoutParams(-1, -2));

        // --- hero section: clock + pills + focused dock app name with ambient glow
        FrameLayout heroSec = new FrameLayout(this);
        heroSec.setClipChildren(false);
        page.addView(heroSec, new LinearLayout.LayoutParams(-1, heroH));

        glowShape = new GradientDrawable();
        glowShape.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        // radius < half the view's height so the gradient fades out fully inside its bounds (no hard edge)
        glowShape.setGradientRadius(dp(340));
        setGlow(glowColor);
        glow = new View(this);
        glow.setBackground(glowShape);
        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(dp(1100), dp(720), Gravity.TOP | Gravity.START);
        glp.leftMargin = pad + dp(170) - dp(550);
        glp.topMargin = heroH - dp(60) - dp(360);
        heroSec.addView(glow, glp);

        hero = text(200, 58, 0xF2FFFFFF);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        hp.leftMargin = pad;
        hp.bottomMargin = dp(16);
        heroSec.addView(hero, hp);

        LinearLayout tl = new LinearLayout(this);
        tl.setOrientation(LinearLayout.VERTICAL);
        greeting = text(300, 22, 0xFF8E8E93);
        clock = text(150, 86, 0xFFFFFFFF);
        clock.setIncludeFontPadding(false);
        clock.setLetterSpacing(-0.01f);
        date = text(300, 21, 0xFFAEAEB2);
        tl.addView(greeting);
        tl.addView(clock);
        tl.addView(date);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        tlp.leftMargin = pad;
        tlp.topMargin = dp(40);
        heroSec.addView(tl, tlp);

        LinearLayout pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pills[0] = pill(R.drawable.ic_input, "Inputs", null);
        TextView[] wt = new TextView[1];
        pills[1] = pill(R.drawable.ic_wifi, "Wi-Fi", wt);
        wifiText = wt[0];
        pills[2] = pill(R.drawable.ic_settings, "Settings", null);
        for (int i = 0; i < 3; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(42));
            if (i > 0) lp.leftMargin = dp(12);
            pillRow.addView(pills[i], lp);
        }
        FrameLayout.LayoutParams prp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        prp.rightMargin = pad;
        prp.topMargin = dp(52);
        heroSec.addView(pillRow, prp);

        // --- football card (top right, under the pills): next match, date/time, countdown
        matchCard = new LinearLayout(this);
        matchCard.setOrientation(LinearLayout.VERTICAL);
        matchCard.setPadding(dp(22), dp(16), dp(22), dp(18));
        GradientDrawable mcBg = new GradientDrawable();
        mcBg.setColor(0x14FFFFFF);
        mcBg.setCornerRadius(dp(22));
        mcBg.setStroke(dp(1), 0x26FFFFFF);
        matchCard.setBackground(mcBg);
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(getColor(R.color.fixture_accent));
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(9), dp(9));
        dlp.rightMargin = dp(9);
        labelRow.addView(dot, dlp);
        mLabel = text(500, 13, 0xFFAEAEB2);
        mLabel.setLetterSpacing(0.08f);
        mLabel.setText(conf.teamName.isEmpty() ? "NEXT MATCH" : conf.teamName.toUpperCase() + "  \u00B7  NEXT MATCH");
        labelRow.addView(mLabel);
        matchCard.addView(labelRow);
        mTitle = text(300, 30, 0xFFFFFFFF);
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(-2, -2);
        tlp2.topMargin = dp(8);
        matchCard.addView(mTitle, tlp2);
        mComp = text(300, 16, 0xFFAEAEB2);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
        clp.topMargin = dp(4);
        matchCard.addView(mComp, clp);
        LinearLayout whenRow = new LinearLayout(this);
        whenRow.setOrientation(LinearLayout.HORIZONTAL);
        whenRow.setGravity(Gravity.BOTTOM);
        mWhen = text(400, 21, 0xFFFFFFFF);
        mCountdown = text(300, 16, 0xFF8E8E93);
        whenRow.addView(mWhen);
        LinearLayout.LayoutParams cdp = new LinearLayout.LayoutParams(-2, -2);
        cdp.leftMargin = dp(12);
        whenRow.addView(mCountdown, cdp);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-2, -2);
        wlp.topMargin = dp(12);
        matchCard.addView(whenRow, wlp);
        matchCard.setVisibility(View.GONE);
        FrameLayout.LayoutParams mcp = new FrameLayout.LayoutParams(dp(380), -2, Gravity.TOP | Gravity.END);
        mcp.rightMargin = pad;
        mcp.topMargin = dp(52) + dp(42) + dp(26);
        heroSec.addView(matchCard, mcp);

        // --- dock band
        FrameLayout band = new FrameLayout(this);
        GradientDrawable bandBg = new GradientDrawable();
        bandBg.setColor(0xFF141416);
        bandBg.setCornerRadius(dp(30));
        bandBg.setStroke(dp(1), 0x0FFFFFFF);
        band.setBackground(bandBg);
        band.setClipChildren(false);
        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER_VERTICAL);
        dockRow.setPadding(pad - dp(40), 0, 0, 0);
        dockRow.setClipChildren(false);
        dockRow.setClipToPadding(false);   // the first tile scales into the row's left padding
        band.addView(dockRow, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, bandH);
        blp.leftMargin = blp.rightMargin = dp(40);
        blp.bottomMargin = bandBottom;
        page.addView(band, blp);

        // --- app grid (all rows laid out; the page itself translates up/down)
        grid = new RecyclerView(this);
        glm = new GridLayoutManager(this, COLS) {
            @Override public boolean canScrollVertically() { return false; }
        };
        grid.setLayoutManager(glm);
        grid.setItemAnimator(null);
        grid.setNestedScrollingEnabled(false);
        grid.setFocusable(false);
        grid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setPadding(pad - gap / 2, 0, pad - gap / 2, dp(90));
        grid.setAdapter(adapter);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.topMargin = dp(14);
        page.addView(grid, gp);

        // --- overlays: inputs menu, then the glass highlight on top of everything
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable mb = new GradientDrawable();
        mb.setColor(0xF01C1C1F);
        mb.setCornerRadius(dp(20));
        mb.setStroke(dp(1), 0x33FFFFFF);
        menu.setBackground(mb);
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        menu.setVisibility(View.GONE);
        root.addView(menu, new FrameLayout.LayoutParams(dp(260), -2));

        hl = new HighlightView(this);
        root.addView(hl, new FrameLayout.LayoutParams(-1, -1));

        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        setContentView(root);
        root.requestFocus();
    }

    TextView text(int weight, float sp, int color) {
        TextView t = new TextView(this);
        t.setTypeface(fonts.get(weight));
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setSingleLine(true);
        return t;
    }

    View pill(int icon, String label, TextView[] out) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.HORIZONTAL);
        p.setGravity(Gravity.CENTER_VERTICAL);
        p.setPadding(dp(16), 0, dp(18), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x1AFFFFFF);
        bg.setCornerRadius(dp(21));
        bg.setStroke(dp(1), 0x26FFFFFF);
        p.setBackground(bg);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(0xFFE5E5EA);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(19), dp(19));
        ip.rightMargin = dp(8);
        p.addView(iv, ip);
        TextView t = text(400, 16, 0xFFE5E5EA);
        t.setText(label);
        t.setMaxWidth(dp(220));
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        p.addView(t);
        if (out != null) out[0] = t;
        return p;
    }

    /** Vertical container measured with unlimited height so the grid lays out every row. */
    static final class PageLayout extends LinearLayout {
        PageLayout(Context c) { super(c); }
        @Override protected void onMeasure(int w, int h) {
            super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        }
    }

    // ---------------------------------------------------------------- apps

    void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            LinkedHashMap<String, Ui.AppEntry> map = new LinkedHashMap<>();
            for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
                for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                    ActivityInfo ai = ri.activityInfo;
                    String p = ai.packageName;
                    if (p.equals(getPackageName()) || map.containsKey(p) || HIDDEN.contains(p) || conf.hidden.contains(p)) continue;
                    // Built-in apps that only have a phone-style entry (no TV entry) are system helpers
                    // (button handlers, test tools...), not apps you'd open from the sofa. User-installed ones stay.
                    if (Intent.CATEGORY_LAUNCHER.equals(cat) && (ai.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    Ui.AppEntry e = new Ui.AppEntry();
                    e.pkg = p;
                    e.label = String.valueOf(ri.loadLabel(pm));
                    e.launch = new Intent(Intent.ACTION_MAIN).addCategory(cat)
                            .setComponent(new ComponentName(p, ai.name))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    Drawable bn = ai.loadBanner(pm);
                    if (bn == null) bn = ai.applicationInfo.loadBanner(pm);
                    e.banner = bn;
                    if (bn == null) e.icon = ri.loadIcon(pm);
                    map.put(p, e);
                }
            }
            List<Ui.AppEntry> d = new ArrayList<>(), g = new ArrayList<>();
            for (String p : readDockConfig()) {
                Ui.AppEntry e = map.remove(p);
                if (e != null && d.size() < COLS) d.add(e);
            }
            g.addAll(map.values());
            Collections.sort(g, (a, b) -> a.label.compareToIgnoreCase(b.label));
            while (d.size() < COLS && !g.isEmpty()) d.add(g.remove(0));   // fill an incomplete dock
            for (Ui.AppEntry e : d) e.color = Ui.sampleColor(e.banner != null ? e.banner : e.icon);
            ui.post(() -> bindApps(d, g));
        }).start();
    }

    /** Dock order: dock.txt in the app's files dir if present (no rebuild needed), else res/values/dock.xml. */
    List<String> readDockConfig() {
        if (!conf.dock.isEmpty()) return conf.dock;
        File f = new File(getExternalFilesDir(null), "dock.txt");
        if (f.exists()) {
            List<String> out = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String l;
                while ((l = r.readLine()) != null) { l = l.trim(); if (!l.isEmpty() && !l.startsWith("#")) out.add(l); }
            } catch (Exception ignored) {}
            if (!out.isEmpty()) return out;
        }
        return Arrays.asList(getResources().getStringArray(R.array.dock_packages));
    }

    void bindApps(List<Ui.AppEntry> d, List<Ui.AppEntry> g) {
        String keepGrid = loaded && region == GRID && gridIdx < gridApps.size() ? gridApps.get(gridIdx).pkg : null;
        dockApps.clear(); dockApps.addAll(d);
        gridApps.clear(); gridApps.addAll(g);
        dockRow.removeAllViews();
        dockTiles.clear();
        focusedTile = null;
        for (int i = 0; i < dockApps.size(); i++) {
            TileView t = new TileView(this, fonts, tileW, tileH, false);
            t.bind(dockApps.get(i));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
            if (i < dockApps.size() - 1) lp.rightMargin = gap;
            dockRow.addView(t, lp);
            dockTiles.add(t);
        }
        adapter.notifyDataSetChanged();

        if (!restored) { keepGrid = restoreGridPkg; restored = true; }
        if (keepGrid != null) for (int i = 0; i < gridApps.size(); i++) if (gridApps.get(i).pkg.equals(keepGrid)) gridIdx = i;
        dockIdx = clamp(dockIdx, dockApps.size());
        gridIdx = clamp(gridIdx, gridApps.size());
        if (region == DOCK && dockApps.isEmpty()) region = gridApps.isEmpty() ? TOP : GRID;
        if (region == GRID && gridApps.isEmpty()) region = DOCK;
        loaded = true;
        if (!dockApps.isEmpty()) { Ui.AppEntry e = dockApps.get(dockIdx); hero.setText(e.label); setGlow(e.color); glowColor = e.color; }
        afterLayout(() -> { applyFocus(false); ui.postDelayed(() -> applyFocus(false), 400); });
    }

    static int clamp(int i, int n) { return n == 0 ? 0 : Math.max(0, Math.min(i, n - 1)); }

    final class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {
        final class VH extends RecyclerView.ViewHolder {
            final TileView tile;
            VH(TileView t) { super(t); tile = t; }
        }
        @Override public VH onCreateViewHolder(ViewGroup parent, int type) {
            TileView t = new TileView(HomeActivity.this, fonts, tileW, tileH, true);
            t.setPadding(gap / 2, 0, gap / 2, dp(6));
            t.setLayoutParams(new RecyclerView.LayoutParams(tileW + gap, -2));
            return new VH(t);
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            h.tile.bind(gridApps.get(pos));
            boolean f = region == GRID && pos == gridIdx;
            h.tile.setFocusedLook(f, false);
            if (f) focusedTile = h.tile;
        }
        @Override public int getItemCount() { return gridApps.size(); }
    }

    // ---------------------------------------------------------------- focus & keys

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean ours = k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN
                || k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT
                || isOk(k) || k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_MENU;
        if (!ours) return super.dispatchKeyEvent(e);
        if (e.getAction() == KeyEvent.ACTION_DOWN && loaded) onNav(k);
        return true;
    }

    static boolean isOk(int k) {
        return k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    void onNav(int k) {
        final int L = KeyEvent.KEYCODE_DPAD_LEFT, R = KeyEvent.KEYCODE_DPAD_RIGHT,
                U = KeyEvent.KEYCODE_DPAD_UP, D = KeyEvent.KEYCODE_DPAD_DOWN, B = KeyEvent.KEYCODE_BACK;
        int n = gridApps.size();
        switch (region) {
            case TOP:
                if (k == L) { if (topIdx <= minTop) return; topIdx--; }
                else if (k == R) { if (topIdx == 2) return; topIdx++; }
                else if (k == D || k == B) { if (!dockApps.isEmpty()) region = DOCK; else if (n > 0) region = GRID; else return; }
                else if (isOk(k)) { pillAction(topIdx); return; }
                else return;
                break;
            case DOCK:
                if (k == L) { if (dockIdx == 0) return; dockIdx--; }
                else if (k == R) { if (dockIdx >= dockApps.size() - 1) return; dockIdx++; }
                else if (k == U) region = TOP;
                else if (k == D) { if (n == 0) return; region = GRID; gridIdx = Math.min(dockIdx, n - 1); }
                else if (isOk(k)) { launch(dockApps.get(dockIdx)); return; }
                else if (k == KeyEvent.KEYCODE_MENU) { appInfo(dockApps.get(dockIdx)); return; }
                else return;
                break;
            case GRID: {
                int row = gridIdx / COLS, col = gridIdx % COLS, rows = (n + COLS - 1) / COLS;
                if (k == L) { if (col == 0) return; gridIdx--; }
                else if (k == R) { if (col == COLS - 1 || gridIdx + 1 >= n) return; gridIdx++; }
                else if (k == U) {
                    if (row > 0) gridIdx -= COLS;
                    else if (!dockApps.isEmpty()) { region = DOCK; dockIdx = Math.min(col, dockApps.size() - 1); }
                    else region = TOP;
                }
                else if (k == D) { if (row >= rows - 1) return; gridIdx = Math.min(gridIdx + COLS, n - 1); }
                else if (k == B) { if (dockApps.isEmpty()) return; region = DOCK; }
                else if (isOk(k)) { launch(gridApps.get(gridIdx)); return; }
                else if (k == KeyEvent.KEYCODE_MENU) { appInfo(gridApps.get(gridIdx)); return; }
                else return;
                break;
            }
            case MENU:
                if (k == U) { if (menuIdx == 0) { closeMenu(); return; } menuIdx--; }
                else if (k == D) { if (menuIdx >= menuRows.size() - 1) return; menuIdx++; }
                else if (isOk(k)) { Runnable r = menuActions.get(menuIdx); closeMenu(); r.run(); return; }
                else if (k == B || k == L || k == R) { closeMenu(); return; }
                else return;
                break;
        }
        applyFocus(true);
    }

    /** Apply the current focus state: tile scale, hero text/glow, page scroll, highlight. */
    void applyFocus(boolean animate) {
        TileView t = null;
        if (region == DOCK && dockIdx < dockTiles.size()) t = dockTiles.get(dockIdx);
        else if (region == GRID) t = gridTile(gridIdx);
        if (t != focusedTile && focusedTile != null) focusedTile.setFocusedLook(false, animate);
        if (t != null) t.setFocusedLook(true, animate);
        focusedTile = t;
        updateHero(animate);
        scrollPage(animate);
        moveHighlight(animate);
        saveFocus();
    }

    TileView gridTile(int pos) {
        View v = glm.findViewByPosition(pos);
        return v instanceof TileView ? (TileView) v : null;
    }

    View targetView() {
        switch (region) {
            case TOP: return pills[topIdx];
            case DOCK: return dockIdx < dockTiles.size() ? dockTiles.get(dockIdx).card : null;
            case GRID: { TileView t = gridTile(gridIdx); return t == null ? null : t.card; }
            case MENU: return menuIdx < menuRows.size() ? menuRows.get(menuIdx) : null;
        }
        return null;
    }

    /** Bounds of v in root coordinates, at the given scale around its centre (follows page translation). */
    RectF rectOf(View v, float scale) {
        int[] rl = new int[2], pl = new int[2];
        root.getLocationInWindow(rl);
        ((View) v.getParent()).getLocationInWindow(pl);
        float l = pl[0] + v.getLeft() - rl[0], t = pl[1] + v.getTop() - rl[1];
        float w = v.getWidth() * scale, h = v.getHeight() * scale;
        float cx = l + v.getWidth() / 2f, cy = t + v.getHeight() / 2f;
        return new RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
    }

    void moveHighlight(boolean animate) {
        final View tv = targetView();
        if (tv == null) { hl.hide(); return; }
        final float sc = (region == DOCK || region == GRID) ? TileView.FOCUS_SCALE : 1f;
        final float rad = region == TOP ? tv.getHeight() / 2f : region == MENU ? dp(12) : dp(14) * sc;
        if (hlAnim != null) hlAnim.cancel();
        if (!animate || !hl.shown || hl.rect.isEmpty()) { hl.set(rectOf(tv, sc), rad); return; }
        final RectF from = new RectF(hl.rect);
        final float fromR = hl.radius;
        hlAnim = ValueAnimator.ofFloat(0f, 1f);
        hlAnim.setDuration(360);
        hlAnim.setInterpolator(new OvershootInterpolator(1.25f));   // the "spring"
        hlAnim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            RectF to = rectOf(tv, sc);
            hl.set(new RectF(lerp(from.left, to.left, f), lerp(from.top, to.top, f),
                    lerp(from.right, to.right, f), lerp(from.bottom, to.bottom, f)), lerp(fromR, rad, Math.min(1f, f)));
        });
        hlAnim.start();
    }

    static float lerp(float a, float b, float f) { return a + (b - a) * f; }

    void scrollPage(boolean animate) {
        float target = 0;
        if (region == GRID) {
            View rowView = glm.findViewByPosition((gridIdx / COLS) * COLS);
            if (rowView != null) {
                float rowTop = grid.getTop() + rowView.getTop();
                target = -(rowTop - dp(110));
                float min = -(page.getHeight() - H);
                if (target < min) target = min;
                if (target > 0) target = 0;
            }
        }
        page.animate().cancel();
        if (animate) page.animate().translationY(target).setDuration(320).setInterpolator(new DecelerateInterpolator(2f)).start();
        else page.setTranslationY(target);
    }

    void updateHero(boolean animate) {
        float a = region == DOCK ? 1f : 0.4f;
        if (dockApps.isEmpty()) { hero.setText(""); return; }
        final Ui.AppEntry e = dockApps.get(dockIdx);
        if (!e.label.contentEquals(hero.getText())) {
            hero.animate().cancel();
            if (animate) {
                hero.animate().alpha(0f).setDuration(90).withEndAction(() -> {
                    hero.setText(e.label);
                    hero.animate().alpha(a).setDuration(180).start();
                }).start();
            } else { hero.setText(e.label); hero.setAlpha(a); }
            animateGlow(e.color, animate);
        } else {
            if (animate) hero.animate().alpha(a).setDuration(180).start(); else hero.setAlpha(a);
        }
        glow.animate().alpha(a).setDuration(animate ? 220 : 0).start();
    }

    void animateGlow(int to, boolean animate) {
        if (glowAnim != null) glowAnim.cancel();
        if (!animate) { glowColor = to; setGlow(to); return; }
        glowAnim = ValueAnimator.ofObject(new ArgbEvaluator(), glowColor, to);
        glowAnim.setDuration(380);
        glowAnim.addUpdateListener(an -> { glowColor = (int) an.getAnimatedValue(); setGlow(glowColor); });
        glowAnim.start();
    }

    void setGlow(int c) {
        glowShape.setColors(new int[]{withAlpha(c, 0x52), withAlpha(c, 0x1A), 0x00000000});
        if (hero != null) hero.setShadowLayer(dp(26), 0, 0, withAlpha(c, 0xB0));
    }

    void saveFocus() {
        if (!loaded) return;
        prefs.edit().putInt("region", region == MENU ? TOP : region).putInt("top", topIdx).putInt("dock", dockIdx)
                .putString("gridPkg", gridIdx < gridApps.size() ? gridApps.get(gridIdx).pkg : null).apply();
    }

    void afterLayout(Runnable r) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                r.run();
            }
        });
        root.requestLayout();
    }

    // ---------------------------------------------------------------- actions

    void launch(Ui.AppEntry e) {
        try { startActivity(e.launch); }
        catch (Exception ex) { Toast.makeText(this, "Can't open " + e.label, Toast.LENGTH_SHORT).show(); }
    }

    void appInfo(Ui.AppEntry e) {
        safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + e.pkg)));
    }

    void safeStart(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); } catch (Exception ex) { Toast.makeText(this, "Not available", Toast.LENGTH_SHORT).show(); }
    }

    void pillAction(int i) {
        if (i == 0) openMenu();
        else if (i == 1) safeStart(new Intent(Settings.ACTION_WIFI_SETTINGS));
        else safeStart(new Intent(Settings.ACTION_SETTINGS));
    }

    /** Physical inputs (HDMI, AV, ...) on this device. Empty on boxes and sticks, which are themselves an HDMI source. */
    List<TvInputInfo> hardwareInputs() {
        List<TvInputInfo> inputs = new ArrayList<>();
        try {
            TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tim != null) for (TvInputInfo in : tim.getTvInputList()) {
                if (in.getParentId() != null) continue;               // skip CEC child devices
                int t = in.getType();
                if (t == TvInputInfo.TYPE_HDMI || t == TvInputInfo.TYPE_COMPOSITE || t == TvInputInfo.TYPE_COMPONENT
                        || t == TvInputInfo.TYPE_SVIDEO || t == TvInputInfo.TYPE_SCART || t == TvInputInfo.TYPE_VGA
                        || t == TvInputInfo.TYPE_DVI) inputs.add(in);
            }
        } catch (Exception ignored) {}
        return inputs;
    }

    /** Show the Inputs pill only when there is something to switch to (hides it on Mi Box, TV Stick, Shield...). */
    void updateInputsPill() {
        boolean has = !hardwareInputs().isEmpty() || liveTvIntent() != null;
        pills[0].setVisibility(has ? View.VISIBLE : View.GONE);
        minTop = has ? 0 : 1;
        if (topIdx < minTop) topIdx = minTop;
    }

    void openMenu() {
        menu.removeAllViews();
        menuRows.clear();
        menuActions.clear();
        List<TvInputInfo> inputs = hardwareInputs();
        Collections.sort(inputs, (a, b) -> {
            int ta = a.getType() == TvInputInfo.TYPE_HDMI ? 0 : 1, tb = b.getType() == TvInputInfo.TYPE_HDMI ? 0 : 1;
            if (ta != tb) return ta - tb;
            return inputLabel(a).compareToIgnoreCase(inputLabel(b));
        });
        for (TvInputInfo in : inputs) {
            String label = inputLabel(in);
            if (in.getType() == TvInputInfo.TYPE_COMPOSITE && !label.toUpperCase(Locale.ROOT).contains("AV")) label = label + " (AV)";
            final String id = in.getId();
            addMenuRow(label, () -> switchInput(id));
        }
        if (liveTvIntent() != null) addMenuRow("Live TV", this::openLiveTv);

        RectF pr = rectOf(pills[0], 1f);
        menu.setX(pr.left);
        menu.setY(pr.bottom + dp(12));
        menu.setAlpha(0f);
        menu.setTranslationY(-dp(8));
        menu.setVisibility(View.VISIBLE);
        menu.animate().alpha(1f).translationY(0).setDuration(170).start();
        region = MENU;
        menuIdx = 0;
        afterLayout(() -> applyFocus(true));
    }

    String inputLabel(TvInputInfo in) {
        CharSequence c = in.loadCustomLabel(this);
        if (c == null || c.length() == 0) c = in.loadLabel(this);
        return c == null ? in.getId() : c.toString();
    }

    void addMenuRow(String label, Runnable action) {
        TextView t = text(400, 17, 0xFFF2F2F7);
        t.setText(label);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(16), 0, dp(16), 0);
        menu.addView(t, new LinearLayout.LayoutParams(-1, dp(46)));
        menuRows.add(t);
        menuActions.add(action);
    }

    void closeMenu() {
        menu.animate().cancel();
        menu.setVisibility(View.GONE);
        region = TOP;
        applyFocus(true);
    }

    void switchInput(String inputId) {
        Uri uri = TvContract.buildChannelUriForPassthroughInput(inputId);
        Intent i = new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); return; } catch (ActivityNotFoundException ignored) {}
        Intent li = liveTvIntent();
        if (li != null) { li.setData(uri); safeStart(li); }
        else Toast.makeText(this, "Can't switch input", Toast.LENGTH_SHORT).show();
    }

    /** This TV's Live TV app: a known maker app, else whatever handles "view TV channels"; null if none. */
    Intent liveTvIntent() {
        PackageManager pm = getPackageManager();
        for (String p : LIVE_TV_APPS) {
            Intent li = pm.getLeanbackLaunchIntentForPackage(p);
            if (li == null) li = pm.getLaunchIntentForPackage(p);
            if (li != null) return li;
        }
        Intent v = new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI);
        ResolveInfo ri = pm.resolveActivity(v, 0);
        if (ri != null && ri.activityInfo != null) {
            String p = ri.activityInfo.packageName;
            if (!"android".equals(p) && !getPackageName().equals(p)) return v.setPackage(p);
        }
        return null;
    }

    void openLiveTv() {
        Intent li = liveTvIntent();
        if (li != null) safeStart(li);
        else Toast.makeText(this, "No Live TV app on this TV", Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- clock, wifi, wallpaper (event-driven, no polling)

    void tick() {
        Date now = new Date();
        boolean h24 = DateFormat.is24HourFormat(this);
        clock.setText(new SimpleDateFormat(h24 ? "H:mm" : "h:mm", Locale.getDefault()).format(now));
        date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greeting.setText(h < 5 ? "Good night" : h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : h < 22 ? "Good evening" : "Good night");
        if (matchCard != null) { refreshMatchCard(); maybeFetchFixtures(); }
    }

    @SuppressWarnings("deprecation")
    void updateWifi() {
        String s = "Offline";
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) {
                if (ni.getType() == ConnectivityManager.TYPE_ETHERNET) s = "Ethernet";
                else if (ni.getType() == ConnectivityManager.TYPE_WIFI) {
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    String ssid = wm.getConnectionInfo().getSSID();
                    if (ssid == null || ssid.contains("unknown ssid")) ssid = ni.getExtraInfo();
                    if (ssid != null) ssid = ssid.replace("\"", "").trim();
                    s = (ssid == null || ssid.isEmpty() || ssid.contains("unknown")) ? "Wi-Fi" : ssid;
                } else s = "Connected";
            }
        } catch (Exception ignored) {}
        if (!s.contentEquals(wifiText.getText())) {
            wifiText.setText(s);
            if (loaded && region == TOP) afterLayout(() -> moveHighlight(false));
        }
    }

    /** Optional wallpaper: push a JPEG/PNG to the app's files dir (see README). Black when absent. */
    void loadWallpaper() {
        File dir = getExternalFilesDir(null);
        File f = null;
        for (String n : new String[]{"wallpaper.jpg", "wallpaper.png"}) {
            File c = new File(dir, n);
            if (c.exists()) { f = c; break; }
        }
        long stamp = f == null ? 0 : f.lastModified() ^ f.length();
        if (stamp == wallStamp) return;
        wallStamp = stamp;
        if (f == null) { wall.setImageDrawable(null); return; }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= W) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        wall.setImageBitmap(BitmapFactory.decodeFile(f.getPath(), o));
    }

    // ---------------------------------------------------------------- football fixtures card

    static final long MATCH_LENGTH = 2 * 3600_000L;     // treat a match as "on" for 2 h after kick-off
    static final long REFETCH_EVERY = 3 * 3600_000L;   // at most one fetch every 3 h, and only while home is shown

    Fixtures.Match nextMatch(long now) {
        for (Fixtures.Match m : matches) if (m.kickoff + MATCH_LENGTH > now) return m;
        return null;
    }

    void maybeFetchFixtures() {
        if (teamId == null || teamId.isEmpty() || fetching) return;
        long now = System.currentTimeMillis();
        Fixtures.Match n = nextMatch(now);
        boolean stale = now - lastFetch > REFETCH_EVERY;
        boolean retryFailed = now - lastFetch > 15 * 60_000L && n == null;   // nothing cached yet: retry after 15 min
        if (!stale && !retryFailed) return;
        fetching = true;
        new Thread(() -> {
            try {
                String json = Fixtures.fetch(teamId);
                List<Fixtures.Match> parsed = Fixtures.parse(json, teamId);
                ui.post(() -> {
                    if (!parsed.isEmpty()) {
                        matches = parsed;
                        prefs.edit().putString("fixJson", json).apply();
                    }
                    lastFetch = System.currentTimeMillis();
                    prefs.edit().putLong("fixFetched", lastFetch).apply();
                    fetching = false;
                    refreshMatchCard();
                });
            } catch (Exception e) {
                ui.post(() -> { lastFetch = System.currentTimeMillis(); fetching = false; });
            }
        }).start();
    }

    void refreshMatchCard() {
        if (matchCard == null) return;
        long now = System.currentTimeMillis();
        Fixtures.Match m = nextMatch(now);
        if (m == null) { matchCard.setVisibility(View.GONE); return; }
        matchCard.setVisibility(View.VISIBLE);
        mTitle.setText((m.home ? "vs " : "at ") + m.opponent);
        String comp = m.competition;
        String where = m.home ? "Home" : "Away";
        mComp.setText(comp.isEmpty() ? where : comp + "  \u00B7  " + where);
        if (now >= m.kickoff) {
            mWhen.setText("LIVE NOW");
            mWhen.setTextColor(getColor(R.color.fixture_accent));
            long mins = (now - m.kickoff) / 60_000L;
            mCountdown.setText("kicked off " + mins + " min ago");
            return;
        }
        mWhen.setTextColor(0xFFFFFFFF);
        Calendar k = Calendar.getInstance(); k.setTimeInMillis(m.kickoff);
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance(); tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        String day;
        if (sameDay(k, today)) day = "Today";
        else if (sameDay(k, tomorrow)) day = "Tomorrow";
        else if (m.kickoff - now < 6L * 24 * 3600_000L) day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(m.kickoff));
        else day = new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(m.kickoff));
        String time = new SimpleDateFormat(DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm a", Locale.getDefault()).format(new Date(m.kickoff));
        mWhen.setText(day + "  \u00B7  " + time);
        long mins = (m.kickoff - now) / 60_000L;
        String cd;
        if (mins < 60) cd = "in " + mins + " min";
        else if (mins < 48 * 60) cd = "in " + (mins / 60) + "h " + (mins % 60) + "m";
        else cd = "in " + (mins / (24 * 60)) + " days";
        mCountdown.setText(cd);
    }

    static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
