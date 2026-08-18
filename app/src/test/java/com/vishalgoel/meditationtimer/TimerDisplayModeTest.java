package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class TimerDisplayModeTest {
    @Test
    public void defaultsToLargeDigitalAndRestoresAnalog() {
        assertEquals(TimerDisplayMode.DIGITAL, TimerDisplayMode.fromId(null));
        assertEquals(TimerDisplayMode.DIGITAL, TimerDisplayMode.fromId("missing"));
        assertEquals(TimerDisplayMode.ANALOG, TimerDisplayMode.fromId("analog"));
    }
}
