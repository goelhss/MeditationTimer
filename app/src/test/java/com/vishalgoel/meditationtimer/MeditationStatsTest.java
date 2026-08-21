package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.Test;

public final class MeditationStatsTest {
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Test
    public void weeklyReportBucketsDurationsSessionsAndDays() {
        long now = at("2026-08-21", 20);
        MeditationLog first = log("a", "2026-08-20", 8, 30);
        MeditationLog second = log("b", "2026-08-20", 19, 15);
        MeditationLog third = log("c", "2026-08-21", 7, 60);

        MeditationStats.Report report = MeditationStats.report(
                List.of(first, second, third), MeditationStats.Range.WEEKLY, now, UTC);

        assertEquals(7, report.buckets().size());
        assertEquals(105L * 60_000L, report.totalDurationMs());
        assertEquals(3, report.sessions());
        assertEquals(2, report.meditationDays());
        assertEquals(45L * 60_000L, report.buckets().get(5).durationMs());
        assertEquals(60L * 60_000L, report.buckets().get(6).durationMs());
    }

    @Test
    public void streakCountsOncePerDayAndAllowsTwoEmptyDays() {
        long now = at("2026-08-21", 20);
        List<MeditationLog> logs = List.of(
                log("a", "2026-08-15", 8, 10),
                log("b", "2026-08-15", 18, 20),
                log("c", "2026-08-18", 8, 30),
                log("d", "2026-08-21", 8, 30));

        MeditationStats.Streak streak = MeditationStats.streak(logs, now, UTC);

        assertEquals(3, streak.currentDays());
        assertEquals(3, streak.bestDays());
        assertTrue(streak.meditatedToday());
    }

    @Test
    public void streakResetsAfterThreeFullInactiveDays() {
        List<MeditationLog> logs = List.of(log("a", "2026-08-16", 8, 30));

        MeditationStats.Streak grace = MeditationStats.streak(
                logs, at("2026-08-19", 20), UTC);
        MeditationStats.Streak reset = MeditationStats.streak(
                logs, at("2026-08-20", 1), UTC);

        assertEquals(1, grace.currentDays());
        assertEquals(0, grace.graceDaysRemaining());
        assertFalse(grace.meditatedToday());
        assertEquals(0, reset.currentDays());
        assertEquals(1, reset.bestDays());
    }

    private static MeditationLog log(String id, String date, int hour, int minutes) {
        long start = at(date, hour);
        return new MeditationLog(id, start, start + minutes * 60_000L,
                minutes * 60_000L);
    }

    private static long at(String date, int hour) {
        return LocalDate.parse(date).atTime(LocalTime.of(hour, 0))
                .atZone(UTC).toInstant().toEpochMilli();
    }
}
