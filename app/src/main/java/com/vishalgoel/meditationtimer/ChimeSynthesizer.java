package com.vishalgoel.meditationtimer;

public final class ChimeSynthesizer {
    public static final int SAMPLE_RATE = 44_100;

    private ChimeSynthesizer() {}

    public static short[] synthesize(ChimeSound sound) {
        if (sound == ChimeSound.CLASSIC_DING) {
            return new short[0];
        }
        int sampleCount = sound.durationMs() * SAMPLE_RATE / 1000;
        double[] dry = new double[sampleCount];
        double[] frequencies = sound.frequencies();
        double[] amplitudes = sound.amplitudes();
        double[] decays = sound.decaySeconds();
        for (int index = 0; index < sampleCount; index++) {
            double seconds = index / (double) SAMPLE_RATE;
            double attack = Math.min(1.0, seconds / 0.006);
            double value = 0.0;
            for (int partial = 0; partial < frequencies.length; partial++) {
                double envelope = attack * Math.exp(-seconds / decays[partial]);
                double strike = Math.sin(2.0 * Math.PI * frequencies[partial] * seconds);
                double shimmer = 0.16 * Math.sin(2.0 * Math.PI
                        * (frequencies[partial] * 1.006) * seconds);
                value += amplitudes[partial] * envelope * (strike + shimmer);
            }
            dry[index] = value;
        }

        int echo = sound.echoDelayMs() * SAMPLE_RATE / 1000;
        double[] wet = new double[sampleCount];
        double max = 0.0;
        for (int index = 0; index < sampleCount; index++) {
            double value = dry[index];
            if (index >= echo) {
                value += dry[index - echo] * sound.echoLevel();
            }
            if (index >= echo * 2) {
                value += dry[index - echo * 2] * sound.echoLevel() * 0.56;
            }
            if (index >= echo * 3) {
                value += dry[index - echo * 3] * sound.echoLevel() * 0.31;
            }
            wet[index] = value;
            max = Math.max(max, Math.abs(value));
        }

        short[] pcm = new short[sampleCount];
        double scale = max == 0.0 ? 0.0 : 0.94 * Short.MAX_VALUE / max;
        for (int index = 0; index < sampleCount; index++) {
            pcm[index] = (short) Math.round(wet[index] * scale);
        }
        return pcm;
    }
}
