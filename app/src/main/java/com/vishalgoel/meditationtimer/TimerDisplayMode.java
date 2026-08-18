package com.vishalgoel.meditationtimer;

public enum TimerDisplayMode {
    DIGITAL("digital", "Large digital"),
    ANALOG("analog", "Analog dial");

    public static final TimerDisplayMode DEFAULT = DIGITAL;

    private final String id;
    private final String label;

    TimerDisplayMode(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() { return id; }
    public String label() { return label; }

    public static TimerDisplayMode fromId(String id) {
        for (TimerDisplayMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return DEFAULT;
    }
}
