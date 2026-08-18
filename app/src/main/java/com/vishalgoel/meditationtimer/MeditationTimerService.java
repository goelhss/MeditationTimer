package com.vishalgoel.meditationtimer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import java.util.List;
import java.util.Locale;

public final class MeditationTimerService extends Service {
    public static final String ACTION_START = "com.vishalgoel.meditationtimer.START";
    public static final String ACTION_PAUSE = "com.vishalgoel.meditationtimer.PAUSE";
    public static final String ACTION_RESUME = "com.vishalgoel.meditationtimer.RESUME";
    public static final String ACTION_RESTART = "com.vishalgoel.meditationtimer.RESTART";
    public static final String ACTION_END = "com.vishalgoel.meditationtimer.END";
    public static final String ACTION_SET_CUES = "com.vishalgoel.meditationtimer.SET_CUES";
    public static final String ACTION_SET_DIM = "com.vishalgoel.meditationtimer.SET_DIM";
    public static final String ACTION_RECOVER = "com.vishalgoel.meditationtimer.RECOVER";
    public static final String ACTION_LOG_YES = "com.vishalgoel.meditationtimer.LOG_YES";
    public static final String ACTION_LOG_NO = "com.vishalgoel.meditationtimer.LOG_NO";
    public static final String ACTION_LOG_DEFAULT = "com.vishalgoel.meditationtimer.LOG_DEFAULT";
    public static final String EVENT_STATE_CHANGED = "com.vishalgoel.meditationtimer.STATE_CHANGED";
    public static final String EVENT_COMPLETED = "com.vishalgoel.meditationtimer.COMPLETED";

    public static final String EXTRA_DURATION_MINUTES = "duration_minutes";
    public static final String EXTRA_PRIMARY_MINUTES = "primary_minutes";
    public static final String EXTRA_ADDITIONAL_MINUTES = "additional_minutes";
    public static final String EXTRA_FINISH_DINGS = "finish_dings";
    public static final String EXTRA_DIM_SCREEN = "dim_screen";
    public static final String EXTRA_CHIMES_ENABLED = "chimes_enabled";
    public static final String EXTRA_VIBRATION_ENABLED = "vibration_enabled";
    public static final String EXTRA_CHIME_SOUND_ID = "chime_sound_id";
    public static final String EXTRA_DISPLAY_MODE_ID = "display_mode_id";

    private static final String CHANNEL_TIMER = "meditation_timer_running";
    private static final int NOTIFICATION_TIMER = 2101;
    private static final int RECOVERY_REQUEST = 2102;
    private static final int DEFAULT_LOG_REQUEST = 2103;
    private static final long TICK_MS = 250L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TimerStateStore stateStore;
    private DiagnosticsStore diagnostics;
    private ToneDingPlayer dingPlayer;
    private PowerManager.WakeLock wakeLock;
    private TimerState state;
    private boolean foreground;
    private long lastNotificationSecond = Long.MIN_VALUE;
    private long lastPersistRealtimeMs;
    private long completionSoundUntilRealtimeMs;

