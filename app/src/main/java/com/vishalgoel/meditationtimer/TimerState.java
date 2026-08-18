package com.vishalgoel.meditationtimer;

public final class TimerState {
    public boolean active;
    public boolean paused;
    public long durationMs;
    public long primaryMs;
    public long additionalMs;
    public int finishDings;
    public long startWallMs;
    public long activeBeforeSegmentMs;
    public long segmentStartedRealtimeMs;
    public long processedThroughActiveMs;
    public boolean dimScreen;
    public boolean chimesEnabled;
    public boolean vibrationEnabled;
    public String chimeSoundId;
    public String displayModeId;

    public static TimerState start(TimerSchedule schedule, long wallMs, long realtimeMs,
                                   boolean dimScreen) {
        return start(schedule, wallMs, realtimeMs, dimScreen, true, false);
    }

    public static TimerState start(TimerSchedule schedule, long wallMs, long realtimeMs,
                                   boolean dimScreen, boolean chimesEnabled,
                                   boolean vibrationEnabled) {
        return start(schedule, wallMs, realtimeMs, dimScreen, chimesEnabled,
                vibrationEnabled, ChimeSound.DEFAULT.id());
    }

    public static TimerState start(TimerSchedule schedule, long wallMs, long realtimeMs,
                                   boolean dimScreen, boolean chimesEnabled,
                                   boolean vibrationEnabled, String chimeSoundId) {
        return start(schedule, wallMs, realtimeMs, dimScreen, chimesEnabled,
                vibrationEnabled, chimeSoundId, TimerDisplayMode.DEFAULT.id());
    }

    public static TimerState start(TimerSchedule schedule, long wallMs, long realtimeMs,
                                   boolean dimScreen, boolean chimesEnabled,
                                   boolean vibrationEnabled, String chimeSoundId,
                                   String displayModeId) {
        if (!chimesEnabled && !vibrationEnabled) {
            throw new IllegalArgumentException("Enable Chimes, Vibrate, or both.");
        }
        TimerState state = new TimerState();
        state.active = true;
        state.paused = false;
        state.durationMs = schedule.durationMs();
        state.primaryMs = schedule.primaryMs();
        state.additionalMs = schedule.additionalMs();
        state.finishDings = schedule.finishDings();
        state.startWallMs = wallMs;
        state.activeBeforeSegmentMs = 0L;
        state.segmentStartedRealtimeMs = realtimeMs;
        state.processedThroughActiveMs = 0L;
        state.dimScreen = dimScreen;
        state.chimesEnabled = chimesEnabled;
        state.vibrationEnabled = vibrationEnabled;
        state.chimeSoundId = ChimeSound.fromId(chimeSoundId).id();
        state.displayModeId = TimerDisplayMode.fromId(displayModeId).id();
        return state;
    }

    public long elapsedActiveMs(long realtimeMs) {
        long elapsed = activeBeforeSegmentMs;
        if (active && !paused) {
            elapsed += Math.max(0L, realtimeMs - segmentStartedRealtimeMs);
        }
        return Math.min(durationMs, Math.max(0L, elapsed));
    }

    public long remainingMs(long realtimeMs) {
        return Math.max(0L, durationMs - elapsedActiveMs(realtimeMs));
    }

    public void setCueMode(boolean chimesEnabled, boolean vibrationEnabled) {
        this.chimesEnabled = chimesEnabled;
        this.vibrationEnabled = vibrationEnabled;
    }

    public void setDimScreen(boolean dimScreen) {
        this.dimScreen = dimScreen;
    }

    public void pause(long realtimeMs) {
        if (!active || paused) {
            return;
        }
        activeBeforeSegmentMs = elapsedActiveMs(realtimeMs);
        paused = true;
    }

    public void resume(long realtimeMs) {
        if (!active || !paused) {
            return;
        }
        segmentStartedRealtimeMs = realtimeMs;
        paused = false;
    }

    public void restart(long wallMs, long realtimeMs) {
        active = true;
        paused = false;
        startWallMs = wallMs;
        activeBeforeSegmentMs = 0L;
        segmentStartedRealtimeMs = realtimeMs;
        processedThroughActiveMs = 0L;
    }

    public long finish(long realtimeMs) {
        long elapsed = elapsedActiveMs(realtimeMs);
        activeBeforeSegmentMs = elapsed;
        active = false;
        paused = false;
        return elapsed;
    }
}
