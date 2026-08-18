package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.junit.Test;

public final class ReminderScheduleTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Test
    public void dailyUsesTodayBeforeTimeAndTomorrowAtTime() {
        ReminderSchedule schedule = new ReminderSchedule(true,
                ReminderSchedule.Frequency.DAILY, 8, 30, 0);

        assertEquals(at(2026, Calendar.AUGUST, 18, 8, 30),
                schedule.nextTriggerAfter(at(2026, Calendar.AUGUST, 18, 7, 0), UTC));
        assertEquals(at(2026, Calendar.AUGUST, 19, 8, 30),
                schedule.nextTriggerAfter(at(2026, Calendar.AUGUST, 18, 8, 30), UTC));
    }

    @Test
    public void weekdayScheduleSkipsWeekend() {
        ReminderSchedule schedule = new ReminderSchedule(true,
                ReminderSchedule.Frequency.WEEKDAYS, 8, 0, 0);

        assertEquals(at(2026, Calendar.AUGUST, 24, 8, 0),
                schedule.nextTriggerAfter(at(2026, Calendar.AUGUST, 21, 9, 0), UTC));
    }

    @Test
    public void weekendScheduleSkipsWeekdays() {
        ReminderSchedule schedule = new ReminderSchedule(true,
                ReminderSchedule.Frequency.WEEKENDS, 9, 0, 0);

        assertEquals(at(2026, Calendar.AUGUST, 22, 9, 0),
                schedule.nextTriggerAfter(at(2026, Calendar.AUGUST, 17, 10, 0), UTC));
    }

    @Test
    public void selectedDaysRequireAtLeastOneDay() {
        assertThrows(IllegalArgumentException.class, () -> new ReminderSchedule(true,
                ReminderSchedule.Frequency.CUSTOM, 8, 0, 0));

        int wednesday = ReminderSchedule.dayBit(Calendar.WEDNESDAY);
        ReminderSchedule schedule = new ReminderSchedule(true,
                ReminderSchedule.Frequency.CUSTOM, 19, 15, wednesday);
        assertEquals(at(2026, Calendar.AUGUST, 19, 19, 15),
                schedule.nextTriggerAfter(at(2026, Calendar.AUGUST, 18, 20, 0), UTC));
    }

    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar calendar = new GregorianCalendar(UTC);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }
}
