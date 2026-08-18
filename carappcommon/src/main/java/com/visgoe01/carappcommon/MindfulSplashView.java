package com.visgoe01.carappcommon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public final class MindfulSplashView {
    public interface CompletionListener {
        void onSplashComplete();
    }

    private static final int[] RAINBOW = {
        Color.rgb(255, 0, 0),
        Color.rgb(255, 127, 0),
        Color.rgb(255, 255, 0),
        Color.rgb(0, 170, 0),
        Color.rgb(0, 0, 255),
        Color.rgb(75, 0, 130)
    };
    private static final int DEEP_BLUE = Color.rgb(0, 34, 112);

    private MindfulSplashView() {}

    public static FrameLayout create(
            Context context,
            String appName,
            String versionText,
            Handler handler,
            CompletionListener completionListener) {
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.rgb(250, 235, 248));

        ImageView background = new ImageView(context);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap art = loadArt(context);
        if (art != null) {
            background.setImageBitmap(art);
        }
        root.addView(background, new FrameLayout.LayoutParams(-1, -1));

        TextView title = splashText(context, appName, 34, true);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        titleParams.setMargins(dp(context, 18), dp(context, 54), dp(context, 18), 0);
        root.addView(title, titleParams);

        TextView version = splashText(context, versionText, 24, true);
        FrameLayout.LayoutParams versionParams = new FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        versionParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        versionParams.setMargins(dp(context, 18), dp(context, 106), dp(context, 18), 0);
        root.addView(version, versionParams);

        TextView breathe = splashText(context, "Breathe...", 30, true);
        FrameLayout.LayoutParams breatheParams = new FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        breatheParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        breatheParams.setMargins(dp(context, 18), 0, dp(context, 18), dp(context, 116));
        root.addView(breathe, breatheParams);

        TextView countdown = splashText(context, "5", 58, true);
        countdown.setBackground(countdownCircleBackground());
        FrameLayout.LayoutParams countParams = new FrameLayout.LayoutParams(dp(context, 118), dp(context, 118));
        countParams.gravity = Gravity.CENTER;
        root.addView(countdown, countParams);

        scheduleCountdown(countdown, handler, completionListener);
        return root;
    }

    private static TextView splashText(Context context, String value, int sp, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value == null ? "" : value);
        text.setTextColor(DEEP_BLUE);
        text.setTextSize(sp);
        text.setGravity(Gravity.CENTER);
        text.setTypeface(Typeface.create("sans-serif-condensed", bold ? Typeface.BOLD : Typeface.NORMAL));
        text.setShadowLayer(8f, 0f, 3f, Color.WHITE);
        return text;
    }

    private static GradientDrawable countdownCircleBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(Color.argb(218, 135, 206, 250));
        background.setStroke(3, Color.argb(230, 255, 255, 255));
        return background;
    }

    private static void scheduleCountdown(
            TextView countdown, Handler handler, CompletionListener completionListener) {
        for (int tick = 0; tick < MindfulSplashTiming.START_COUNT; tick += 1) {
            final int count = MindfulSplashTiming.countAtTick(tick);
            long delayMs = MindfulSplashTiming.delayForTick(tick);
            handler.postDelayed(
                    () -> {
                        countdown.setText(String.valueOf(count));
                        countdown.setTextColor(RAINBOW[(MindfulSplashTiming.START_COUNT - count) % RAINBOW.length]);
                        countdown.setAlpha(count % 2 == 0 ? 0.78f : 1.0f);
                    },
                    delayMs);
        }
        handler.postDelayed(
                () -> {
                    if (completionListener != null) {
                        completionListener.onSplashComplete();
                    }
                },
                MindfulSplashTiming.HOLD_MS);
    }

    private static Bitmap loadArt(Context context) {
        try (InputStream input = context.getAssets().open("car_common/avni-art.jpg")) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException error) {
            return null;
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
