package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public final class ToneDingPlayer {
    private static final int DING_MS = 180;
    private static final long GAP_MS = 420L;
    private final Handler handler;
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 82);
    private final Vibrator vibrator;

    public ToneDingPlayer(Context context, Handler handler) {
        this.handler = handler;
        if (Build.VERSION.SDK_INT >= 31) {
            vibrator = context.getSystemService(VibratorManager.class).getDefaultVibrator();
        } else {
            vibrator = context.getSystemService(Vibrator.class);
        }
    }

    public void play(int count) {
        play(count, true, false);
    }

    public void play(int count, boolean chimesEnabled, boolean vibrationEnabled) {
        if (!chimesEnabled && !vibrationEnabled) {
            return;
        }
        playNext(Math.max(0, count), chimesEnabled, vibrationEnabled);
    }

    private void playNext(int remaining, boolean chimesEnabled, boolean vibrationEnabled) {
        if (remaining <= 0) {
            return;
        }
        if (chimesEnabled) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, DING_MS);
        }
        if (vibrationEnabled && vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(DING_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        }
        if (remaining > 1) {
            handler.postDelayed(() -> playNext(remaining - 1, chimesEnabled,
                    vibrationEnabled), GAP_MS);
        }
    }

    public void release() {
        tone.release();
    }
}
