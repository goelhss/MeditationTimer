package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.List;
import org.junit.Test;

public final class BackupCodecTest {
    @Test
    public void roundTripPreservesSupportedData() {
        BackupSnapshot original = sample();

        String json = BackupCodec.encode(original);
        BackupSnapshot decoded = BackupCodec.decode(json);

        assertEquals(original.generatedAtMs(), decoded.generatedAtMs());
        assertEquals(original.logs(), decoded.logs());
        assertEquals(original.resolutions(), decoded.resolutions());
        assertEquals(original.settings(), decoded.settings());
        assertEquals(original.reminder().enabled(), decoded.reminder().enabled());
        assertEquals(original.reminder().frequency(), decoded.reminder().frequency());
        assertEquals(original.reminder().hour(), decoded.reminder().hour());
        assertEquals(original.reminder().minute(), decoded.reminder().minute());
        assertEquals(original.reminder().customDaysMask(), decoded.reminder().customDaysMask());
        assertTrue(json.contains("com.vishalgoel.meditationtimer"));
        assertTrue(json.contains("customConfiguration"));
    }

    @Test
    public void rejectsWrongApplicationAndFormat() {
        String json = BackupCodec.encode(sample());

        assertThrows(IllegalArgumentException.class,
                () -> BackupCodec.decode(json.replace("com.vishalgoel.meditationtimer",
                        "example.attacker")));
        assertThrows(IllegalArgumentException.class,
                () -> BackupCodec.decode(json.replace("\"formatVersion\": 1",
                        "\"formatVersion\": 999")));
    }

    @Test
    public void rejectsOversizedInput() {
        String oversized = "x".repeat(BackupCodec.MAX_BACKUP_BYTES + 1);
        assertThrows(IllegalArgumentException.class, () -> BackupCodec.decode(oversized));
    }

    @Test
    public void olderBackupWithoutCustomPresetLoadsCurrentSettingsAsCustom() {
        String json = BackupCodec.encode(sample())
                .replaceAll(",?\\s*\"customSaved\"\\s*:\\s*true", "")
                .replaceAll(",?\\s*\"selectedPreset\"\\s*:\\s*\"[^\"]+\"", "")
                .replaceAll(",?\\s*\"customConfiguration\"\\s*:\\s*\\{[^}]+}", "");

        BackupSnapshot decoded = BackupCodec.decode(json);

        assertEquals(MeditationPreset.CUSTOM.id(), decoded.settings().selectedPresetId());
        assertEquals(60, decoded.settings().customConfiguration().durationMinutes());
    }

    private static BackupSnapshot sample() {
        return new BackupSnapshot(123_456L,
                List.of(new MeditationLog("log-1", 100L, 200L, 100L)),
                List.of(new Resolution("resolution-1", 300L, "Meditate daily")),
                new BackupSnapshot.TimerSettings(60, 15, 5, 10, 10,
                        true, false, true, ChimeSound.DEFAULT.id(),
                        TimerDisplayMode.DEFAULT.id(), AppColorTheme.DARK_PURPLE.id(),
                        true, MeditationPreset.REGULAR_30.resolve(null),
                        MeditationPreset.CUSTOM.id()),
                new ReminderSchedule(true, ReminderSchedule.Frequency.WEEKDAYS,
                        8, 30, ReminderSchedule.WEEKDAYS_MASK));
    }
}
