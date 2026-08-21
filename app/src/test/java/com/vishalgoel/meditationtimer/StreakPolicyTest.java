package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.Test;

public final class StreakPolicyTest {
    @Test
    public void rejectsUnboundedOrInvalidPersistedPauseData() {
        assertThrows(IllegalArgumentException.class, () -> new StreakSettings(
                true, true, true, 100L, 100L, List.of(),
                MeditationStats.NO_RESET_DAY, 1));
        assertThrows(IllegalArgumentException.class, () -> new StreakSettings(
                true, true, false, 0L, 0L,
                java.util.Collections.nCopies(StreakSettings.MAX_VACATION_WINDOWS + 1,
                        new MeditationStats.VacationWindow(1L, 2L)),
                MeditationStats.NO_RESET_DAY, 1));
    }

    @Test
    public void storeKeepsIndependentControlsAndThirtyDayLimit() throws IOException {
        String store = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/StreakStore.java");
        String backup = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/BackupCodec.java");

        assertTrue(store.contains("setCountingEnabled"));
        assertTrue(store.contains("setReminderEnabled"));
        assertTrue(store.contains("plusDays(30L)"));
        assertTrue(store.contains("NOTICE_RESET"));
        assertTrue(store.contains("Math.max(local.longestEver(), incoming.longestEver())"));
        assertTrue(backup.contains("vacationWindows"));
        assertTrue(backup.contains("longestEver"));
    }
}
