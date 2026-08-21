package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

public final class MeditationConfigurationStore {
    private static final String PREFS = "timer_settings";
    private static final String CUSTOM_PREFIX = "custom_";
    private final SharedPreferences preferences;

    public MeditationConfigurationStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public MeditationConfiguration current() {
        return read("");
    }

    public MeditationConfiguration custom() {
        return preferences.getBoolean("custom_saved", false) ? read(CUSTOM_PREFIX) : current();
    }

    public boolean customSaved() {
        return preferences.getBoolean("custom_saved", false);
    }

    public MeditationPreset selectedPreset() {
        return MeditationPreset.fromId(preferences.getString(
                "selected_preset", MeditationPreset.CUSTOM.id()));
    }

    public void saveCurrent(MeditationConfiguration configuration,
                            MeditationPreset selectedPreset) {
        SharedPreferences.Editor editor = preferences.edit();
        write(editor, "", configuration);
        editor.putString("selected_preset", selectedPreset.id()).apply();
    }

    public void saveCustom(MeditationConfiguration configuration) {
        SharedPreferences.Editor editor = preferences.edit();
        write(editor, CUSTOM_PREFIX, configuration);
        editor.putBoolean("custom_saved", true)
                .putString("selected_preset", MeditationPreset.CUSTOM.id())
                .apply();
    }

    public void restoreCustom(MeditationConfiguration configuration,
                              boolean customSaved, String selectedPresetId) {
        SharedPreferences.Editor editor = preferences.edit();
        write(editor, CUSTOM_PREFIX, configuration);
        editor.putBoolean("custom_saved", customSaved)
                .putString("selected_preset", MeditationPreset.fromId(selectedPresetId).id())
                .apply();
    }

    private MeditationConfiguration read(String prefix) {
        return new MeditationConfiguration(
                preferences.getInt(prefix + "duration", 60),
                preferences.getInt(prefix + "prep_seconds", 15),
                preferences.getInt(prefix + "primary", 5),
                preferences.getInt(prefix + "additional", 10),
                preferences.getInt(prefix + "finish", 10),
                preferences.getBoolean(prefix + "chimes", true),
                preferences.getBoolean(prefix + "vibrate", false),
                preferences.getBoolean(prefix + "dim", true),
                preferences.getString(prefix + "chime_sound", ChimeSound.DEFAULT.id()),
                preferences.getString(prefix + "timer_display", TimerDisplayMode.DEFAULT.id()));
    }

    private static void write(SharedPreferences.Editor editor, String prefix,
                              MeditationConfiguration value) {
        editor.putInt(prefix + "duration", value.durationMinutes())
                .putInt(prefix + "prep_seconds", value.preparationSeconds())
                .putInt(prefix + "primary", value.primaryMinutes())
                .putInt(prefix + "additional", value.additionalMinutes())
                .putInt(prefix + "finish", value.finishDings())
                .putBoolean(prefix + "chimes", value.chimes())
                .putBoolean(prefix + "vibrate", value.vibrate())
                .putBoolean(prefix + "dim", value.dim())
                .putString(prefix + "chime_sound", value.chimeSoundId())
                .putString(prefix + "timer_display", value.timerDisplayId());
    }
}
