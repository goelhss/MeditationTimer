package com.vishalgoel.meditationtimer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class MeditationStats {
    public enum Range {
        DAILY("daily", "Daily"),
        WEEKLY("weekly", "Weekly"),
        MONTHLY("monthly", "Monthly"),
        THREE_MONTHS("three_months", "3 months"),
        SIX_MONTHS("six_months", "6 months"),
        FULL_YEAR("full_year", "Full year");

        private final String id;
        private final String label;

        Range(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String id() { return id; }
        public String label() { return label; }

        public static Range fromId(String id) {
            for (Range range : values()) {
                if (range.id.equals(id)) {
                    return range;
                }
            }
            return WEEKLY;
        }

        @Override
        public String toString() { return label; }
    }

    public record Bucket(String label, long durationMs, int sessions) {}
    public record Report(Range range, List<Bucket> buckets, long totalDurationMs,
                         int sessions, int meditationDays) {}
    public record Streak(int currentDays, int bestDays, int daysSinceLastMeditation,
                         int graceDaysRemaining, boolean meditatedToday) {}
    private record Window(long startMs, long endMs, String label) {}

    private MeditationStats() {}

    public static Report report(List<MeditationLog> logs, Range range, long nowMs,
                                ZoneId zone) {
        List<Window> windows = windows(range, nowMs, zone);
        long[] durations = new long[windows.size()];
        int[] sessions = new int[windows.size()];
        Set<LocalDate> days = new TreeSet<>();
        long totalDuration = 0L;
        int totalSessions = 0;
        List<MeditationLog> safeLogs = logs == null ? List.of() : logs;
        for (MeditationLog log : safeLogs) {
            if (log == null || log.durationMs() <= 0L || log.startTimeMs() > nowMs) {
                continue;
            }
            for (int index = 0; index < windows.size(); index += 1) {
                Window window = windows.get(index);
                if (log.startTimeMs() >= window.startMs()
                        && log.startTimeMs() < window.endMs()) {
                    durations[index] += log.durationMs();
                    sessions[index] += 1;
                    totalDuration += log.durationMs();
                    totalSessions += 1;
                    days.add(Instant.ofEpochMilli(log.startTimeMs()).atZone(zone).toLocalDate());
                    break;
                }
            }
        }
        List<Bucket> buckets = new ArrayList<>();
        for (int index = 0; index < windows.size(); index += 1) {
            buckets.add(new Bucket(windows.get(index).label(), durations[index], sessions[index]));
        }
        return new Report(range, List.copyOf(buckets), totalDuration, totalSessions, days.size());
    }

    public static Streak streak(List<MeditationLog> logs, long nowMs, ZoneId zone) {
        LocalDate today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate();
        TreeSet<LocalDate> meditationDays = new TreeSet<>();
        for (MeditationLog log : logs == null ? List.<MeditationLog>of() : logs) {
            if (log != null && log.durationMs() > 0L && log.startTimeMs() <= nowMs) {
                meditationDays.add(Instant.ofEpochMilli(log.startTimeMs())
                        .atZone(zone).toLocalDate());
            }
        }
        if (meditationDays.isEmpty()) {
            return new Streak(0, 0, -1, 0, false);
        }

        int run = 0;
        int best = 0;
        LocalDate previous = null;
        for (LocalDate day : meditationDays) {
            if (previous == null || ChronoUnit.DAYS.between(previous, day) > 3L) {
                run = 1;
            } else {
                run += 1;
            }
            best = Math.max(best, run);
            previous = day;
        }
        int daysSince = (int) Math.max(0L, ChronoUnit.DAYS.between(previous, today));
        boolean todayDone = daysSince == 0;
        if (daysSince > 3) {
            return new Streak(0, best, daysSince, 0, false);
        }
        return new Streak(run, best, daysSince, Math.max(0, 3 - daysSince), todayDone);
    }

    private static List<Window> windows(Range range, long nowMs, ZoneId zone) {
        ZonedDateTime now = Instant.ofEpochMilli(nowMs).atZone(zone);
        LocalDate today = now.toLocalDate();
        List<Window> result = new ArrayList<>();
        DateTimeFormatter day = DateTimeFormatter.ofPattern("EEE", Locale.US);
        DateTimeFormatter date = DateTimeFormatter.ofPattern("M/d", Locale.US);
        DateTimeFormatter month = DateTimeFormatter.ofPattern("MMM", Locale.US);
        if (range == Range.DAILY) {
            ZonedDateTime start = today.atStartOfDay(zone);
            DateTimeFormatter hour = DateTimeFormatter.ofPattern("ha", Locale.US);
            for (int index = 0; index < 6; index += 1) {
                ZonedDateTime from = start.plusHours(index * 4L);
                result.add(new Window(from.toInstant().toEpochMilli(),
                        from.plusHours(4).toInstant().toEpochMilli(),
                        from.format(hour).toLowerCase(Locale.US)));
            }
        } else if (range == Range.WEEKLY) {
            LocalDate start = today.minusDays(6);
            addDayWindows(result, start, 7, 1, zone, day);
        } else if (range == Range.MONTHLY) {
            LocalDate start = today.minusDays(29);
            addDayWindows(result, start, 6, 5, zone, date);
        } else if (range == Range.THREE_MONTHS) {
            LocalDate start = today.minusDays(90);
            addDayWindows(result, start, 13, 7, zone, date);
        } else {
            int months = range == Range.SIX_MONTHS ? 6 : 12;
            LocalDate start = today.withDayOfMonth(1).minusMonths(months - 1L);
            for (int index = 0; index < months; index += 1) {
                LocalDate from = start.plusMonths(index);
                result.add(new Window(from.atStartOfDay(zone).toInstant().toEpochMilli(),
                        from.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                        from.format(month)));
            }
        }
        return result;
    }

    private static void addDayWindows(List<Window> result, LocalDate start, int count,
                                      int daysPerBucket, ZoneId zone,
                                      DateTimeFormatter labelFormat) {
        for (int index = 0; index < count; index += 1) {
            LocalDate from = start.plusDays((long) index * daysPerBucket);
            result.add(new Window(from.atStartOfDay(zone).toInstant().toEpochMilli(),
                    from.plusDays(daysPerBucket).atStartOfDay(zone).toInstant().toEpochMilli(),
                    from.format(labelFormat)));
        }
    }
}
