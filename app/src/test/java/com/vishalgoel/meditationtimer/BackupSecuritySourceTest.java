package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class BackupSecuritySourceTest {
    @Test
    public void requestsOnlyPrivateDriveAppDataScopeAndDoesNotPersistTokens() throws IOException {
        String activity = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");
        String drive = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/DriveAppDataClient.java");

        assertTrue(activity.contains("https://www.googleapis.com/auth/drive.appdata"));
        assertFalse(activity.contains("https://www.googleapis.com/auth/drive.file"));
        assertFalse(activity.contains("https://www.googleapis.com/auth/drive\""));
        assertFalse(activity.contains("putString(\"access_token\""));
        assertTrue(drive.contains("appDataFolder"));
        assertTrue(drive.contains("deleteBackup"));
    }

    @Test
    public void backupRulesWhitelistOnlyUserDataAndRequireEncryption() throws IOException {
        String modern = TestSources.read("app/src/main/res/xml/data_extraction_rules.xml");
        String legacy = TestSources.read("app/src/main/res/xml/backup_rules.xml");

        assertTrue(modern.contains("disableIfNoEncryptionCapabilities=\"true\""));
        assertTrue(legacy.contains("requireFlags=\"clientSideEncryption\""));
        assertTrue(modern.contains("streak_settings.xml"));
        assertTrue(legacy.contains("streak_settings.xml"));
        assertFalse(modern.contains("diagnostics.xml"));
        assertFalse(modern.contains("timer_state.xml"));
        assertFalse(modern.contains("backup_status.xml"));
    }
}
