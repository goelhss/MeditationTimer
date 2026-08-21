package com.vishalgoel.meditationtimer;

import java.util.List;

public record StreakSettings(
        boolean countingEnabled,
        boolean reminderEnabled,
        boolean paused,
        long pauseStartedMs,
        long pauseUntilMs,
        List<MeditationStats.VacationWindow> vacationWindows,
        long resetEpochDay,
        int longestEver) {

    public static final int MAX_VACATION_WINDOWS = 48;

    public StreakSettings {
        vacationWindows = vacationWindows == null ? List.of() : List.copyOf(vacationWindows);
        if (vacationWindows.size() > MAX_VACATION_WINDOWS) {
            throw new IllegalArgumentException("The backup contains too many streak pauses.");
        }
        if (paused && (pauseStartedMs <= 0L || pauseUntilMs <= pauseStartedMs)) {
            throw new IllegalArgumentException("The streak pause is invalid.");
        }
        if (longestEver < 0 || longestEver > 1_000_000) {
            throw new IllegalArgumentException("The longest streak is invalid.");
        }
    }

    public static StreakSettings defaults() {
        return new StreakSettings(true, true, false, 0L, 0L, List.of(),
                MeditationStats.NO_RESET_DAY, 0);
    }
}
