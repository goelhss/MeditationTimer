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
        BackupSnapshot.TimerSettings timerSettings = new BackupSnapshot.TimerSettings(
                settings.getInt("duration", 60),
                settings.getInt("prep_seconds", 15),
                settings.getInt("primary", 5),
                settings.getInt("additional", 10),
                settings.getInt("finish", 10),
                settings.getBoolean("chimes", true),
                settings.getBoolean("vibrate", false),
                settings.getBoolean("dim", true),
                settings.getString("chime_sound", ChimeSound.DEFAULT.id()),
                settings.getString("timer_display", TimerDisplayMode.DEFAULT.id()),
                settings.getString("background_theme", AppColorTheme.DARK_PURPLE.id()));
        return new BackupSnapshot(generatedAtMs,
                new MeditationLogStore(context).all(),
                new ResolutionStore(context).all(),
                timerSettings,
                new ReminderStore(context).load());
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
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE).edit()
                .putInt("duration", settings.durationMinutes())
                .putInt("prep_seconds", settings.preparationSeconds())
                .putInt("primary", settings.primaryMinutes())
                .putInt("additional", settings.additionalMinutes())
                .putInt("finish", settings.finishDings())
                .putBoolean("chimes", settings.chimes())
                .putBoolean("vibrate", settings.vibrate())
                .putBoolean("dim", settings.dim())
                .putString("chime_sound", settings.chimeSoundId())
                .putString("timer_display", settings.timerDisplayId())
                .putString("background_theme", settings.backgroundThemeId())
                .apply();
        new ReminderStore(context).save(incoming.reminder());
        new ReminderScheduler(context).apply(incoming.reminder());
        new BackupStatusStore(context).markDirty();
        return new RestoreResult(mergedLogs.size() - oldLogs.size(),
                mergedResolutions.size() - oldResolutions.size());
    }
}
