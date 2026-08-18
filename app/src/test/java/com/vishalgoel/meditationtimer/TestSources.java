package com.vishalgoel.meditationtimer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class TestSources {
    private TestSources() {}

    static String read(String rootRelativePath) throws IOException {
        Path[] candidates = {
                Paths.get(rootRelativePath),
                Paths.get("..", rootRelativePath),
                rootRelativePath.startsWith("app/")
                        ? Paths.get(rootRelativePath.substring(4)) : Paths.get("__missing__")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
        }
        throw new IOException("Could not find source file: " + rootRelativePath);
    }
}
