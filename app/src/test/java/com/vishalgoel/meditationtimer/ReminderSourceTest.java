package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class ReminderSourceTest {
    @Test
    public void reminderAlarmReschedulesAndOpensTimerTab() throws IOException {
        String scheduler = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/ReminderScheduler.java");
        String receiver = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/ReminderReceiver.java");

        assertTrue(scheduler.contains("setExactAndAllowWhileIdle"));
        assertTrue(scheduler.contains("alarms.cancel"));
        assertTrue(receiver.contains("new ReminderScheduler(context).apply(schedule)"));
        assertTrue(receiver.contains("MainActivity.EXTRA_OPEN_TAB"));
        assertTrue(receiver.contains("MainActivity.TAB_TIMER"));
        assertTrue(receiver.contains("Notification.CATEGORY_REMINDER"));
        assertTrue(receiver.contains("MeditationStats.streak"));
        assertTrue(receiver.contains("Grow old with a healthy soul. Meditate daily"));
    }

    @Test
    public void reminderUiSupportsRequestedFrequenciesAndPermissionTiming() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");

        assertTrue(source.contains("Meditation Reminder"));
        assertTrue(source.contains("Remind me to meditate"));
        assertTrue(source.contains("Save reminder"));
        assertTrue(source.contains("ReminderSchedule.Frequency.values()"));
        assertTrue(source.contains("requestNotificationPermissionIfNeeded()"));
    }
}
