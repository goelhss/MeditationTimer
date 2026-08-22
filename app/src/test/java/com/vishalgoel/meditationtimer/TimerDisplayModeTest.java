package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class TimerDisplayModeTest {
    @Test
    public void defaultsToLargeDigitalAndRestoresAnalog() {
        assertEquals(TimerDisplayMode.DIGITAL, TimerDisplayMode.fromId(null));
        assertEquals(TimerDisplayMode.DIGITAL, TimerDisplayMode.fromId("missing"));
        assertEquals(TimerDisplayMode.ANALOG, TimerDisplayMode.fromId("analog"));
    }

    @Test
    public void analogShowsFullSessionWithElapsedAndRemainingColors() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/AnalogTimerView.java");

        assertTrue(source.contains("ELAPSED_COLOR = Color.rgb(78, 52, 108)"));
        assertTrue(source.contains("REMAINING_COLOR = Color.rgb(244, 180, 104)"));
        assertTrue(source.contains("canvas.drawArc(arc, -90f, 360f, false, paint)"));
        assertTrue(source.contains("360f * elapsedFraction"));
        assertTrue(source.contains("formatMinuteCountdown(remainingMs)"));
    }
}
