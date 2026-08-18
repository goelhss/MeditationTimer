package com.vishalgoel.meditationtimer;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class TimerSchedule {
    public static final long MINUTE_MS = 60_000L;
    public static final int MAX_DURATION_MINUTES = 24 * 60;
    public static final int MAX_FINISH_DINGS = 30;

    public record Cue(long elapsedMs, int dingCount) {}

    private final long durationMs;
    private final long primaryMs;
    private final long additionalMs;
    private final int finishDings;

    public TimerSchedule(int durationMinutes, int primaryMinutes, int additionalMinutes,
                         int finishDings) {
        if (durationMinutes < 1 || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("Duration must be between 1 and 1440 minutes.");
        }
        if (primaryMinutes < 1 || additionalMinutes < 1) {
            throw new IllegalArgumentException("Ding frequencies must be at least 1 minute.");
        }
        if (finishDings < 1 || finishDings > MAX_FINISH_DINGS) {
            throw new IllegalArgumentException("Finish dings must be between 1 and 30.");
        }
        this.durationMs = durationMinutes * MINUTE_MS;
        this.primaryMs = primaryMinutes * MINUTE_MS;
        this.additionalMs = additionalMinutes * MINUTE_MS;
        this.finishDings = finishDings;
    }

    public long durationMs() {
        return durationMs;
    }

    public long primaryMs() {
        return primaryMs;
    }

    public long additionalMs() {
        return additionalMs;
    }

    public int finishDings() {
        return finishDings;
    }

    public int dingCountAt(long elapsedMs) {
        if (elapsedMs <= 0L || elapsedMs >= durationMs) {
            return 0;
        }
        int count = 0;
        if (elapsedMs % primaryMs == 0L) {
            count++;
        }
        if (elapsedMs % additionalMs == 0L) {
            count++;
        }
        return count;
    }

    public List<Cue> cuesBetween(long fromExclusiveMs, long toInclusiveMs) {
        long end = Math.min(toInclusiveMs, durationMs - 1L);
        if (end <= fromExclusiveMs) {
            return List.of();
        }
        TreeSet<Long> times = new TreeSet<>();
        addMultiples(times, primaryMs, fromExclusiveMs, end);
        addMultiples(times, additionalMs, fromExclusiveMs, end);
        List<Cue> result = new ArrayList<>(times.size());
        for (long time : times) {
            result.add(new Cue(time, dingCountAt(time)));
        }
        return result;
    }

    private static void addMultiples(TreeSet<Long> times, long intervalMs,
                                     long fromExclusiveMs, long endInclusiveMs) {
        long next = (Math.max(0L, fromExclusiveMs) / intervalMs + 1L) * intervalMs;
        while (next <= endInclusiveMs) {
            times.add(next);
            next += intervalMs;
        }
    }
}
