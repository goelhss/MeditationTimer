package com.vishalgoel.meditationtimer;

import android.content.Context;
import android.content.SharedPreferences;

public final class BackupStatusStore {
    private final SharedPreferences preferences;

    public BackupStatusStore(Context context) {
        preferences = context.getSharedPreferences("backup_status", Context.MODE_PRIVATE);
    }

    public boolean isGoogleConnected() {
        return preferences.getBoolean("google_connected", false);
    }

    public void setGoogleConnected(boolean connected) {
        preferences.edit().putBoolean("google_connected", connected).apply();
    }

    public boolean isAutoBackupEnabled() {
        return preferences.getBoolean("auto_backup", true);
    }

    public void setAutoBackupEnabled(boolean enabled) {
        preferences.edit().putBoolean("auto_backup", enabled).apply();
    }

    public boolean isDirty() {
        return preferences.getBoolean("dirty", true);
    }

    public void markDirty() {
        preferences.edit().putBoolean("dirty", true).apply();
    }

    public long lastSuccessMs() {
        return preferences.getLong("last_success", 0L);
    }

    public void recordSuccess(long wallTimeMs) {
        preferences.edit()
                .putBoolean("google_connected", true)
                .putBoolean("dirty", false)
                .putLong("last_success", wallTimeMs)
                .remove("last_error")
                .remove("restore_decision_required")
                .apply();
    }

    public boolean isRestoreDecisionRequired() {
        return preferences.getBoolean("restore_decision_required", false);
    }

    public void requireRestoreDecision() {
        preferences.edit().putBoolean("restore_decision_required", true).apply();
    }

    public void clearRestoreDecision() {
        preferences.edit().remove("restore_decision_required").apply();
    }

    public String lastError() {
        return preferences.getString("last_error", "");
    }

    public void recordError(String message) {
        preferences.edit().putString("last_error", message == null ? "" : message).apply();
    }

    public void clearConnection() {
        preferences.edit()
                .putBoolean("google_connected", false)
                .remove("last_error")
                .remove("restore_decision_required")
                .apply();
    }

    public void recordBackupDeleted() {
        preferences.edit()
                .putBoolean("google_connected", true)
                .putBoolean("auto_backup", false)
                .putBoolean("dirty", true)
                .remove("last_success")
                .remove("last_error")
                .remove("restore_decision_required")
                .apply();
    }
}
