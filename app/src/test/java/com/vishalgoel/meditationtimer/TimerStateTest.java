package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TimerStateTest {
    @Test
    public void pauseExcludesPausedTimeAndResumeContinues() {
        TimerState state = TimerState.start(new TimerSchedule(60, 5, 10, 10),
                1_000L, 10_000L, true);

        state.pause(25_000L);
        assertTrue(state.paused);
        assertEquals(15_000L, state.elapsedActiveMs(45_000L));

        state.resume(50_000L);
        assertFalse(state.paused);
        assertEquals(22_000L, state.elapsedActiveMs(57_000L));
    }

    @Test
    public void restartResetsElapsedAndStartTime() {
        TimerState state = TimerState.start(new TimerSchedule(60, 5, 10, 10),
                1_000L, 10_000L, false);
        state.pause(40_000L);

        state.restart(8_000L, 100_000L);

        assertEquals(0L, state.elapsedActiveMs(100_000L));
        assertEquals(8_000L, state.startWallMs);
        assertFalse(state.paused);
    }

    @Test
    public void realtimeRollbackNeverCreatesNegativeElapsed() {
        TimerState state = TimerState.start(new TimerSchedule(60, 5, 10, 10),
                1_000L, 10_000L, true);

        assertEquals(0L, state.elapsedActiveMs(5_000L));
    }
}
