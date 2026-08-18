package com.vishalgoel.meditationtimer;

public record MeditationLog(String id, long startTimeMs, long endTimeMs, long durationMs) {}
