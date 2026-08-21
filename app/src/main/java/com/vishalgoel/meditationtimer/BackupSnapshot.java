package com.vishalgoel.meditationtimer;

import java.util.List;

public record BackupSnapshot(
        long generatedAtMs,
        List<MeditationLog> logs,
        List<Resolution> resolutions,
        TimerSettings settings,
        ReminderSchedule reminder) {

    public record TimerSettings(
            int durationMinutes,
            int preparationSeconds,
            int primaryMinutes,
            int additionalMinutes,
            int finishDings,
            boolean chimes,
            boolean vibrate,
            boolean dim,
            String chimeSoundId,
            String timerDisplayId,
            String backgroundThemeId,
            boolean customSaved,
            MeditationConfiguration customConfiguration,
            String selectedPresetId) {

        public TimerSettings {
            new TimerSchedule(durationMinutes, primaryMinutes, additionalMinutes, finishDings);
            if (preparationSeconds < 0 || preparationSeconds > 3_600) {
                throw new IllegalArgumentException("Preparation time must be between 0 and 3600 seconds.");
            }
            if (!chimes && !vibrate) {
                throw new IllegalArgumentException("A backup must keep Chimes, Vibrate, or both enabled.");
            }
            chimeSoundId = ChimeSound.fromId(chimeSoundId).id();
            timerDisplayId = TimerDisplayMode.fromId(timerDisplayId).id();
            backgroundThemeId = AppColorTheme.fromId(backgroundThemeId).id();
            customConfiguration = customConfiguration == null
                    ? new MeditationConfiguration(durationMinutes, preparationSeconds,
                    primaryMinutes, additionalMinutes, finishDings, chimes, vibrate, dim,
                    chimeSoundId, timerDisplayId) : customConfiguration;
            selectedPresetId = MeditationPreset.fromId(selectedPresetId).id();
        }
    }
}
