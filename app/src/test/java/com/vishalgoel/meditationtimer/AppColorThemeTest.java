package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AppColorThemeTest {
    @Test
    public void exposesRequestedThemesAndDefaultsToDarkPurple() {
        assertEquals(4, AppColorTheme.values().length);
        assertEquals("Dark Purple", AppColorTheme.DARK_PURPLE.label());
        assertEquals("Dark Blue", AppColorTheme.DARK_BLUE.label());
        assertEquals("Dark Gray", AppColorTheme.DARK_GRAY.label());
        assertEquals("Black", AppColorTheme.BLACK.label());
        assertEquals(AppColorTheme.DARK_PURPLE, AppColorTheme.fromId("unknown"));
    }
}
