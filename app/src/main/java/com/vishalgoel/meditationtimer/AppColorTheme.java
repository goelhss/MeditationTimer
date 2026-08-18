package com.vishalgoel.meditationtimer;

public enum AppColorTheme {
    DARK_PURPLE("dark_purple", "Dark Purple", 0xFF241133),
    DARK_BLUE("dark_blue", "Dark Blue", 0xFF0D2440),
    DARK_GRAY("dark_gray", "Dark Gray", 0xFF25272A),
    BLACK("black", "Black", 0xFF000000);

    private final String id;
    private final String label;
    private final int backgroundColor;

    AppColorTheme(String id, String label, int backgroundColor) {
        this.id = id;
        this.label = label;
        this.backgroundColor = backgroundColor;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public static AppColorTheme fromId(String id) {
        for (AppColorTheme theme : values()) {
            if (theme.id.equals(id)) {
                return theme;
            }
        }
        return DARK_PURPLE;
    }
}
