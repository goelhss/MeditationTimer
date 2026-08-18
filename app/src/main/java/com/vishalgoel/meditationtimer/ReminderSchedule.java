package com.vishalgoel.meditationtimer;

import java.util.Calendar;
import java.util.TimeZone;

public final class ReminderSchedule {
    public enum Frequency {
        DAILY("daily", "Daily"),
        WEEKDAYS("weekdays", "Weekdays"),
        WEEKENDS("weekends", "Weekends"),
        CUSTOM("custom", "Selected days");

        private final String id;
        private final String label;

        Frequency(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public static Frequency fromId(String id) {
            for (Frequency value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return DAILY;
        }
    }

    public static final int WEEKDAYS_MASK = dayBit(Calendar.MONDAY)
            | dayBit(Calendar.TUESDAY) | dayBit(Calendar.WEDNESDAY)
            | dayBit(Calendar.THURSDAY) | dayBit(Calendar.FRIDAY);
    public static final int WEEKENDS_MASK = dayBit(Calendar.SATURDAY)
            | dayBit(Calendar.SUNDAY);
    public static final int EVERY_DAY_MASK = WEEKDAYS_MASK | WEEKENDS_MASK;

    private final boolean enabled;
    private final Frequency frequency;
    private final int hour;
    private final int minute;
    private final int customDaysMask;

    public ReminderSchedule(boolean enabled, Frequency frequency, int hour, int minute,
                            int customDaysMask) {
        if (frequency == null) {
            throw new IllegalArgumentException("Choose how often to be reminded.");
        }
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Choose a valid reminder time.");
        }
        if (enabled && frequency == Frequency.CUSTOM && (customDaysMask & EVERY_DAY_MASK) == 0) {
            throw new IllegalArgumentException("Select at least one reminder day.");
        }
        this.enabled = enabled;
        this.frequency = frequency;
        this.hour = hour;
        this.minute = minute;
        this.customDaysMask = customDaysMask & EVERY_DAY_MASK;
    }

    public boolean enabled() {
        return enabled;
    }

    public Frequency frequency() {
        return frequency;
    }

    public int hour() {
        return hour;
    }

    public int minute() {
        return minute;
    }

    public int customDaysMask() {
        return customDaysMask;
    }

    public int effectiveDaysMask() {
        if (frequency == Frequency.WEEKDAYS) {
            return WEEKDAYS_MASK;
        }
        if (frequency == Frequency.WEEKENDS) {
            return WEEKENDS_MASK;
        }
        if (frequency == Frequency.CUSTOM) {
            return customDaysMask;
        }
        return EVERY_DAY_MASK;
    }

    public long nextTriggerAfter(long wallTimeMs, TimeZone timeZone) {
        if (!enabled) {
            return 0L;
        }
        Calendar candidate = Calendar.getInstance(timeZone);
        candidate.setTimeInMillis(wallTimeMs);
        candidate.set(Calendar.HOUR_OF_DAY, hour);
        candidate.set(Calendar.MINUTE, minute);
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);
        int eligibleDays = effectiveDaysMask();
        for (int offset = 0; offset <= 7; offset += 1) {
            if ((eligibleDays & dayBit(candidate.get(Calendar.DAY_OF_WEEK))) != 0
                    && candidate.getTimeInMillis() > wallTimeMs) {
                return candidate.getTimeInMillis();
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1);
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);
        }
        throw new IllegalStateException("Could not find the next reminder day.");
    }

    public static int dayBit(int calendarDay) {
        return 1 << calendarDay;
    }
}
