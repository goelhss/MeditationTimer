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
    public boolean preparing;
    public long prepDurationMs;
    public long prepBeforeSegmentMs;

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
        return start(schedule, wallMs, realtimeMs, dimScreen, chimesEnabled,
                vibrationEnabled, chimeSoundId, displayModeId, 0L);
    }

    public static TimerState start(TimerSchedule schedule, long wallMs, long realtimeMs,
                                   boolean dimScreen, boolean chimesEnabled,
                                   boolean vibrationEnabled, String chimeSoundId,
                                   String displayModeId, long prepDurationMs) {
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
        state.prepDurationMs = Math.max(0L, prepDurationMs);
        state.preparing = state.prepDurationMs > 0L;
        state.prepBeforeSegmentMs = 0L;
        state.startWallMs = wallMs + state.prepDurationMs;
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
        if (preparing) {
            return 0L;
        }
        long elapsed = activeBeforeSegmentMs;
        if (active && !paused) {
            elapsed += Math.max(0L, realtimeMs - segmentStartedRealtimeMs);
        }
        return Math.min(durationMs, Math.max(0L, elapsed));
    }

    public long remainingMs(long realtimeMs) {
        return Math.max(0L, durationMs - elapsedActiveMs(realtimeMs));
    }

    public long elapsedPreparationMs(long realtimeMs) {
        if (!preparing) {
            return prepDurationMs;
        }
        long elapsed = prepBeforeSegmentMs;
        if (active && !paused) {
            elapsed += Math.max(0L, realtimeMs - segmentStartedRealtimeMs);
        }
        return Math.min(prepDurationMs, Math.max(0L, elapsed));
    }

    public long preparationRemainingMs(long realtimeMs) {
        return preparing ? Math.max(0L, prepDurationMs - elapsedPreparationMs(realtimeMs)) : 0L;
    }

    public long totalRemainingMs(long realtimeMs) {
        return preparationRemainingMs(realtimeMs) + remainingMs(realtimeMs);
    }

    public void finishPreparation(long wallMs, long realtimeMs) {
        if (!active || paused || !preparing) {
            return;
        }
        long preparationEndRealtimeMs = segmentStartedRealtimeMs
                + Math.max(0L, prepDurationMs - prepBeforeSegmentMs);
        long overshootMs = Math.max(0L, realtimeMs - preparationEndRealtimeMs);
        preparing = false;
        prepBeforeSegmentMs = prepDurationMs;
        startWallMs = wallMs - overshootMs;
        activeBeforeSegmentMs = 0L;
        segmentStartedRealtimeMs = realtimeMs - overshootMs;
        processedThroughActiveMs = 0L;
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
        if (preparing) {
            prepBeforeSegmentMs = elapsedPreparationMs(realtimeMs);
        } else {
            activeBeforeSegmentMs = elapsedActiveMs(realtimeMs);
        }
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
        preparing = prepDurationMs > 0L;
        prepBeforeSegmentMs = 0L;
        startWallMs = wallMs + prepDurationMs;
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
