package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class ResolutionCodecTest {
    @Test
    public void roundTripPreservesCommentsAndSortsNewestFirst() {
        List<Resolution> decoded = ResolutionCodec.decode(ResolutionCodec.encode(List.of(
                new Resolution("older", 1_000L, "Meditate daily"),
                new Resolution("newer", 2_000L, "Complete two hours each day"))));

        assertEquals(2, decoded.size());
        assertEquals("newer", decoded.get(0).id());
        assertEquals("Complete two hours each day", decoded.get(0).comment());
    }

    @Test
    public void malformedOrIncompleteDataIsSafe() {
        assertTrue(ResolutionCodec.decode("not-json").isEmpty());
        assertTrue(ResolutionCodec.decode("[{\"id\":\"x\",\"date\":1,\"comment\":\"\"}]")
                .isEmpty());
    }
}
