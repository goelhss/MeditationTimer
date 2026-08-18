package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ToneDingPlayer {
    private static final int DING_MS = 180;
    public static final long GAP_MS = 900L;
    private final Handler handler;
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private final Vibrator vibrator;
    private final Set<AudioTrack> activeTracks = new HashSet<>();
    private final Map<ChimeSound, short[]> soundCache = new EnumMap<>(ChimeSound.class);

    public ToneDingPlayer(Context context, Handler handler) {
        this.handler = handler;
        if (Build.VERSION.SDK_INT >= 31) {
            vibrator = context.getSystemService(VibratorManager.class).getDefaultVibrator();
        } else {
            vibrator = context.getSystemService(Vibrator.class);
        }
    }

    public void play(int count) {
        play(count, true, false, ChimeSound.DEFAULT.id());
    }

    public void play(int count, boolean chimesEnabled, boolean vibrationEnabled) {
        play(count, chimesEnabled, vibrationEnabled, ChimeSound.DEFAULT.id());
    }

    public void play(int count, boolean chimesEnabled, boolean vibrationEnabled,
                     String soundId) {
        if (!chimesEnabled && !vibrationEnabled) {
            return;
        }
        playNext(Math.max(0, count), chimesEnabled, vibrationEnabled,
                ChimeSound.fromId(soundId));
    }

    private void playNext(int remaining, boolean chimesEnabled, boolean vibrationEnabled,
                          ChimeSound sound) {
        if (remaining <= 0) {
            return;
        }
        if (chimesEnabled) {
            playSound(sound);
        }
        if (vibrationEnabled && vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(DING_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        }
        if (remaining > 1) {
            handler.postDelayed(() -> playNext(remaining - 1, chimesEnabled,
                    vibrationEnabled, sound), GAP_MS);
        }
    }

    private void playSound(ChimeSound sound) {
        if (sound == ChimeSound.CLASSIC_DING) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, DING_MS);
            return;
        }
        short[] pcm = soundCache.computeIfAbsent(sound, ChimeSynthesizer::synthesize);
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(ChimeSynthesizer.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.length * 2)
                .build();
        int written = track.write(pcm, 0, pcm.length);
        if (written <= 0) {
            track.release();
            return;
        }
        track.setVolume(1.0f);
        activeTracks.add(track);
        track.play();
        handler.postDelayed(() -> {
            activeTracks.remove(track);
            track.stop();
            track.release();
        }, sound.durationMs() + 250L);
    }

    public static long playbackSpanMs(int count, String soundId) {
        ChimeSound sound = ChimeSound.fromId(soundId);
        if (count <= 0) {
            return 0L;
        }
        return (count - 1L) * GAP_MS + sound.durationMs();
    }

    public void release() {
        for (AudioTrack track : new HashSet<>(activeTracks)) {
            track.stop();
            track.release();
        }
        activeTracks.clear();
        soundCache.clear();
        tone.release();
    }
}
