package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class UiSourceTest {
    @Test
    public void keepsRequestedTabsAndTimerDefaults() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");

        assertTrue(source.contains("tabButton(\"Timer\""));
        assertTrue(source.contains("tabButton(\"Logs\""));
        assertTrue(source.contains("tabButton(\"Resolution\""));
        assertTrue(source.contains("tabButton(\"Reminder\""));
        assertTrue(source.contains("tabButton(\"Backup\""));
        assertTrue(source.contains("tabButton(\"About\""));
        assertFalse(source.contains("TAB_SETTINGS"));
        assertTrue(source.contains("getInt(\"duration\", 60)"));
        assertTrue(source.contains("getInt(\"primary\", 5)"));
        assertTrue(source.contains("getInt(\"additional\", 10)"));
        assertTrue(source.contains("getInt(\"finish\", 10)"));
        assertTrue(source.contains("getInt(\"prep_seconds\", 15)"));
        assertTrue(source.contains("getBoolean(\"dim\", true)"));
        assertTrue(source.contains("getBoolean(\"chimes\", true)"));
        assertTrue(source.contains("getBoolean(\"vibrate\", false)"));
        assertTrue(source.contains("Ding sound"));
        assertTrue(source.contains("chimeSoundSpinner"));
        assertTrue(source.contains("Timer display"));
        assertTrue(source.contains("timerDisplaySpinner"));
        assertTrue(source.contains("new AnalogTimerView"));
        assertTrue(source.contains("Live cues"));
        assertTrue(source.contains("sendCueMode"));
        assertTrue(source.contains("sendDimMode"));
        assertTrue(source.contains("optionCheckBox(\"Dim\", state.dimScreen)"));
        assertTrue(source.contains("Turn both cue switches off for silence"));
        assertTrue(source.contains("Elapsed \" + MeditationTimerService.formatCountdown"));
        assertTrue(source.contains("Well done."));
        assertTrue(source.contains("R.drawable.lotus_splash"));
        assertTrue(source.contains("renderResolution"));
        assertTrue(source.contains("Save resolution"));
    }

    @Test
    public void aboutContainsAppearanceUpdatesDiagnosticsAndLicense() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");

        assertTrue(source.contains("Background color"));
        assertTrue(source.contains("Check for Updates"));
        assertTrue(source.contains("What’s new in 1.6.0"));
        assertTrue(source.contains("Share Debug logs"));
        assertTrue(source.contains("View MIT License"));
        assertTrue(source.contains("LotusSplashView.create"));
        assertFalse(source.contains("MindfulSplashView.create"));
        String splash = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/LotusSplashView.java");
        assertTrue(splash.contains("R.drawable.lotus_splash"));
        assertFalse(splash.toLowerCase().contains("countdown"));
    }
}
