package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DiagnosticsStore {
    private static final int MAX_LOG_CHARS = 24_000;
    private final SharedPreferences prefs;

    public DiagnosticsStore(Context context) {
        prefs = context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE);
    }

    public synchronized void record(String event) {
        String safe = event == null ? "" : event.replace('\n', ' ').replace('\r', ' ');
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                .format(new Date());
        String combined = timestamp + " " + safe + "\n" + prefs.getString("events", "");
        if (combined.length() > MAX_LOG_CHARS) {
            combined = combined.substring(0, MAX_LOG_CHARS);
        }
        prefs.edit().putString("events", combined).apply();
    }

    public synchronized void recordCueJitter(long jitterMs) {
        long nonNegative = Math.max(0L, jitterMs);
        long count = prefs.getLong("jitter_count", 0L) + 1L;
        long total = prefs.getLong("jitter_total", 0L) + nonNegative;
        long min = prefs.getLong("jitter_min", Long.MAX_VALUE);
        long max = prefs.getLong("jitter_max", 0L);
        prefs.edit()
                .putLong("jitter_count", count)
                .putLong("jitter_total", total)
                .putLong("jitter_min", Math.min(min, nonNegative))
                .putLong("jitter_max", Math.max(max, nonNegative))
                .putLong("jitter_last", nonNegative)
                .apply();
    }

    public synchronized String summary() {
        long count = prefs.getLong("jitter_count", 0L);
        if (count == 0L) {
            return "Ding timing: no samples yet";
        }
        long total = prefs.getLong("jitter_total", 0L);
        return String.format(Locale.US,
                "Ding timing (ms): avg %d · min %d · max %d · last %d · count %d",
                total / count, prefs.getLong("jitter_min", 0L),
                prefs.getLong("jitter_max", 0L), prefs.getLong("jitter_last", 0L), count);
    }

    public synchronized String export(String version) {
        return "Meditation Timer Diagnostics\nVersion: " + version + "\n" + summary()
                + "\n\nEvents (newest first)\n" + prefs.getString("events", "");
    }

    public synchronized void clear() {
        prefs.edit().clear().apply();
    }
}
