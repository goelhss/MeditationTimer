package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class LogTextExporterTest {
    @Test
    public void exportContainsDateAndActiveDuration() {
        String text = LogTextExporter.export(List.of(
                new MeditationLog("one", 0L, 3_661_000L, 3_661_000L)));

        assertTrue(text.startsWith("Meditation Timer Logs"));
        assertTrue(text.contains("Date/Time:"));
        assertTrue(text.contains("Duration: 1:01:01"));
    }

    @Test
    public void formatsShortDurations() {
        assertEquals("0:00", LogTextExporter.formatDuration(999L));
        assertEquals("5:07", LogTextExporter.formatDuration(307_000L));
    }
}
