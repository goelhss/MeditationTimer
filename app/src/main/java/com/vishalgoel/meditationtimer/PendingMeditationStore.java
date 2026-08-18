package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public final class PendingMeditationStore {
    public static final long DEFAULT_YES_DELAY_MS = 10_000L;

    public record Pending(String id, long startTimeMs, long endTimeMs, long durationMs,
                          long decisionDeadlineMs) {}

    private final SharedPreferences prefs;
    private final MeditationLogStore logStore;

    public PendingMeditationStore(Context context) {
        prefs = context.getSharedPreferences("pending_meditation", Context.MODE_PRIVATE);
        logStore = new MeditationLogStore(context);
    }

    public synchronized Pending create(long startTimeMs, long endTimeMs, long durationMs) {
        Pending pending = new Pending(UUID.randomUUID().toString(), startTimeMs, endTimeMs,
                Math.max(0L, durationMs), endTimeMs + DEFAULT_YES_DELAY_MS);
        prefs.edit()
                .putString("id", pending.id())
                .putLong("start", pending.startTimeMs())
                .putLong("end", pending.endTimeMs())
                .putLong("duration", pending.durationMs())
                .putLong("deadline", pending.decisionDeadlineMs())
                .apply();
        return pending;
    }

    public synchronized Pending get() {
        String id = prefs.getString("id", "");
        if (id == null || id.isBlank()) {
            return null;
        }
        return new Pending(id, prefs.getLong("start", 0L), prefs.getLong("end", 0L),
                prefs.getLong("duration", 0L), prefs.getLong("deadline", 0L));
    }

    public synchronized boolean decide(boolean shouldLog) {
        Pending pending = get();
        if (pending == null) {
            return false;
        }
        if (shouldLog) {
            logStore.add(new MeditationLog(pending.id(), pending.startTimeMs(),
                    pending.endTimeMs(), pending.durationMs()));
        }
        prefs.edit().clear().apply();
        return true;
    }

    public synchronized boolean applyDefaultIfDue(long wallMs) {
        Pending pending = get();
        return pending != null
                && CompletionDecisionPolicy.shouldDefaultToYes(pending.decisionDeadlineMs(), wallMs)
                && decide(true);
    }
}
