package net.amirul.tvhome;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A 16:9 app card. Scales up slightly when focused; grid tiles also show their name underneath. */
final class TileView extends LinearLayout {
    static final float FOCUS_SCALE = 1.1f;
    final FrameLayout card;
    final ImageView img;
    final TextView label, fallbackName;
    private boolean focused;

    TileView(Context c, Ui.Fonts fonts, int w, int h, boolean showLabel) {
        super(c);
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        final float r = Ui.dp(14);
        card = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1C1E);
        bg.setCornerRadius(r);
        card.setBackground(bg);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); }
        });
        card.setClipToOutline(true);
        img = new ImageView(c);
        card.addView(img, new FrameLayout.LayoutParams(-1, -1));
        fallbackName = new TextView(c);
        fallbackName.setTypeface(fonts.get(450));
        fallbackName.setTextSize(11);
        fallbackName.setTextColor(0xFFD1D1D6);
        fallbackName.setSingleLine(true);
        fallbackName.setEllipsize(TextUtils.TruncateAt.END);
        fallbackName.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        fl.bottomMargin = Math.round(h * 0.09f);
        fl.leftMargin = fl.rightMargin = Ui.dp(8);
        card.addView(fallbackName, fl);
        addView(card, new LayoutParams(w, h));

        label = new TextView(c);
        label.setTypeface(fonts.get(450));
        label.setTextSize(14);
        label.setTextColor(0xFFE5E5EA);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        label.setAlpha(0f);
        if (showLabel) {
            LayoutParams lp = new LayoutParams(w + Ui.dp(20), Ui.dp(28));
            lp.leftMargin = -Ui.dp(10);
            lp.topMargin = Ui.dp(12);
            addView(label, lp);
        }
    }

    void bind(Ui.AppEntry e) {
        label.setText(e.label);
        if (e.banner != null) {
            img.setImageDrawable(null);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setPadding(0, 0, 0, 0);
            img.setImageDrawable(e.banner);
            fallbackName.setVisibility(GONE);
        } else {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int ch = card.getLayoutParams().height;   // icon sits above the name, scaled to the tile
            img.setPadding(0, Math.round(ch * 0.12f), 0, Math.round(ch * 0.38f));
            img.setImageDrawable(e.icon);
            fallbackName.setText(e.label);
            fallbackName.setVisibility(VISIBLE);
        }
    }

    void setFocusedLook(boolean f, boolean animate) {
        if (f == focused && animate) return;
        focused = f;
        float s = f ? FOCUS_SCALE : 1f;
        card.animate().cancel();
        label.animate().cancel();
        if (animate) {
            card.animate().scaleX(s).scaleY(s).setDuration(220).setInterpolator(new DecelerateInterpolator(1.6f)).start();
            label.animate().alpha(f ? 1f : 0f).translationY(f ? Ui.dp(4) : 0).setDuration(200).start();
        } else {
            card.setScaleX(s); card.setScaleY(s);
            label.setAlpha(f ? 1f : 0f); label.setTranslationY(f ? Ui.dp(4) : 0);
        }
    }
}
