package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class AppPolicySourceTest {
    @Test
    public void packageIdentityIsAligned() throws IOException {
        String gradle = TestSources.read("app/build.gradle");
        String manifest = TestSources.read("app/src/main/AndroidManifest.xml");

        assertTrue(gradle.contains("namespace \"com.vishalgoel.meditationtimer\""));
        assertTrue(gradle.contains("applicationId \"com.vishalgoel.meditationtimer\""));
        assertTrue(manifest.contains("${applicationId}.logs"));
    }

    @Test
    public void manifestKeepsMinimalBackgroundAndShareCapabilities() throws IOException {
        String manifest = TestSources.read("app/src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE"));
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"));
        assertTrue(manifest.contains("android.permission.SCHEDULE_EXACT_ALARM"));
        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"));
        assertTrue(manifest.contains("android.permission.VIBRATE"));
        assertTrue(manifest.contains("android.permission.WAKE_LOCK"));
        assertTrue(manifest.contains("android:foregroundServiceType=\"mediaPlayback\""));
        assertTrue(manifest.contains("android:stopWithTask=\"false\""));
        assertTrue(manifest.contains(".LogShareProvider"));
        assertTrue(manifest.contains(".ReminderReceiver"));
        assertTrue(manifest.contains("android.permission.INTERNET"));
        assertTrue(manifest.contains("android:dataExtractionRules="));
        assertTrue(manifest.contains("android:fullBackupContent="));
    }

    @Test
    public void releaseScriptsEnforceTestsSigningAndAabVerification() throws IOException {
        String build = TestSources.read("scripts/build.sh");
        String bundle = TestSources.read("scripts/build-aab.sh");
        String upload = TestSources.read("scripts/upload_play_internal.sh");
        String publish = TestSources.read("scripts/publish_google_play_internal.sh");

        assertTrue(build.contains("Running always-required tests"));
        assertTrue(build.contains("TEST_SUITE"));
        assertTrue(build.contains(":app:lintDebug"));
        assertTrue(bundle.contains("signing/keystore.properties"));
        assertTrue(bundle.contains("jarsigner -verify"));
        assertTrue(upload.contains("jarsigner -verify"));
        assertTrue(upload.contains("TOOLCHAIN_ROOT"));
        assertTrue(upload.contains("export JAVA_HOME"));
        assertTrue(upload.contains("export PATH"));
        assertTrue(upload.contains("com.vishalgoel.meditationtimer"));
        assertTrue(upload.contains("--app MeditationTimer"));
        assertTrue(publish.contains("accepts no publishing arguments"));
        assertTrue(publish.contains("--check"));
        assertTrue(publish.contains("CHECK_ONLY"));
        assertTrue(publish.contains("git status --porcelain"));
        assertTrue(publish.contains("git rev-parse origin/main"));
        assertTrue(publish.contains("TEST_SUITE=all"));
        assertTrue(publish.contains("UPLOAD_PLAY=1"));
        assertTrue(publish.contains("UPLOAD_NO_COMMIT=0"));
        assertFalse(publish.contains("--track"));
    }
}
