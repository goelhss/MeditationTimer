package com.visgoe01.carappcommon;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class MindfulSplashTimingTest {
    @Test
    public void countdownRunsFiveToOneAtOneSecondTicks() {
        assertEquals(5, MindfulSplashTiming.countAtTick(0));
        assertEquals(4, MindfulSplashTiming.countAtTick(1));
        assertEquals(3, MindfulSplashTiming.countAtTick(2));
        assertEquals(2, MindfulSplashTiming.countAtTick(3));
        assertEquals(1, MindfulSplashTiming.countAtTick(4));
        assertEquals(1, MindfulSplashTiming.countAtTick(99));
        assertEquals(5000L, MindfulSplashTiming.HOLD_MS);
        assertEquals(3000L, MindfulSplashTiming.delayForTick(3));
    }
}
