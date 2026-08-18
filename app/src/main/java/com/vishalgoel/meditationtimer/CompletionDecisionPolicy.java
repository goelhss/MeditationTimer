package com.vishalgoel.meditationtimer;

public final class CompletionDecisionPolicy {
    private CompletionDecisionPolicy() {}

    public static boolean shouldDefaultToYes(long decisionDeadlineMs, long nowMs) {
        return nowMs >= decisionDeadlineMs;
    }

    public static long secondsRemaining(long decisionDeadlineMs, long nowMs) {
        return Math.max(0L, (decisionDeadlineMs - nowMs + 999L) / 1000L);
    }
}
