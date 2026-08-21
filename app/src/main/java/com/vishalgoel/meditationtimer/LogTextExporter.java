package com.vishalgoel.meditationtimer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LogTextExporter {
    private LogTextExporter() {}

    public static String export(List<MeditationLog> logs) {
        return export(logs, false, 0, 0);
    }

    public static String export(List<MeditationLog> logs, boolean streakCountingEnabled,
                                int currentStreak, int longestStreak) {
        StringBuilder text = new StringBuilder();
        text.append("Meditation Timer Logs\n");
        text.append("=====================\n\n");
        if (streakCountingEnabled) {
            text.append("Current streak: ").append(currentStreak).append(" days\n");
            text.append("Longest streak ever: ").append(longestStreak).append(" days\n\n");
        } else if (longestStreak > 0) {
            text.append("Streak counting: Off\n");
            text.append("Longest streak previously recorded: ").append(longestStreak)
                    .append(" days\n\n");
        }
        if (logs.isEmpty()) {
            text.append("No meditation sessions logged.\n");
            return text.toString();
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm:ss a z", Locale.US);
        int number = 1;
        for (MeditationLog log : logs) {
            text.append(number++).append(". Date/Time: ")
                    .append(format.format(new Date(log.startTimeMs()))).append('\n');
            text.append("   End: ").append(format.format(new Date(log.endTimeMs()))).append('\n');
            text.append("   Duration: ").append(formatDuration(log.durationMs())).append("\n\n");
        }
        return text.toString();
    }

    public static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, durationMs) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }
}
