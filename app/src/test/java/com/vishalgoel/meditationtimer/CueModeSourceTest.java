package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class CueModeSourceTest {
    @Test
    public void intervalAndCompletionCuesUsePersistedModes() throws IOException {
        String service = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MeditationTimerService.java");
        String store = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/TimerStateStore.java");
        String player = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/ToneDingPlayer.java");

        assertTrue(service.contains("state.chimesEnabled, state.vibrationEnabled"));
        assertTrue(store.contains("chimes_enabled"));
        assertTrue(store.contains("vibration_enabled"));
        assertTrue(player.contains("VibrationEffect.createOneShot"));
        assertTrue(player.contains("if (chimesEnabled)"));
    }
}
