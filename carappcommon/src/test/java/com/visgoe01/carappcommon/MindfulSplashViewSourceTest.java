package com.visgoe01.carappcommon;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class MindfulSplashViewSourceTest {
    @Test
    public void countdownUsesSkyBlueOvalBackgroundInsteadOfRectangle() throws IOException {
        String source = readSplashView();

        assertTrue(source.contains("countdown.setBackground(countdownCircleBackground())"));
        assertTrue(source.contains("background.setShape(GradientDrawable.OVAL)"));
        assertTrue(source.contains("background.setColor(Color.argb(218, 135, 206, 250))"));
        assertFalse(source.contains("countdown.setBackgroundColor("));
    }

    private static String readSplashView() throws IOException {
        Path modulePath = Paths.get("src/main/java/com/visgoe01/carappcommon/MindfulSplashView.java");
        if (Files.exists(modulePath)) {
            return new String(Files.readAllBytes(modulePath), StandardCharsets.UTF_8);
        }
        return new String(
                Files.readAllBytes(
                        Paths.get(
                                "CarAppCommon/src/main/java/com/visgoe01/carappcommon/MindfulSplashView.java")),
                StandardCharsets.UTF_8);
    }
}
