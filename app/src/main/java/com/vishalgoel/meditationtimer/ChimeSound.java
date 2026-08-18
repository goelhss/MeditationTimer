package com.vishalgoel.meditationtimer;

public enum ChimeSound {
    TEMPLE_BELL("temple_bell", "Temple Bell · warm & resonant", 2900,
            new double[] {523.25, 786.2, 1048.8, 1569.4},
            new double[] {1.0, 0.52, 0.34, 0.17},
            new double[] {1.65, 1.2, 0.92, 0.7}, 170, 0.42),
    SINGING_BOWL("singing_bowl", "Singing Bowl · deep & long", 3600,
            new double[] {196.0, 293.7, 392.8, 589.1},
            new double[] {1.0, 0.64, 0.38, 0.18},
            new double[] {2.35, 1.9, 1.45, 1.0}, 215, 0.48),
    CRYSTAL_CHIME("crystal_chime", "Crystal Chime · bright & clear", 2500,
            new double[] {659.25, 988.7, 1319.8, 1977.5},
            new double[] {1.0, 0.58, 0.31, 0.14},
            new double[] {1.35, 1.05, 0.78, 0.58}, 145, 0.36),
    CLASSIC_DING("classic_ding", "Classic Ding · short", 180,
            new double[0], new double[0], new double[0], 0, 0.0);

    public static final ChimeSound DEFAULT = TEMPLE_BELL;

    private final String id;
    private final String label;
    private final int durationMs;
    private final double[] frequencies;
    private final double[] amplitudes;
    private final double[] decaySeconds;
    private final int echoDelayMs;
    private final double echoLevel;

    ChimeSound(String id, String label, int durationMs, double[] frequencies,
               double[] amplitudes, double[] decaySeconds, int echoDelayMs,
               double echoLevel) {
        this.id = id;
        this.label = label;
        this.durationMs = durationMs;
        this.frequencies = frequencies;
        this.amplitudes = amplitudes;
        this.decaySeconds = decaySeconds;
        this.echoDelayMs = echoDelayMs;
        this.echoLevel = echoLevel;
    }

    public String id() { return id; }
    public String label() { return label; }
    public int durationMs() { return durationMs; }
    double[] frequencies() { return frequencies; }
    double[] amplitudes() { return amplitudes; }
    double[] decaySeconds() { return decaySeconds; }
    int echoDelayMs() { return echoDelayMs; }
    double echoLevel() { return echoLevel; }

    public static ChimeSound fromId(String id) {
        for (ChimeSound sound : values()) {
            if (sound.id.equals(id)) {
                return sound;
            }
        }
        return DEFAULT;
    }
}
