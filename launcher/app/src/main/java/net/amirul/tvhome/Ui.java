package net.amirul.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.Map;

/** Small shared helpers: units, fonts, colour sampling, app record. */
final class Ui {
    private Ui() {}

    static float density = 1f;

    static int dp(float v) { return Math.round(v * density); }

    static int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    /** One app shown on the home screen. */
    static final class AppEntry {
        String pkg, label;
        Intent launch;
        Drawable banner, icon;
        int color = 0xFF8E8E93;
    }

    /** Outfit variable font at any weight; falls back to the system sans if the asset is missing. */
    static final class Fonts {
        private final AssetManager am;
        private final Map<Integer, Typeface> cache = new HashMap<>();
        Fonts(Context c) { am = c.getAssets(); }
        Typeface get(int weight) {
            Typeface t = cache.get(weight);
            if (t != null) return t;
            try {
                t = new Typeface.Builder(am, "fonts/Outfit.ttf")
                        .setFontVariationSettings("'wght' " + weight).build();
            } catch (Exception e) { t = null; }
            if (t == null) t = Typeface.create(weight < 350 ? "sans-serif-light" : "sans-serif", Typeface.NORMAL);
            cache.put(weight, t);
            return t;
        }
    }

    /** Dominant saturated colour of a banner, brightened for use as an ambient glow. */
    static int sampleColor(Drawable d) {
        final int fallback = 0xFF8E8E93;
        if (d == null) return fallback;
        Bitmap b = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Drawable.ConstantState cs = d.getConstantState();
        Drawable src = cs != null ? cs.newDrawable().mutate() : d;
        Rect old = src.copyBounds();
        src.setBounds(0, 0, 32, 18);
        src.draw(c);
        if (src == d) d.setBounds(old);
        float[] hsv = new float[3];
        double sx = 0, sy = 0, ws = 0, sat = 0, val = 0;
        for (int y = 0; y < 18; y++) for (int x = 0; x < 32; x++) {
            int p = b.getPixel(x, y);
            if (Color.alpha(p) < 128) continue;
            Color.colorToHSV(p, hsv);
            double w = hsv[1] * hsv[2];
            w = w * w;
            if (w < 0.02) continue;
            double h = Math.toRadians(hsv[0]);
            sx += Math.cos(h) * w; sy += Math.sin(h) * w;
            sat += hsv[1] * w; val += hsv[2] * w; ws += w;
        }
        b.recycle();
        if (ws == 0) return fallback;
        float hue = (float) Math.toDegrees(Math.atan2(sy, sx));
        if (hue < 0) hue += 360f;
        float s = (float) Math.min(0.85, Math.max(0.55, sat / ws));
        float v = (float) Math.max(0.9, val / ws);
        return Color.HSVToColor(new float[]{hue, s, Math.min(1f, v)});
    }
}
