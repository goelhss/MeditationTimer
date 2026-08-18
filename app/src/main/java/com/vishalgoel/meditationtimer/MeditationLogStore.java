package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MeditationLogStore {
    private static final String KEY_LOGS = "logs_json";
    private final SharedPreferences prefs;

    public MeditationLogStore(Context context) {
        prefs = context.getSharedPreferences("meditation_logs", Context.MODE_PRIVATE);
    }

    public synchronized List<MeditationLog> all() {
        return MeditationLogCodec.decode(prefs.getString(KEY_LOGS, "[]"));
    }

    public synchronized void add(MeditationLog log) {
        List<MeditationLog> logs = new ArrayList<>(all());
        for (MeditationLog existing : logs) {
            if (existing.id().equals(log.id())) {
                return;
            }
        }
        logs.add(0, log);
        prefs.edit().putString(KEY_LOGS, MeditationLogCodec.encode(logs)).apply();
    }

    public synchronized int delete(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Set<String> wanted = new HashSet<>(ids);
        List<MeditationLog> logs = new ArrayList<>(all());
        int before = logs.size();
        logs.removeIf(log -> wanted.contains(log.id()));
        prefs.edit().putString(KEY_LOGS, MeditationLogCodec.encode(logs)).apply();
        return before - logs.size();
    }

    public synchronized void deleteAll() {
        prefs.edit().putString(KEY_LOGS, "[]").apply();
    }
}
