package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public final class UiSourceTest {
    @Test
    public void keepsRequestedTabsAndTimerDefaults() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");
        String configurationStore = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MeditationConfigurationStore.java");

        assertTrue(source.contains("tabButton(\"Timer\""));
        assertTrue(source.contains("tabButton(\"Logs\""));
        assertTrue(source.contains("tabButton(\"Stats\""));
        assertTrue(source.contains("tabButton(\"Resolution\""));
        assertTrue(source.contains("tabButton(\"Reminder\""));
        assertTrue(source.contains("tabButton(\"Backup\""));
        assertTrue(source.contains("tabButton(\"About\""));
        assertFalse(source.contains("TAB_SETTINGS"));
        assertTrue(configurationStore.contains("getInt(prefix + \"duration\", 60)"));
        assertTrue(configurationStore.contains("getInt(prefix + \"primary\", 5)"));
        assertTrue(configurationStore.contains("getInt(prefix + \"additional\", 10)"));
        assertTrue(configurationStore.contains("getInt(prefix + \"finish\", 10)"));
        assertTrue(configurationStore.contains("getInt(prefix + \"prep_seconds\", 15)"));
        assertTrue(configurationStore.contains("getBoolean(prefix + \"dim\", true)"));
        assertTrue(configurationStore.contains("getBoolean(prefix + \"chimes\", true)"));
        assertTrue(configurationStore.contains("getBoolean(prefix + \"vibrate\", false)"));
        assertTrue(source.contains("labeledControl(\"Today\", preset)"));
        assertTrue(source.contains("Save as Custom"));
        assertTrue(source.contains("configurationStore.saveCustom"));
        assertTrue(source.contains("Ding sound"));
        assertTrue(source.contains("chimeSoundSpinner"));
        assertTrue(source.contains("Timer style"));
        assertTrue(source.contains("timerDisplaySpinner"));
        assertTrue(source.contains("new AnalogTimerView"));
        assertTrue(source.contains("countdownView.setTextSize(96)"));
        assertTrue(source.contains("Time left"));
        assertTrue(source.contains("sendCueMode"));
        assertTrue(source.contains("sendDimMode"));
        assertTrue(source.contains("optionCheckBox(\"Dim\", state.dimScreen)"));
        assertFalse(source.contains("Live cues"));
        assertFalse(source.contains("Turn both cue switches off for silence"));
        assertFalse(source.contains("Elapsed \" + MeditationTimerService.formatCountdown"));
        assertFalse(source.contains("Primary ding every"));
        assertFalse(source.contains("Google receives one private app-data JSON file"));
        assertFalse(source.contains("Portable JSON file"));
        assertTrue(source.contains("Well done."));
        assertTrue(source.contains("R.drawable.lotus_ocean_13_petals"));
        assertTrue(source.contains("renderResolution"));
        assertTrue(source.contains("Save resolution"));
        assertTrue(source.contains("renderStats"));
        assertTrue(source.contains("StatsChartView"));
        assertTrue(source.contains("new StreakStore(this).snapshot"));
        assertTrue(source.contains("Count meditation streaks"));
        assertTrue(source.contains("Use streak encouragement in reminders"));
        assertTrue(source.contains("Pause streak — going on vacation"));
        assertTrue(source.contains("Streak reset to 1. Good luck this time."));
        assertTrue(source.contains(" · Best "));
        assertTrue(source.contains("updateSelectionActions.run()"));
    }

    @Test
    public void aboutContainsAppearanceUpdatesDiagnosticsAndLicense() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");

        assertTrue(source.contains("bodyText(\"Background\")"));
        assertTrue(source.contains("Check for Updates"));
        assertTrue(source.contains("What’s new in 1.8.0"));
        assertTrue(source.contains("Share Debug logs"));
        assertTrue(source.contains("View change-log"));
        assertTrue(source.contains("View MIT License"));
        assertTrue(source.contains("LotusSplashView.create"));
        assertFalse(source.contains("MindfulSplashView.create"));
        String splash = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/LotusSplashView.java");
        assertTrue(splash.contains("R.drawable.lotus_ocean_13_petals"));
        assertFalse(splash.toLowerCase().contains("countdown"));

        String artwork = TestSources.read(
                "docs/assets/meditation-timer-purple-lotus-ocean-13-petals.svg");
        Matcher numberedPetals = Pattern.compile("<!-- \\d+: ").matcher(artwork);
        int petalCount = 0;
        while (numberedPetals.find()) {
            petalCount++;
        }
        assertEquals(13, petalCount);
    }
}
