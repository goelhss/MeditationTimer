package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.List;

public final class StatsChartView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<MeditationStats.Bucket> buckets = List.of();

    public StatsChartView(Context context) {
        super(context);
        barPaint.setColor(Color.rgb(173, 92, 230));
        labelPaint.setColor(Color.rgb(72, 54, 82));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(dp(11));
        guidePaint.setColor(Color.rgb(218, 205, 224));
        guidePaint.setStrokeWidth(dp(1));
        setBackgroundColor(Color.WHITE);
    }

    public void setBuckets(List<MeditationStats.Bucket> buckets) {
        this.buckets = buckets == null ? List.of() : List.copyOf(buckets);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (buckets.isEmpty()) {
            return;
        }
        float left = dp(12);
        float right = getWidth() - dp(12);
        float top = dp(18);
        float bottom = getHeight() - dp(34);
        canvas.drawLine(left, bottom, right, bottom, guidePaint);
        long max = 1L;
        for (MeditationStats.Bucket bucket : buckets) {
            max = Math.max(max, bucket.durationMs());
        }
        float slot = (right - left) / buckets.size();
        float barWidth = Math.max(dp(4), slot * 0.62f);
        for (int index = 0; index < buckets.size(); index += 1) {
            MeditationStats.Bucket bucket = buckets.get(index);
            float center = left + slot * index + slot / 2f;
            float height = (bottom - top) * bucket.durationMs() / (float) max;
            canvas.drawRoundRect(center - barWidth / 2f, bottom - height,
                    center + barWidth / 2f, bottom, dp(4), dp(4), barPaint);
            canvas.drawText(bucket.label(), center, getHeight() - dp(12), labelPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
