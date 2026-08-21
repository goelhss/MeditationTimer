package com.vishalgoel.meditationtimer;

public enum MeditationPreset {
    QUICK_5("quick_5", "Quick 5 minutes",
            new MeditationConfiguration(5, 15, 5, 10, 10,
                    true, false, true, ChimeSound.CRYSTAL_CHIME.id(),
                    TimerDisplayMode.DEFAULT.id())),
    REGULAR_30("regular_30", "Regular 30 minutes",
            new MeditationConfiguration(30, 15, 5, 10, 10,
                    true, false, true, ChimeSound.TEMPLE_BELL.id(),
                    TimerDisplayMode.DEFAULT.id())),
    WEEKLY_60("weekly_60", "A good weekly 60 minutes",
            new MeditationConfiguration(60, 15, 5, 10, 10,
                    true, false, true, ChimeSound.MEDITATION_BOWL.id(),
                    TimerDisplayMode.DEFAULT.id())),
    CUSTOM("custom", "Custom", null);

    private final String id;
    private final String label;
    private final MeditationConfiguration configuration;

    MeditationPreset(String id, String label, MeditationConfiguration configuration) {
        this.id = id;
        this.label = label;
        this.configuration = configuration;
    }

    public String id() { return id; }
    public String label() { return label; }
    public boolean isCustom() { return this == CUSTOM; }

    public MeditationConfiguration resolve(MeditationConfiguration custom) {
        return isCustom() ? custom : configuration;
    }

    public static MeditationPreset fromId(String id) {
        for (MeditationPreset preset : values()) {
            if (preset.id.equals(id)) {
                return preset;
            }
        }
        return CUSTOM;
    }

    @Override
    public String toString() {
        return label;
    }
}
