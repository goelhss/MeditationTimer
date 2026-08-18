package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

public final class TimerScheduleTest {
    @Test
    public void defaultIntervalsProduceOneThenTwoDings() {
        TimerSchedule schedule = new TimerSchedule(60, 5, 10, 10);

        assertEquals(1, schedule.dingCountAt(5 * TimerSchedule.MINUTE_MS));
        assertEquals(2, schedule.dingCountAt(10 * TimerSchedule.MINUTE_MS));
        assertEquals(1, schedule.dingCountAt(15 * TimerSchedule.MINUTE_MS));
        assertEquals(2, schedule.dingCountAt(20 * TimerSchedule.MINUTE_MS));
    }

    @Test
    public void completionInstantUsesFinishDingsInsteadOfPeriodicDings() {
        TimerSchedule schedule = new TimerSchedule(60, 5, 10, 10);

        assertEquals(0, schedule.dingCountAt(60 * TimerSchedule.MINUTE_MS));
        assertEquals(10, schedule.finishDings());
    }

    @Test
    public void cuesBetweenCatchesCrossedIntervalsWithoutDuplicates() {
        TimerSchedule schedule = new TimerSchedule(30, 5, 10, 8);

        List<TimerSchedule.Cue> cues = schedule.cuesBetween(
                4 * TimerSchedule.MINUTE_MS + 59_900L,
                10 * TimerSchedule.MINUTE_MS + 100L);

        assertEquals(2, cues.size());
        assertEquals(new TimerSchedule.Cue(5 * TimerSchedule.MINUTE_MS, 1), cues.get(0));
        assertEquals(new TimerSchedule.Cue(10 * TimerSchedule.MINUTE_MS, 2), cues.get(1));
    }

    @Test
    public void validatesUserRanges() {
        assertThrows(IllegalArgumentException.class, () -> new TimerSchedule(0, 5, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> new TimerSchedule(60, 0, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> new TimerSchedule(60, 5, 10, 31));
    }
}