    private final Runnable defaultLogRunnable = () -> decidePendingLog(true);

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            processTick();
            if (state != null && state.active) {
                handler.postDelayed(this, state.paused ? 1000L : TICK_MS);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        stateStore = new TimerStateStore(this);
        diagnostics = new DiagnosticsStore(this);
        dingPlayer = new ToneDingPlayer(this, handler);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MeditationTimer:ActiveSession");
        wakeLock.setReferenceCounted(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_RECOVER : intent.getAction();
        if (ACTION_START.equals(action)) {
            startNew(intent);
        } else if (ACTION_PAUSE.equals(action)) {
            restoreState();
            pause();
        } else if (ACTION_RESUME.equals(action)) {
            restoreState();
            resume();
        } else if (ACTION_RESTART.equals(action)) {
            restoreState();
            restart();
        } else if (ACTION_END.equals(action)) {
            restoreState();
            finish(false);
        } else if (ACTION_SET_CUES.equals(action)) {
            restoreState();
            setCueMode(intent);
        } else if (ACTION_SET_DIM.equals(action)) {
            restoreState();
            setDimScreen(intent);
        } else if (ACTION_LOG_YES.equals(action) || ACTION_LOG_DEFAULT.equals(action)) {
            decidePendingLog(true);
        } else if (ACTION_LOG_NO.equals(action)) {
            decidePendingLog(false);
        } else {
            restoreState();
            if (state != null && state.active) {
                ensureForeground();
                resumeTicker();
            } else {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void startNew(Intent intent) {
        TimerSchedule schedule = new TimerSchedule(
                intent.getIntExtra(EXTRA_DURATION_MINUTES, 60),
                intent.getIntExtra(EXTRA_PRIMARY_MINUTES, 5),
                intent.getIntExtra(EXTRA_ADDITIONAL_MINUTES, 10),
                intent.getIntExtra(EXTRA_FINISH_DINGS, 10));
        state = TimerState.start(schedule, System.currentTimeMillis(),
                SystemClock.elapsedRealtime(), intent.getBooleanExtra(EXTRA_DIM_SCREEN, true),
                intent.getBooleanExtra(EXTRA_CHIMES_ENABLED, true),
                intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, false),
                intent.getStringExtra(EXTRA_CHIME_SOUND_ID),
                intent.getStringExtra(EXTRA_DISPLAY_MODE_ID));
        stateStore.save(state);
        diagnostics.record("timer.start duration_ms=" + state.durationMs);
        acquireWakeLock();
        ensureForeground();
        scheduleRecovery();
        resumeTicker();
        broadcast(EVENT_STATE_CHANGED);
    }

    private void pause() {
        if (state == null || !state.active || state.paused) {
            return;
        }
        state.pause(SystemClock.elapsedRealtime());
        state.processedThroughActiveMs = state.elapsedActiveMs(SystemClock.elapsedRealtime());
        stateStore.save(state);
        releaseWakeLock();
        cancelRecovery();
        diagnostics.record("timer.pause elapsed_ms=" + state.activeBeforeSegmentMs);
        ensureForeground();
        updateNotification(true);
        resumeTicker();
        broadcast(EVENT_STATE_CHANGED);
    }

    private void resume() {
        if (state == null || !state.active || !state.paused) {
            return;
        }
        state.resume(SystemClock.elapsedRealtime());
        stateStore.save(state);
        acquireWakeLock();
        diagnostics.record("timer.resume elapsed_ms=" + state.activeBeforeSegmentMs);
        ensureForeground();
        scheduleRecovery();
        resumeTicker();
        broadcast(EVENT_STATE_CHANGED);
    }

    private void restart() {
        if (state == null || !state.active) {
            return;
        }
        state.restart(System.currentTimeMillis(), SystemClock.elapsedRealtime());
        stateStore.save(state);
        acquireWakeLock();
        diagnostics.record("timer.restart");
        ensureForeground();
        scheduleRecovery();
        resumeTicker();
        broadcast(EVENT_STATE_CHANGED);
    }

    private void restoreState() {
        if (state == null) {
            state = stateStore.load();
        }
    }

    private void setCueMode(Intent intent) {
        if (state == null || !state.active || intent == null) {
            return;
        }
        state.setCueMode(intent.getBooleanExtra(EXTRA_CHIMES_ENABLED,
                        state.chimesEnabled),
                intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED,
                        state.vibrationEnabled));
        stateStore.save(state);
        diagnostics.record("timer.cues chimes=" + state.chimesEnabled
                + " vibration=" + state.vibrationEnabled);
        broadcast(EVENT_STATE_CHANGED);
    }

    private void setDimScreen(Intent intent) {
        if (state == null || !state.active || intent == null) {
            return;
        }
        state.setDimScreen(intent.getBooleanExtra(EXTRA_DIM_SCREEN, state.dimScreen));
        stateStore.save(state);
        diagnostics.record("timer.dim value=" + state.dimScreen);
        broadcast(EVENT_STATE_CHANGED);
    }

    private void resumeTicker() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    private void processTick() {
        if (state == null || !state.active) {
            return;
        }
        long realtime = SystemClock.elapsedRealtime();
        long elapsed = state.elapsedActiveMs(realtime);
        if (!state.paused && elapsed >= state.durationMs) {
            finish(true);
            return;
        }
        if (!state.paused) {
            TimerSchedule schedule = scheduleForState();
            List<TimerSchedule.Cue> cues = schedule.cuesBetween(
                    state.processedThroughActiveMs, elapsed);
            for (TimerSchedule.Cue cue : cues) {
                dingPlayer.play(cue.dingCount(), state.chimesEnabled, state.vibrationEnabled,
                        state.chimeSoundId);
                long jitter = Math.max(0L, elapsed - cue.elapsedMs());
                diagnostics.recordCueJitter(jitter);
                diagnostics.record("timer.ding count=" + cue.dingCount() + " jitter_ms=" + jitter);
            }
            state.processedThroughActiveMs = elapsed;
            if (!cues.isEmpty() || realtime - lastPersistRealtimeMs >= 5000L) {
                stateStore.save(state);
                lastPersistRealtimeMs = realtime;
            }
        }
        long remainingSecond = (state.remainingMs(realtime) + 999L) / 1000L;
        if (remainingSecond != lastNotificationSecond) {
            updateNotification(false);
            lastNotificationSecond = remainingSecond;
        }
    }

    private TimerSchedule scheduleForState() {
        return new TimerSchedule((int) (state.durationMs / TimerSchedule.MINUTE_MS),
                (int) (state.primaryMs / TimerSchedule.MINUTE_MS),
                (int) (state.additionalMs / TimerSchedule.MINUTE_MS), state.finishDings);
    }

    private void finish(boolean naturalCompletion) {
        if (state == null || !state.active) {
            return;
        }
        long realtime = SystemClock.elapsedRealtime();
        long activeDuration = state.finish(realtime);
        long endWall = System.currentTimeMillis();
        if (naturalCompletion) {
            activeDuration = state.durationMs;
            state.activeBeforeSegmentMs = state.durationMs;
            dingPlayer.play(state.finishDings, state.chimesEnabled, state.vibrationEnabled,
                    state.chimeSoundId);
            completionSoundUntilRealtimeMs = SystemClock.elapsedRealtime()
                    + ToneDingPlayer.playbackSpanMs(state.finishDings, state.chimeSoundId);
        } else {
            completionSoundUntilRealtimeMs = 0L;
        }
        stateStore.save(state);
        stateStore.clearActive();
        releaseWakeLock();
        cancelRecovery();
        PendingMeditationStore.Pending pending = new PendingMeditationStore(this)
                .create(state.startWallMs, endWall, activeDuration);
        diagnostics.record((naturalCompletion ? "timer.complete" : "timer.end")
                + " duration_ms=" + activeDuration);
        updateCompletionNotification(pending);
        scheduleDefaultLog(pending);
        handler.removeCallbacks(ticker);
        broadcast(EVENT_COMPLETED);
    }

    private void decidePendingLog(boolean shouldLog) {
        boolean decided = new PendingMeditationStore(this).decide(shouldLog);
        cancelDefaultLog();
        if (decided) {
            diagnostics.record("timer.log_decision value=" + shouldLog);
            broadcast(EVENT_STATE_CHANGED);
        }
        long soundRemaining = completionSoundUntilRealtimeMs - SystemClock.elapsedRealtime();
        if (soundRemaining > 0L) {
            handler.postDelayed(this::stopAfterDecision, soundRemaining);
            return;
        }
        stopAfterDecision();
    }

    private void stopAfterDecision() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        foreground = false;
        stopSelf();
    }

    private void ensureForeground() {
        if (state == null || !state.active) {
            return;
        }
        Notification notification = buildTimerNotification();
        if (!foreground) {
            startForeground(NOTIFICATION_TIMER, notification);
            foreground = true;
        } else {
            getSystemService(NotificationManager.class).notify(NOTIFICATION_TIMER, notification);
        }
    }

    private void updateNotification(boolean force) {
        if (!foreground && !force) {
            return;
        }
        ensureForeground();
    }

    private Notification buildTimerNotification() {
        long remaining = state.remainingMs(SystemClock.elapsedRealtime());
        String title = state.paused ? "Meditation paused" : "Meditation in progress";
        String text = formatCountdown(remaining) + (state.paused ? " remaining" : " remaining · screen-lock safe");
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), immutableFlags(PendingIntent.FLAG_UPDATE_CURRENT));
        String action = state.paused ? ACTION_RESUME : ACTION_PAUSE;
        String label = state.paused ? "Resume" : "Pause";
        return new Notification.Builder(this, CHANNEL_TIMER)
                .setSmallIcon(com.vishalgoel.meditationtimer.R.drawable.ic_meditation)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .addAction(notificationAction(label, action, 31))
                .addAction(notificationAction("End", ACTION_END, 32))
                .build();
    }

    private void updateCompletionNotification(PendingMeditationStore.Pending pending) {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), immutableFlags(PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = new Notification.Builder(this, CHANNEL_TIMER)
                .setSmallIcon(com.vishalgoel.meditationtimer.R.drawable.ic_meditation)
                .setContentTitle("Meditation ended")
                .setContentText("Log this session? Yes will be selected in 10 seconds.")
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(notificationAction("Log", ACTION_LOG_YES, 33))
                .addAction(notificationAction("Don't log", ACTION_LOG_NO, 34))
                .build();
        if (!foreground) {
            startForeground(NOTIFICATION_TIMER, notification);
            foreground = true;
        } else {
            getSystemService(NotificationManager.class).notify(NOTIFICATION_TIMER, notification);
        }
    }

    private Notification.Action notificationAction(String label, String action, int requestCode) {
        Intent intent = new Intent(this, TimerActionReceiver.class).setAction(action);
        PendingIntent pending = PendingIntent.getBroadcast(this, requestCode, intent,
                immutableFlags(PendingIntent.FLAG_UPDATE_CURRENT));
        return new Notification.Action.Builder(
                com.vishalgoel.meditationtimer.R.drawable.ic_meditation, label, pending).build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_TIMER,
                "Active meditation timer", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps an active meditation timer running with the screen locked.");
        channel.setSound(null, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void scheduleRecovery() {
        if (state == null || !state.active || state.paused) {
            return;
        }
        AlarmManager alarms = getSystemService(AlarmManager.class);
        long elapsed = state.elapsedActiveMs(SystemClock.elapsedRealtime());
        TimerSchedule schedule = scheduleForState();
        long nextActive = state.durationMs;
        List<TimerSchedule.Cue> future = schedule.cuesBetween(elapsed, state.durationMs);
        if (!future.isEmpty()) {
            nextActive = future.get(0).elapsedMs();
        }
        long trigger = SystemClock.elapsedRealtime() + Math.max(1000L, nextActive - elapsed);
        PendingIntent recovery = recoveryIntent();
        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, recovery);
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, recovery);
            }
        } catch (SecurityException denied) {
            alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, recovery);
        }
    }

    private void cancelRecovery() {
        getSystemService(AlarmManager.class).cancel(recoveryIntent());
    }

    private PendingIntent recoveryIntent() {
        Intent intent = new Intent(this, TimerActionReceiver.class).setAction(ACTION_RECOVER);
        return PendingIntent.getBroadcast(this, RECOVERY_REQUEST, intent,
                immutableFlags(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void scheduleDefaultLog(PendingMeditationStore.Pending pending) {
        AlarmManager alarms = getSystemService(AlarmManager.class);
        PendingIntent decision = defaultLogIntent();
        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        pending.decisionDeadlineMs(), decision);
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        pending.decisionDeadlineMs(), decision);
            }
        } catch (SecurityException denied) {
            alarms.set(AlarmManager.RTC_WAKEUP, pending.decisionDeadlineMs(), decision);
        }
        handler.removeCallbacks(defaultLogRunnable);
        handler.postDelayed(defaultLogRunnable, PendingMeditationStore.DEFAULT_YES_DELAY_MS);
    }

    private void cancelDefaultLog() {
        handler.removeCallbacks(defaultLogRunnable);
        getSystemService(AlarmManager.class).cancel(defaultLogIntent());
    }

    private PendingIntent defaultLogIntent() {
        Intent intent = new Intent(this, TimerActionReceiver.class).setAction(ACTION_LOG_DEFAULT);
        return PendingIntent.getBroadcast(this, DEFAULT_LOG_REQUEST, intent,
                immutableFlags(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void acquireWakeLock() {
        if (!wakeLock.isHeld()) {
            long remaining = state == null ? 60_000L
                    : state.remainingMs(SystemClock.elapsedRealtime());
            wakeLock.acquire(Math.max(60_000L, remaining + 60_000L));
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void broadcast(String action) {
        sendBroadcast(new Intent(action).setPackage(getPackageName()),
                getPackageName() + ".permission.INTERNAL_EVENTS");
    }

    private static int immutableFlags(int flags) {
        return flags | PendingIntent.FLAG_IMMUTABLE;
    }

    public static String formatCountdown(long remainingMs) {
        long totalSeconds = Math.max(0L, remainingMs + 999L) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseWakeLock();
        if (dingPlayer != null) {
            dingPlayer.release();
        }
        super.onDestroy();
    }
}
