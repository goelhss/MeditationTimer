package com.vishalgoel.meditationtimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class TimerActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) {
            return;
        }
        Intent service = new Intent(context, MeditationTimerService.class).setAction(action);
        context.startForegroundService(service);
    }
}
