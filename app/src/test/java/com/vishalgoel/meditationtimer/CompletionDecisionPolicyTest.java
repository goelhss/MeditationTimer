package com.vishalgoel.meditationtimer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CompletionDecisionPolicyTest {
    @Test
    public void defaultsToYesOnlyAtDeadline() {
        assertFalse(CompletionDecisionPolicy.shouldDefaultToYes(10_000L, 9_999L));
        assertTrue(CompletionDecisionPolicy.shouldDefaultToYes(10_000L, 10_000L));
    }

    @Test
    public void countdownRoundsUpAndStopsAtZero() {
        assertEquals(10L, CompletionDecisionPolicy.secondsRemaining(10_000L, 1L));
        assertEquals(1L, CompletionDecisionPolicy.secondsRemaining(10_000L, 9_001L));
        assertEquals(0L, CompletionDecisionPolicy.secondsRemaining(10_000L, 11_000L));
    }
}
