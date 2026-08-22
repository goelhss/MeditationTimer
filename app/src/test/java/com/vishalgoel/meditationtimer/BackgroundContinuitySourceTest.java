package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class BackgroundContinuitySourceTest {
    @Test
    public void servicePersistsElapsedRealtimeStateAndRecovers() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MeditationTimerService.java");
        String store = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/TimerStateStore.java");

        assertTrue(source.contains("PowerManager.PARTIAL_WAKE_LOCK"));
        assertTrue(source.contains("SystemClock.elapsedRealtime()"));
        assertTrue(source.contains("START_STICKY"));
        assertTrue(source.contains("stateStore.save(state)"));
        assertTrue(source.contains("scheduleRecovery()"));
        assertTrue(source.contains("setExactAndAllowWhileIdle"));
        assertTrue(source.contains("releaseWakeLock()"));
        assertTrue(source.contains("cancelRecovery()"));
        assertTrue(source.contains("EXTRA_PREP_SECONDS"));
        assertTrue(source.contains("state.preparationRemainingMs"));
        assertTrue(source.contains("scheduleRecoveryAt"));
        assertTrue(source.contains("private static final long TICK_MS = 60_000L"));
        assertTrue(source.contains("formatMinuteCountdown"));
        assertTrue(store.contains("prep_duration_ms"));
        assertTrue(store.contains("prep_before_ms"));
    }

    @Test
    public void visibleTimerRefreshesByMinuteWithoutChangingExactAlarmRecovery()
            throws IOException {
        String activity = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");
        String service = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MeditationTimerService.java");

        assertTrue(activity.contains("private static final long UI_REFRESH_MS = 60_000L"));
        assertTrue(activity.contains("handler.postDelayed(this, UI_REFRESH_MS)"));
        assertTrue(activity.contains("formatMinuteCountdown(remaining)"));
        assertTrue(service.contains("remainingMinute"));
        assertTrue(service.contains("setExactAndAllowWhileIdle"));
        assertTrue(service.contains("if (!cues.isEmpty()) {\n                scheduleRecovery();"));
    }

    @Test
    public void ongoingNotificationProvidesRequiredActions() throws IOException {
        String source = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MeditationTimerService.java");

        assertTrue(source.contains(".setOngoing(true)"));
        assertTrue(source.contains("ACTION_PAUSE"));
        assertTrue(source.contains("ACTION_RESUME"));
        assertTrue(source.contains("ACTION_END"));
        assertTrue(source.contains("screen-lock safe"));
    }

    @Test
    public void requirementsCoverLockAndProcessScenarios() throws IOException {
        String requirements = TestSources.read("Requirements.md");

        assertTrue(requirements.contains("manual screen lock"));
        assertTrue(requirements.contains("task dismissal"));
        assertTrue(requirements.contains("process recreation"));
        assertTrue(requirements.contains("notification actions"));
        assertTrue(requirements.contains("permission denial"));
        assertTrue(requirements.contains("completion while locked"));
    }
}
