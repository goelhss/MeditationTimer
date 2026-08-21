package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ResolutionStore {
    private static final String KEY_RESOLUTIONS = "resolutions_json";
    private final SharedPreferences prefs;

    public ResolutionStore(Context context) {
        prefs = context.getSharedPreferences("meditation_resolutions", Context.MODE_PRIVATE);
    }

    public synchronized List<Resolution> all() {
        return ResolutionCodec.decode(prefs.getString(KEY_RESOLUTIONS, "[]"));
    }

    public synchronized void add(long dateMs, String comment) {
        String cleanComment = comment == null ? "" : comment.trim();
        if (cleanComment.isBlank()) {
            throw new IllegalArgumentException("Enter the commitment you made.");
        }
        List<Resolution> resolutions = new ArrayList<>(all());
        resolutions.add(new Resolution(UUID.randomUUID().toString(), dateMs, cleanComment));
        prefs.edit().putString(KEY_RESOLUTIONS,
                ResolutionCodec.encode(resolutions)).apply();
    }

    public synchronized boolean delete(String id) {
        List<Resolution> resolutions = new ArrayList<>(all());
        boolean removed = resolutions.removeIf(resolution -> resolution.id().equals(id));
        if (removed) {
            prefs.edit().putString(KEY_RESOLUTIONS,
                    ResolutionCodec.encode(resolutions)).apply();
        }
        return removed;
    }
}
