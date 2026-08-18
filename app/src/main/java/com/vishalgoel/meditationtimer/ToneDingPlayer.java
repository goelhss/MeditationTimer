package com.vishalgoel.meditationtimer;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

public final class ToneDingPlayer {
    private static final int DING_MS = 180;
    private static final long GAP_MS = 420L;
    private final Handler handler;
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 82);

    public ToneDingPlayer(Handler handler) {
        this.handler = handler;
    }

    public void play(int count) {
        playNext(Math.max(0, count));
    }

    private void playNext(int remaining) {
        if (remaining <= 0) {
            return;
        }
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, DING_MS);
        if (remaining > 1) {
            handler.postDelayed(() -> playNext(remaining - 1), GAP_MS);
        }
    }

    public void release() {
        tone.release();
    }
}
