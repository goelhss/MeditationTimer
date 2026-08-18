package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class UiSourceTest {
    @Test
    public void keepsRequestedThreeTabsAndTimerDefaults() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");

        assertTrue(source.contains("tabButton(\"Timer\""));
        assertTrue(source.contains("tabButton(\"Meditation Logs\""));
        assertTrue(source.contains("tabButton(\"About\""));
        assertFalse(source.contains("TAB_SETTINGS"));
        assertTrue(source.contains("getInt(\"duration\", 60)"));
        assertTrue(source.contains("getInt(\"primary\", 5)"));
        assertTrue(source.contains("getInt(\"additional\", 10)"));
        assertTrue(source.contains("getInt(\"finish\", 10)"));
        assertTrue(source.contains("getBoolean(\"dim\", true)"));
    }

    @Test
    public void aboutContainsAppearanceUpdatesDiagnosticsAndLicense() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");

        assertTrue(source.contains("Background color"));
        assertTrue(source.contains("Check for Updates"));
        assertTrue(source.contains("What’s new in 1.0.0"));
        assertTrue(source.contains("Share Debug logs"));
        assertTrue(source.contains("View MIT License"));
        assertTrue(source.contains("MindfulSplashView.create"));
    }
}
