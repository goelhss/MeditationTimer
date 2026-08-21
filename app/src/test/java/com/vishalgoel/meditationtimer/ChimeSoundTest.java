package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ChimeSoundTest {
    @Test
    public void unknownOrMissingSelectionUsesTempleBell() {
        assertEquals(ChimeSound.TEMPLE_BELL, ChimeSound.fromId(null));
        assertEquals(ChimeSound.TEMPLE_BELL, ChimeSound.fromId("missing"));
        assertEquals(ChimeSound.SINGING_BOWL, ChimeSound.fromId("singing_bowl"));
        assertEquals(ChimeSound.MEDITATION_BOWL, ChimeSound.fromId("meditation_bowl"));
    }

    @Test
    public void resonantSoundsAreLongLoudAndDistinct() {
        short[] temple = ChimeSynthesizer.synthesize(ChimeSound.TEMPLE_BELL);
        short[] bowl = ChimeSynthesizer.synthesize(ChimeSound.SINGING_BOWL);
        short[] meditationBowl = ChimeSynthesizer.synthesize(ChimeSound.MEDITATION_BOWL);
        short[] crystal = ChimeSynthesizer.synthesize(ChimeSound.CRYSTAL_CHIME);

        assertEquals(ChimeSound.TEMPLE_BELL.durationMs() * ChimeSynthesizer.SAMPLE_RATE / 1000,
                temple.length);
        assertTrue(peak(temple) > 30_000);
        assertTrue(peak(bowl) > 30_000);
        assertTrue(peak(meditationBowl) > 30_000);
        assertTrue(peak(crystal) > 30_000);
        assertNotEquals(temple.length, bowl.length);
        assertNotEquals(bowl.length, crystal.length);
        assertNotEquals(bowl.length, meditationBowl.length);
        assertTrue(tailEnergy(temple) > 1_000_000L);
    }

    private static int peak(short[] samples) {
        int peak = 0;
        for (short sample : samples) {
            peak = Math.max(peak, Math.abs((int) sample));
        }
        return peak;
    }

    private static long tailEnergy(short[] samples) {
        long energy = 0L;
        for (int index = samples.length * 4 / 5; index < samples.length; index += 32) {
            energy += Math.abs((int) samples[index]);
        }
        return energy;
    }
}
