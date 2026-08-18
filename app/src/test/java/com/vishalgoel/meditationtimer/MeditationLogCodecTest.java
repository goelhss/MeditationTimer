package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class MeditationLogCodecTest {
    @Test
    public void roundTripPreservesLogsAndSortsNewestFirst() {
        List<MeditationLog> logs = List.of(
                new MeditationLog("older", 100L, 200L, 90L),
                new MeditationLog("newer", 300L, 500L, 180L));

        List<MeditationLog> decoded = MeditationLogCodec.decode(MeditationLogCodec.encode(logs));

        assertEquals(2, decoded.size());
        assertEquals("newer", decoded.get(0).id());
        assertEquals("older", decoded.get(1).id());
    }

    @Test
    public void malformedOrInvalidDataIsIgnoredSafely() {
        assertTrue(MeditationLogCodec.decode("not-json").isEmpty());
        assertTrue(MeditationLogCodec.decode(
                "[{\"id\":\"x\",\"start\":5,\"end\":4,\"duration\":1}]").isEmpty());
    }
}
