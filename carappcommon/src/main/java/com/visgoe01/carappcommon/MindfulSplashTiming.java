package com.visgoe01.carappcommon;

public final class MindfulSplashTiming {
    public static final int START_COUNT = 5;
    public static final long TICK_MS = 1000L;
    public static final long HOLD_MS = START_COUNT * TICK_MS;

    private MindfulSplashTiming() {}

    public static int countAtTick(int tick) {
        if (tick <= 0) {
            return START_COUNT;
        }
        if (tick >= START_COUNT) {
            return 1;
        }
        return START_COUNT - tick;
    }

    public static long delayForTick(int tick) {
        if (tick <= 0) {
            return 0L;
        }
        return tick * TICK_MS;
    }
}
