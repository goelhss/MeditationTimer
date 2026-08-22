package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class LotusSplashView {
    static final long DISPLAY_MS = 2200L;

    private LotusSplashView() {}

    public static View create(Context context, Handler handler, Runnable onComplete) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(context, 32), dp(context, 48), dp(context, 32), dp(context, 48));
        root.setBackgroundColor(Color.rgb(23, 15, 36));

        ImageView lotus = new ImageView(context);
        lotus.setImageResource(R.drawable.lotus_ocean_13_petals);
        lotus.setContentDescription("Purple 13-petal lotus over the ocean");
        lotus.setAdjustViewBounds(true);
        root.addView(lotus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 330)));

        TextView title = new TextView(context);
        title.setText(R.string.app_name);
        title.setTextColor(Color.rgb(247, 241, 255));
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(context, 20);
        root.addView(title, titleParams);

        TextView message = new TextView(context);
        message.setText(R.string.splash_message);
        message.setTextColor(Color.rgb(205, 191, 218));
        message.setTextSize(17);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = dp(context, 8);
        root.addView(message, messageParams);

        handler.postDelayed(onComplete, DISPLAY_MS);
        return root;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
