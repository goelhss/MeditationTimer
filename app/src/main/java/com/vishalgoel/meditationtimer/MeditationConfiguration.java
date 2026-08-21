package com.vishalgoel.meditationtimer;

public record MeditationConfiguration(
        int durationMinutes,
        int preparationSeconds,
        int primaryMinutes,
        int additionalMinutes,
        int finishDings,
        boolean chimes,
        boolean vibrate,
        boolean dim,
        String chimeSoundId,
        String timerDisplayId) {

    public MeditationConfiguration {
        new TimerSchedule(durationMinutes, primaryMinutes, additionalMinutes, finishDings);
        if (preparationSeconds < 0 || preparationSeconds > 3_600) {
            throw new IllegalArgumentException(
                    "Preparation time must be between 0 and 3600 seconds.");
        }
        if (!chimes && !vibrate) {
            throw new IllegalArgumentException("Enable Chimes, Vibrate, or both.");
        }
        chimeSoundId = ChimeSound.fromId(chimeSoundId).id();
        timerDisplayId = TimerDisplayMode.fromId(timerDisplayId).id();
    }
}
