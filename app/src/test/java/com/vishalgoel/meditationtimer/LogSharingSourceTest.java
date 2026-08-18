package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class LogSharingSourceTest {
    @Test
    public void shareUsesReadOnlyTextFileAndAndroidChooser() throws IOException {
        String activity = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");
        String provider = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/LogShareProvider.java");

        assertTrue(activity.contains("Intent.ACTION_SEND"));
        assertTrue(activity.contains("Intent.EXTRA_STREAM"));
        assertTrue(activity.contains("FLAG_GRANT_READ_URI_PERMISSION"));
        assertTrue(activity.contains("Intent.createChooser"));
        assertTrue(provider.contains("Shared logs are read-only"));
        assertTrue(provider.contains("ParcelFileDescriptor.MODE_READ_ONLY"));
    }

    @Test
    public void logUiSupportsSelectedAndAllDeletion() throws IOException {
        String activity = TestSources.read(
                "app/src/main/java/com/vishalgoel/meditationtimer/MainActivity.java");

        assertTrue(activity.contains("Select all "));
        assertTrue(activity.contains("Delete selected"));
        assertTrue(activity.contains("Delete all entries"));
        assertTrue(activity.contains("selectedLogIds"));
    }
}
