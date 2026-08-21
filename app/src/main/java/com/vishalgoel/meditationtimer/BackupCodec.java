package com.vishalgoel.meditationtimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class BackupCodec {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_BACKUP_BYTES = 1024 * 1024;
    public static final int MAX_HISTORY_ENTRIES = 10_000;
    private static final String APP_ID = "com.vishalgoel.meditationtimer";

    private BackupCodec() {}

    public static String encode(BackupSnapshot snapshot) {
        try {
            JSONObject root = new JSONObject();
            root.put("formatVersion", FORMAT_VERSION);
            root.put("applicationId", APP_ID);
            root.put("generatedAt", snapshot.generatedAtMs());
            root.put("logs", new JSONArray(MeditationLogCodec.encode(snapshot.logs())));
            root.put("resolutions", new JSONArray(ResolutionCodec.encode(snapshot.resolutions())));

            BackupSnapshot.TimerSettings settings = snapshot.settings();
            JSONObject settingsJson = new JSONObject();
            settingsJson.put("durationMinutes", settings.durationMinutes());
            settingsJson.put("preparationSeconds", settings.preparationSeconds());
            settingsJson.put("primaryMinutes", settings.primaryMinutes());
            settingsJson.put("additionalMinutes", settings.additionalMinutes());
            settingsJson.put("finishDings", settings.finishDings());
            settingsJson.put("chimes", settings.chimes());
            settingsJson.put("vibrate", settings.vibrate());
            settingsJson.put("dim", settings.dim());
            settingsJson.put("chimeSound", settings.chimeSoundId());
            settingsJson.put("timerDisplay", settings.timerDisplayId());
            settingsJson.put("backgroundTheme", settings.backgroundThemeId());
            settingsJson.put("customSaved", settings.customSaved());
            settingsJson.put("selectedPreset", settings.selectedPresetId());
            settingsJson.put("customConfiguration",
                    encodeConfiguration(settings.customConfiguration()));
            root.put("settings", settingsJson);

            ReminderSchedule reminder = snapshot.reminder();
            JSONObject reminderJson = new JSONObject();
            reminderJson.put("enabled", reminder.enabled());
            reminderJson.put("frequency", reminder.frequency().id());
            reminderJson.put("hour", reminder.hour());
            reminderJson.put("minute", reminder.minute());
            reminderJson.put("customDays", reminder.customDaysMask());
            root.put("reminder", reminderJson);
            String json = root.toString(2);
            if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                    > MAX_BACKUP_BYTES) {
                throw new IllegalArgumentException("Your backup is larger than the 1 MB limit.");
            }
            return json;
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static BackupSnapshot decode(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("The selected backup is empty.");
        }
        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BACKUP_BYTES) {
            throw new IllegalArgumentException("The backup is larger than 1 MB.");
        }
        try {
            JSONObject root = new JSONObject(json);
            int format = root.optInt("formatVersion", -1);
            if (format != FORMAT_VERSION) {
                throw new IllegalArgumentException("This backup format is not supported.");
            }
            if (!APP_ID.equals(root.optString("applicationId", ""))) {
                throw new IllegalArgumentException("This is not a Meditation Timer backup.");
            }
            JSONArray logsJson = requiredArray(root, "logs");
            JSONArray resolutionsJson = requiredArray(root, "resolutions");
            if (logsJson.length() > MAX_HISTORY_ENTRIES
                    || resolutionsJson.length() > MAX_HISTORY_ENTRIES) {
                throw new IllegalArgumentException("The backup contains too many history entries.");
            }
            List<MeditationLog> logs = MeditationLogCodec.decode(logsJson.toString());
            List<Resolution> resolutions = ResolutionCodec.decode(resolutionsJson.toString());

            JSONObject settings = requiredObject(root, "settings");
            MeditationConfiguration currentConfiguration = new MeditationConfiguration(
                    requiredInt(settings, "durationMinutes"),
                    requiredInt(settings, "preparationSeconds"),
                    requiredInt(settings, "primaryMinutes"),
                    requiredInt(settings, "additionalMinutes"),
                    requiredInt(settings, "finishDings"),
                    requiredBoolean(settings, "chimes"),
                    requiredBoolean(settings, "vibrate"),
                    requiredBoolean(settings, "dim"),
                    requiredString(settings, "chimeSound"),
                    requiredString(settings, "timerDisplay"));
            MeditationConfiguration customConfiguration = settings.has("customConfiguration")
                    ? decodeConfiguration(requiredObject(settings, "customConfiguration"))
                    : currentConfiguration;
            BackupSnapshot.TimerSettings timerSettings = new BackupSnapshot.TimerSettings(
                    currentConfiguration.durationMinutes(),
                    currentConfiguration.preparationSeconds(),
                    currentConfiguration.primaryMinutes(),
                    currentConfiguration.additionalMinutes(),
                    currentConfiguration.finishDings(),
                    currentConfiguration.chimes(),
                    currentConfiguration.vibrate(),
                    currentConfiguration.dim(),
                    currentConfiguration.chimeSoundId(),
                    currentConfiguration.timerDisplayId(),
                    requiredString(settings, "backgroundTheme"),
                    settings.optBoolean("customSaved", false),
                    customConfiguration,
                    settings.optString("selectedPreset", MeditationPreset.CUSTOM.id()));

            JSONObject reminderJson = requiredObject(root, "reminder");
            ReminderSchedule reminder = new ReminderSchedule(
                    requiredBoolean(reminderJson, "enabled"),
                    ReminderSchedule.Frequency.fromId(requiredString(reminderJson, "frequency")),
                    requiredInt(reminderJson, "hour"),
                    requiredInt(reminderJson, "minute"),
                    requiredInt(reminderJson, "customDays"));
            return new BackupSnapshot(root.optLong("generatedAt", 0L), logs, resolutions,
                    timerSettings, reminder);
        } catch (JSONException error) {
            throw new IllegalArgumentException("The backup is not valid JSON.", error);
        }
    }

    private static JSONArray requiredArray(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof JSONArray array)) {
            throw new IllegalArgumentException("The backup is missing " + key + ".");
        }
        return array;
    }

    private static JSONObject requiredObject(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof JSONObject child)) {
            throw new IllegalArgumentException("The backup is missing " + key + ".");
        }
        return child;
    }

    private static int requiredInt(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("The backup has an invalid " + key + ".");
        }
        return number.intValue();
    }

    private static boolean requiredBoolean(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof Boolean bool)) {
            throw new IllegalArgumentException("The backup has an invalid " + key + ".");
        }
        return bool;
    }

    private static String requiredString(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("The backup has an invalid " + key + ".");
        }
        return text;
    }

    private static JSONObject encodeConfiguration(MeditationConfiguration value)
            throws JSONException {
        JSONObject json = new JSONObject();
        json.put("durationMinutes", value.durationMinutes());
        json.put("preparationSeconds", value.preparationSeconds());
        json.put("primaryMinutes", value.primaryMinutes());
        json.put("additionalMinutes", value.additionalMinutes());
        json.put("finishDings", value.finishDings());
        json.put("chimes", value.chimes());
        json.put("vibrate", value.vibrate());
        json.put("dim", value.dim());
        json.put("chimeSound", value.chimeSoundId());
        json.put("timerDisplay", value.timerDisplayId());
        return json;
    }

    private static MeditationConfiguration decodeConfiguration(JSONObject json)
            throws JSONException {
        return new MeditationConfiguration(
                requiredInt(json, "durationMinutes"),
                requiredInt(json, "preparationSeconds"),
                requiredInt(json, "primaryMinutes"),
                requiredInt(json, "additionalMinutes"),
                requiredInt(json, "finishDings"),
                requiredBoolean(json, "chimes"),
                requiredBoolean(json, "vibrate"),
                requiredBoolean(json, "dim"),
                requiredString(json, "chimeSound"),
                requiredString(json, "timerDisplay"));
    }
}
