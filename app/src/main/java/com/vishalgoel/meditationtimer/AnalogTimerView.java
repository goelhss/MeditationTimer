package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public final class AnalogTimerView extends View {
    private static final int ELAPSED_COLOR = Color.rgb(78, 52, 108);
    private static final int REMAINING_COLOR = Color.rgb(244, 180, 104);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private long durationMs = TimerSchedule.MINUTE_MS;
    private long remainingMs = TimerSchedule.MINUTE_MS;

    public AnalogTimerView(Context context) {
        super(context);
        setMinimumHeight(dp(310));
    }

    public void setTime(long remainingMs, long durationMs) {
        this.durationMs = Math.max(1L, durationMs);
        this.remainingMs = Math.max(0L, Math.min(this.durationMs, remainingMs));
        setContentDescription("Analog timer, "
                + LogTextExporter.formatDuration(this.durationMs - this.remainingMs)
                + " elapsed, " + LogTextExporter.formatDuration(this.remainingMs)
                + " remaining");
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int preferred = Math.min(width, dp(360));
        int height = resolveSize(preferred, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight()) - dp(28);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = size / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(221, 238, 255));
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.rgb(56, 108, 176));
        canvas.drawCircle(cx, cy, radius, paint);

        for (int tick = 0; tick < 60; tick++) {
            double angle = Math.toRadians(tick * 6.0 - 90.0);
            float outerX = cx + (float) Math.cos(angle) * (radius - dp(10));
            float outerY = cy + (float) Math.sin(angle) * (radius - dp(10));
            float innerRadius = radius - dp(tick % 5 == 0 ? 27 : 19);
            float innerX = cx + (float) Math.cos(angle) * innerRadius;
            float innerY = cy + (float) Math.sin(angle) * innerRadius;
            paint.setStrokeWidth(dp(tick % 5 == 0 ? 3 : 1));
            paint.setColor(Color.rgb(38, 53, 68));
            canvas.drawLine(innerX, innerY, outerX, outerY, paint);
        }

        float remainingFraction = remainingMs / (float) durationMs;
        float elapsedFraction = 1f - remainingFraction;
        arc.set(cx - radius + dp(5), cy - radius + dp(5),
                cx + radius - dp(5), cy + radius - dp(5));
        paint.setStrokeWidth(dp(18));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(REMAINING_COLOR);
        canvas.drawArc(arc, -90f, 360f, false, paint);
        if (elapsedFraction > 0f) {
            paint.setColor(ELAPSED_COLOR);
            canvas.drawArc(arc, -90f, 360f * elapsedFraction, false, paint);
        }

        double handAngle = Math.toRadians(-90.0 + 360.0 * elapsedFraction);
        float handLength = radius - dp(48);
        paint.setStrokeWidth(dp(7));
        paint.setColor(ELAPSED_COLOR);
        canvas.drawLine(cx, cy, cx + (float) Math.cos(handAngle) * handLength,
                cy + (float) Math.sin(handAngle) * handLength, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, dp(10), paint);

        String label = MeditationTimerService.formatMinuteCountdown(remainingMs);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(31));
        paint.setColor(Color.rgb(38, 53, 68));
        paint.setFakeBoldText(true);
        canvas.drawText(label, cx, cy + dp(12), paint);
        paint.setTextSize(dp(13));
        paint.setColor(ELAPSED_COLOR);
        canvas.drawText("Elapsed", cx - radius * 0.28f, cy + radius * 0.52f, paint);
        paint.setColor(REMAINING_COLOR);
        canvas.drawText("Left", cx + radius * 0.28f, cy + radius * 0.52f, paint);
        paint.setFakeBoldText(false);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
