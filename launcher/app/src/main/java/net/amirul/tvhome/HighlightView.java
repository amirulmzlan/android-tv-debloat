package net.amirul.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** The single translucent "glass" focus highlight that moves between tiles. No blur: fill + sheen + hairline. */
final class HighlightView extends View {
    final RectF rect = new RectF();
    float radius;
    boolean shown;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    HighlightView(Context c) {
        super(c);
        fill.setColor(0x1FFFFFFF);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Ui.dp(1.5f));
        stroke.setColor(0x6BFFFFFF);
        setWillNotDraw(false);
    }

    void set(RectF r, float rad) {
        rect.set(r);
        radius = rad;
        shown = true;
        invalidate();
    }

    void hide() { shown = false; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        if (!shown || rect.isEmpty()) return;
        c.drawRoundRect(rect, radius, radius, fill);
        sheen.setShader(new LinearGradient(0, rect.top, 0, rect.top + rect.height() * 0.55f,
                0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, radius, radius, sheen);
        float h = stroke.getStrokeWidth() / 2f;
        c.drawRoundRect(rect.left + h, rect.top + h, rect.right - h, rect.bottom - h, radius, radius, stroke);
    }
}
