package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public final class MeditationPresetTest {
    @Test
    public void builtInDurationsAndSoundsMatchTheirPurpose() {
        assertEquals(5, MeditationPreset.QUICK_5.resolve(null).durationMinutes());
        assertEquals(ChimeSound.CRYSTAL_CHIME.id(),
                MeditationPreset.QUICK_5.resolve(null).chimeSoundId());
        assertEquals(30, MeditationPreset.REGULAR_30.resolve(null).durationMinutes());
        assertEquals(ChimeSound.TEMPLE_BELL.id(),
                MeditationPreset.REGULAR_30.resolve(null).chimeSoundId());
        assertEquals(60, MeditationPreset.WEEKLY_60.resolve(null).durationMinutes());
        assertEquals(ChimeSound.MEDITATION_BOWL.id(),
                MeditationPreset.WEEKLY_60.resolve(null).chimeSoundId());
    }

    @Test
    public void customReturnsSavedConfigurationAndUnknownIdsAreSafe() {
        MeditationConfiguration custom = MeditationPreset.REGULAR_30.resolve(null);

        assertSame(custom, MeditationPreset.CUSTOM.resolve(custom));
        assertEquals(MeditationPreset.CUSTOM, MeditationPreset.fromId("future-value"));
    }
}
