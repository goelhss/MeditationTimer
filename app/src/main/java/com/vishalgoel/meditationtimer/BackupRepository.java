package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

public final class BackupRepository {
    private static final String SETTINGS_PREFS = "timer_settings";

    public record RestoreResult(int logsAdded, int resolutionsAdded) {}

    private final Context context;

    public BackupRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public BackupSnapshot snapshot(long generatedAtMs) {
        SharedPreferences settings = context.getSharedPreferences(SETTINGS_PREFS,
                Context.MODE_PRIVATE);
        MeditationConfigurationStore configurationStore =
                new MeditationConfigurationStore(context);
        MeditationConfiguration current = configurationStore.current();
        BackupSnapshot.TimerSettings timerSettings = new BackupSnapshot.TimerSettings(
                current.durationMinutes(), current.preparationSeconds(),
                current.primaryMinutes(), current.additionalMinutes(), current.finishDings(),
                current.chimes(), current.vibrate(), current.dim(), current.chimeSoundId(),
                current.timerDisplayId(),
                settings.getString("background_theme", AppColorTheme.DARK_PURPLE.id()),
                configurationStore.customSaved(), configurationStore.custom(),
                configurationStore.selectedPreset().id());
        return new BackupSnapshot(generatedAtMs,
                new MeditationLogStore(context).all(),
                new ResolutionStore(context).all(),
                timerSettings,
                new ReminderStore(context).load(),
                new StreakStore(context).backupSettings());
    }

    public RestoreResult restore(BackupSnapshot incoming) {
        MeditationLogStore logStore = new MeditationLogStore(context);
        ResolutionStore resolutionStore = new ResolutionStore(context);
        List<MeditationLog> oldLogs = logStore.all();
        List<Resolution> oldResolutions = resolutionStore.all();
        List<MeditationLog> mergedLogs = BackupMerger.mergeLogs(oldLogs, incoming.logs());
        List<Resolution> mergedResolutions = BackupMerger.mergeResolutions(
                oldResolutions, incoming.resolutions());
        logStore.replaceAll(mergedLogs);
        resolutionStore.replaceAll(mergedResolutions);

        BackupSnapshot.TimerSettings settings = incoming.settings();
        MeditationConfigurationStore configurationStore =
                new MeditationConfigurationStore(context);
        configurationStore.saveCurrent(new MeditationConfiguration(
                        settings.durationMinutes(), settings.preparationSeconds(),
                        settings.primaryMinutes(), settings.additionalMinutes(),
                        settings.finishDings(), settings.chimes(), settings.vibrate(),
                        settings.dim(), settings.chimeSoundId(), settings.timerDisplayId()),
                MeditationPreset.fromId(settings.selectedPresetId()));
        configurationStore.restoreCustom(settings.customConfiguration(),
                settings.customSaved(), settings.selectedPresetId());
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE).edit()
                .putString("background_theme", settings.backgroundThemeId())
                .apply();
        new ReminderStore(context).save(incoming.reminder());
        new ReminderScheduler(context).apply(incoming.reminder());
        new StreakStore(context).restoreMerged(incoming.streak());
        new BackupStatusStore(context).markDirty();
        return new RestoreResult(mergedLogs.size() - oldLogs.size(),
                mergedResolutions.size() - oldResolutions.size());
    }
}
