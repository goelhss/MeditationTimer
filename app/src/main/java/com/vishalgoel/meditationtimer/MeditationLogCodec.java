package com.vishalgoel.meditationtimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MeditationLogCodec {
    private MeditationLogCodec() {}

    public static String encode(List<MeditationLog> logs) {
        JSONArray array = new JSONArray();
        for (MeditationLog log : logs) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", log.id());
                object.put("start", log.startTimeMs());
                object.put("end", log.endTimeMs());
                object.put("duration", log.durationMs());
            } catch (JSONException impossible) {
                throw new IllegalStateException(impossible);
            }
            array.put(object);
        }
        return array.toString();
    }

    public static List<MeditationLog> decode(String json) {
        List<MeditationLog> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int index = 0; index < array.length(); index++) {
                JSONObject object = array.optJSONObject(index);
                if (object == null) {
                    continue;
                }
                String id = object.optString("id", "");
                long start = object.optLong("start", -1L);
                long end = object.optLong("end", -1L);
                long duration = object.optLong("duration", -1L);
                if (!id.isBlank() && start >= 0L && end >= start && duration >= 0L) {
                    result.add(new MeditationLog(id, start, end, duration));
                }
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        result.sort(Comparator.comparingLong(MeditationLog::endTimeMs).reversed());
        return result;
    }
}
