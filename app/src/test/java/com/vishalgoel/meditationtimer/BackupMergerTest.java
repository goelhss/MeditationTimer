package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public final class BackupMergerTest {
    @Test
    public void logsMergeByIdAndKeepLocalVersionOnConflict() {
        MeditationLog local = new MeditationLog("same", 10L, 20L, 10L);
        MeditationLog conflict = new MeditationLog("same", 30L, 40L, 10L);
        MeditationLog added = new MeditationLog("new", 50L, 60L, 10L);

        List<MeditationLog> merged = BackupMerger.mergeLogs(
                List.of(local), List.of(conflict, added));

        assertEquals(2, merged.size());
        assertEquals(local, merged.get(1));
        assertEquals(added, merged.get(0));
    }

    @Test
    public void resolutionsMergeByIdWithoutDuplicates() {
        Resolution local = new Resolution("same", 10L, "Local");
        Resolution added = new Resolution("new", 20L, "Added");

        List<Resolution> merged = BackupMerger.mergeResolutions(
                List.of(local), List.of(local, added));

        assertEquals(List.of(added, local), merged);
    }
}
