package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StreakStore {
    private static final String PREFS = "streak_settings";
    public static final String NOTICE_NONE = "";
    public static final String NOTICE_RESUMED = "resumed";
    public static final String NOTICE_RESET = "reset";
    private final SharedPreferences preferences;
    private final BackupStatusStore backupStatus;

    public record Snapshot(boolean countingEnabled, boolean reminderEnabled, boolean paused,
                           long pauseUntilMs, MeditationStats.Streak streak) {}

    public StreakStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        backupStatus = new BackupStatusStore(context);
    }

    public StreakSettings load() {
        StreakSettings defaults = StreakSettings.defaults();
        return new StreakSettings(
                preferences.getBoolean("counting_enabled", defaults.countingEnabled()),
                preferences.getBoolean("reminder_enabled", defaults.reminderEnabled()),
                preferences.getBoolean("paused", false),
                preferences.getLong("pause_started", 0L),
                preferences.getLong("pause_until", 0L),
                decodeWindows(preferences.getString("vacation_windows", "")),
                preferences.getLong("reset_epoch_day", MeditationStats.NO_RESET_DAY),
                preferences.getInt("longest_ever", 0));
    }

    public Snapshot snapshot(List<MeditationLog> logs, long nowMs, ZoneId zone) {
        StreakSettings settings = load();
        if (!settings.countingEnabled()) {
            return new Snapshot(false, settings.reminderEnabled(), false, 0L,
                    new MeditationStats.Streak(0, settings.longestEver(), -1, 0, false));
        }
        List<MeditationStats.VacationWindow> windows = new ArrayList<>(
                settings.vacationWindows());
        boolean pauseActive = settings.paused() && nowMs <= settings.pauseUntilMs();
        if (pauseActive) {
            LocalDate start = day(settings.pauseStartedMs(), zone);
            LocalDate throughToday = day(nowMs, zone).plusDays(1L);
            if (throughToday.isAfter(start)) {
                windows.add(new MeditationStats.VacationWindow(
                        start.toEpochDay(), throughToday.toEpochDay()));
            }
        }
        MeditationStats.Streak calculated = MeditationStats.streak(logs, nowMs, zone,
                windows, settings.resetEpochDay());
        int longest = Math.max(settings.longestEver(), calculated.bestDays());
        if (longest != settings.longestEver()) {
            save(new StreakSettings(settings.countingEnabled(), settings.reminderEnabled(),
                    settings.paused(), settings.pauseStartedMs(), settings.pauseUntilMs(),
                    settings.vacationWindows(), settings.resetEpochDay(), longest), false);
        }
        return new Snapshot(true, settings.reminderEnabled(), pauseActive,
                pauseActive ? settings.pauseUntilMs() : 0L, new MeditationStats.Streak(
                calculated.currentDays(), longest, calculated.daysSinceLastMeditation(),
                calculated.graceDaysRemaining(), calculated.meditatedToday()));
    }

    public void setCountingEnabled(boolean enabled) {
        StreakSettings current = load();
        save(new StreakSettings(enabled, current.reminderEnabled(),
                enabled && current.paused(), enabled ? current.pauseStartedMs() : 0L,
                enabled ? current.pauseUntilMs() : 0L, current.vacationWindows(),
                current.resetEpochDay(), current.longestEver()), true);
    }

    public void setReminderEnabled(boolean enabled) {
        StreakSettings current = load();
        save(new StreakSettings(current.countingEnabled(), enabled, current.paused(),
                current.pauseStartedMs(), current.pauseUntilMs(), current.vacationWindows(),
                current.resetEpochDay(), current.longestEver()), true);
    }

    public void pause(long nowMs, ZoneId zone) {
        StreakSettings current = load();
        if (!current.countingEnabled()) {
            throw new IllegalStateException("Turn on streak counting before pausing it.");
        }
        long until = Instant.ofEpochMilli(nowMs).atZone(zone).plusDays(30L)
                .toInstant().toEpochMilli();
        save(new StreakSettings(true, current.reminderEnabled(), true, nowMs, until,
                current.vacationWindows(), current.resetEpochDay(), current.longestEver()), true);
        preferences.edit().putString("notice", NOTICE_NONE).apply();
    }

    public void resumeNow(long nowMs, ZoneId zone) {
        finishPause(nowMs, zone);
    }

    public boolean onAppOpened(long nowMs, ZoneId zone) {
        if (load().paused()) {
            finishPause(nowMs, zone);
            return true;
        }
        return false;
    }

    public String consumeNotice() {
        String notice = preferences.getString("notice", NOTICE_NONE);
        if (notice == null || notice.isBlank()) {
            return NOTICE_NONE;
        }
        preferences.edit().putString("notice", NOTICE_NONE).apply();
        return notice;
    }

    public StreakSettings backupSettings() {
        return load();
    }

    public void restoreMerged(StreakSettings incoming) {
        StreakSettings local = load();
        Set<MeditationStats.VacationWindow> windows = new LinkedHashSet<>(
                local.vacationWindows());
        windows.addAll(incoming.vacationWindows());
        List<MeditationStats.VacationWindow> merged = new ArrayList<>(windows);
        if (merged.size() > StreakSettings.MAX_VACATION_WINDOWS) {
            merged = merged.subList(merged.size() - StreakSettings.MAX_VACATION_WINDOWS,
                    merged.size());
        }
        boolean useIncomingPause = incoming.paused()
                && (!local.paused() || incoming.pauseStartedMs() >= local.pauseStartedMs());
        save(new StreakSettings(incoming.countingEnabled(), incoming.reminderEnabled(),
                useIncomingPause ? incoming.paused() : local.paused(),
                useIncomingPause ? incoming.pauseStartedMs() : local.pauseStartedMs(),
                useIncomingPause ? incoming.pauseUntilMs() : local.pauseUntilMs(), merged,
                Math.max(local.resetEpochDay(), incoming.resetEpochDay()),
                Math.max(local.longestEver(), incoming.longestEver())), true);
    }

    private void finishPause(long nowMs, ZoneId zone) {
        StreakSettings current = load();
        if (!current.paused()) {
            return;
        }
        if (nowMs <= current.pauseUntilMs()) {
            List<MeditationStats.VacationWindow> windows = new ArrayList<>(
                    current.vacationWindows());
            LocalDate start = day(current.pauseStartedMs(), zone);
            LocalDate end = day(nowMs, zone).plusDays(1L);
            if (end.isAfter(start)) {
                windows.add(new MeditationStats.VacationWindow(
                        start.toEpochDay(), end.toEpochDay()));
            }
            save(new StreakSettings(current.countingEnabled(), current.reminderEnabled(),
                    false, 0L, 0L, windows, current.resetEpochDay(),
                    current.longestEver()), true);
            preferences.edit().putString("notice", NOTICE_RESUMED).apply();
        } else {
            long today = day(nowMs, zone).toEpochDay();
            save(new StreakSettings(current.countingEnabled(), current.reminderEnabled(),
                    false, 0L, 0L, current.vacationWindows(), today,
                    current.longestEver()), true);
            preferences.edit().putString("notice", NOTICE_RESET).apply();
        }
    }

    private void save(StreakSettings settings, boolean dirty) {
        preferences.edit()
                .putBoolean("counting_enabled", settings.countingEnabled())
                .putBoolean("reminder_enabled", settings.reminderEnabled())
                .putBoolean("paused", settings.paused())
                .putLong("pause_started", settings.pauseStartedMs())
                .putLong("pause_until", settings.pauseUntilMs())
                .putString("vacation_windows", encodeWindows(settings.vacationWindows()))
                .putLong("reset_epoch_day", settings.resetEpochDay())
                .putInt("longest_ever", settings.longestEver())
                .apply();
        if (dirty) {
            backupStatus.markDirty();
        }
    }

    private static LocalDate day(long wallMs, ZoneId zone) {
        return Instant.ofEpochMilli(wallMs).atZone(zone).toLocalDate();
    }

    private static String encodeWindows(List<MeditationStats.VacationWindow> windows) {
        StringBuilder encoded = new StringBuilder();
        for (MeditationStats.VacationWindow window : windows) {
            if (encoded.length() > 0) {
                encoded.append(',');
            }
            encoded.append(window.startEpochDay()).append(':')
                    .append(window.endEpochDayExclusive());
        }
        return encoded.toString();
    }

    private static List<MeditationStats.VacationWindow> decodeWindows(String encoded) {
        List<MeditationStats.VacationWindow> windows = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return windows;
        }
        for (String item : encoded.split(",")) {
            String[] values = item.split(":", -1);
            try {
                if (values.length == 2 && windows.size() < StreakSettings.MAX_VACATION_WINDOWS) {
                    windows.add(new MeditationStats.VacationWindow(
                            Long.parseLong(values[0]), Long.parseLong(values[1])));
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupt local pause metadata; meditation logs remain untouched.
            }
        }
        return windows;
    }
}
