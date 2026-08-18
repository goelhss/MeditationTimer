package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

public final class TimerStateStore {
    private static final String PREFS = "timer_state";
    private final SharedPreferences prefs;

    public TimerStateStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(TimerState state) {
        prefs.edit()
                .putBoolean("active", state.active)
                .putBoolean("paused", state.paused)
                .putLong("duration_ms", state.durationMs)
                .putLong("primary_ms", state.primaryMs)
                .putLong("additional_ms", state.additionalMs)
                .putInt("finish_dings", state.finishDings)
                .putLong("start_wall_ms", state.startWallMs)
                .putLong("active_before_ms", state.activeBeforeSegmentMs)
                .putLong("segment_realtime_ms", state.segmentStartedRealtimeMs)
                .putLong("processed_through_ms", state.processedThroughActiveMs)
                .putBoolean("dim_screen", state.dimScreen)
                .putBoolean("chimes_enabled", state.chimesEnabled)
                .putBoolean("vibration_enabled", state.vibrationEnabled)
                .apply();
    }

    public TimerState load() {
        TimerState state = new TimerState();
        state.active = prefs.getBoolean("active", false);
        state.paused = prefs.getBoolean("paused", false);
        state.durationMs = prefs.getLong("duration_ms", 60L * TimerSchedule.MINUTE_MS);
        state.primaryMs = prefs.getLong("primary_ms", 5L * TimerSchedule.MINUTE_MS);
        state.additionalMs = prefs.getLong("additional_ms", 10L * TimerSchedule.MINUTE_MS);
        state.finishDings = prefs.getInt("finish_dings", 10);
        state.startWallMs = prefs.getLong("start_wall_ms", 0L);
        state.activeBeforeSegmentMs = prefs.getLong("active_before_ms", 0L);
        state.segmentStartedRealtimeMs = prefs.getLong("segment_realtime_ms", 0L);
        state.processedThroughActiveMs = prefs.getLong("processed_through_ms", 0L);
        state.dimScreen = prefs.getBoolean("dim_screen", true);
        state.chimesEnabled = prefs.getBoolean("chimes_enabled", true);
        state.vibrationEnabled = prefs.getBoolean("vibration_enabled", false);
        return state;
    }

    public boolean hasActiveSession() {
        return prefs.getBoolean("active", false);
    }

    public void clearActive() {
        prefs.edit().putBoolean("active", false).putBoolean("paused", false).apply();
    }
}
