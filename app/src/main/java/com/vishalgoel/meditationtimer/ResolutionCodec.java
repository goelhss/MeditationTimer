package com.vishalgoel.meditationtimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ResolutionCodec {
    private ResolutionCodec() {}

    public static String encode(List<Resolution> resolutions) {
        JSONArray array = new JSONArray();
        for (Resolution resolution : resolutions) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", resolution.id());
                object.put("date", resolution.dateMs());
                object.put("comment", resolution.comment());
            } catch (JSONException impossible) {
                throw new IllegalStateException(impossible);
            }
            array.put(object);
        }
        return array.toString();
    }

    public static List<Resolution> decode(String json) {
        List<Resolution> result = new ArrayList<>();
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
                long date = object.optLong("date", -1L);
                String comment = object.optString("comment", "").trim();
                if (!id.isBlank() && date >= 0L && !comment.isBlank()) {
                    result.add(new Resolution(id, date, comment));
                }
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        result.sort(Comparator.comparingLong(Resolution::dateMs).reversed());
        return result;
    }
}
