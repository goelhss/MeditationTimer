package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

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

    @Test
    public void cueModeIsStoredAndSilentModeIsRejected() {
        TimerSchedule schedule = new TimerSchedule(60, 5, 10, 10);
        TimerState vibrationOnly = TimerState.start(schedule, 1_000L, 10_000L,
                true, false, true);

        assertFalse(vibrationOnly.chimesEnabled);
        assertTrue(vibrationOnly.vibrationEnabled);
        assertEquals(ChimeSound.TEMPLE_BELL.id(), vibrationOnly.chimeSoundId);
        assertEquals(TimerDisplayMode.DIGITAL.id(), vibrationOnly.displayModeId);
        TimerState bowl = TimerState.start(schedule, 1_000L, 10_000L,
                true, true, false, ChimeSound.SINGING_BOWL.id(),
                TimerDisplayMode.ANALOG.id());
        assertEquals(ChimeSound.SINGING_BOWL.id(), bowl.chimeSoundId);
        assertEquals(TimerDisplayMode.ANALOG.id(), bowl.displayModeId);
        bowl.setCueMode(false, false);
        assertFalse(bowl.chimesEnabled);
        assertFalse(bowl.vibrationEnabled);
        bowl.setDimScreen(false);
        assertFalse(bowl.dimScreen);
        assertThrows(IllegalArgumentException.class, () ->
                TimerState.start(schedule, 1_000L, 10_000L, true, false, false));
    }

    @Test
    public void preparationTimePrecedesAndIsExcludedFromMeditation() {
        TimerState state = TimerState.start(new TimerSchedule(60, 5, 10, 10),
                1_000L, 10_000L, true, true, false,
                ChimeSound.MEDITATION_BOWL.id(), TimerDisplayMode.DIGITAL.id(), 15_000L);

        assertTrue(state.preparing);
        assertEquals(15_000L, state.preparationRemainingMs(10_000L));
        assertEquals(0L, state.elapsedActiveMs(20_000L));

        state.finishPreparation(17_000L, 26_000L);

        assertFalse(state.preparing);
        assertEquals(16_000L, state.startWallMs);
        assertEquals(1_000L, state.elapsedActiveMs(26_000L));
    }

    @Test
    public void preparationCanPauseResumeAndRestart() {
        TimerState state = TimerState.start(new TimerSchedule(60, 5, 10, 10),
                1_000L, 10_000L, true, true, false,
                ChimeSound.DEFAULT.id(), TimerDisplayMode.DEFAULT.id(), 15_000L);
        state.pause(15_000L);
        assertEquals(10_000L, state.preparationRemainingMs(40_000L));
        state.resume(50_000L);
        assertEquals(5_000L, state.preparationRemainingMs(55_000L));

        state.restart(80_000L, 90_000L);
        assertTrue(state.preparing);
        assertEquals(15_000L, state.preparationRemainingMs(90_000L));
        assertEquals(95_000L, state.startWallMs);
    }
}
