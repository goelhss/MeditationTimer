package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

public final class ReminderStore {
    private static final String PREFS = "meditation_reminder";
    private final SharedPreferences preferences;
    private final BackupStatusStore backupStatus;

    public ReminderStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        backupStatus = new BackupStatusStore(context);
    }

    public ReminderSchedule load() {
        return new ReminderSchedule(
                preferences.getBoolean("enabled", false),
                ReminderSchedule.Frequency.fromId(preferences.getString("frequency", "daily")),
                preferences.getInt("hour", 8),
                preferences.getInt("minute", 0),
                preferences.getInt("custom_days", ReminderSchedule.WEEKDAYS_MASK));
    }

    public void save(ReminderSchedule schedule) {
        preferences.edit()
                .putBoolean("enabled", schedule.enabled())
                .putString("frequency", schedule.frequency().id())
                .putInt("hour", schedule.hour())
                .putInt("minute", schedule.minute())
                .putInt("custom_days", schedule.customDaysMask())
                .apply();
        backupStatus.markDirty();
    }
}
