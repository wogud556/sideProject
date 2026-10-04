package com.hanati.bank.refinance.refinance.domain;

import org.junit.jupiter.api.Test;

import static com.hanati.bank.refinance.refinance.domain.RefinanceStatus.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefinanceStatusTransitionTest {

    @Test
    void allows_the_documented_happy_path() {
        assertTrue(RefinanceStatusTransition.isAllowed(DRAFT, REQUESTED));
        assertTrue(RefinanceStatusTransition.isAllowed(REQUESTED, REVIEWING));
        assertTrue(RefinanceStatusTransition.isAllowed(REVIEWING, APPROVED));
        assertTrue(RefinanceStatusTransition.isAllowed(APPROVED, EXECUTING));
        assertTrue(RefinanceStatusTransition.isAllowed(EXECUTING, NEW_LOAN_EXECUTED));
        assertTrue(RefinanceStatusTransition.isAllowed(NEW_LOAN_EXECUTED, REPAYING));
        assertTrue(RefinanceStatusTransition.isAllowed(REPAYING, COMPLETED));
    }

    @Test
    void allows_rejection_only_from_reviewing() {
        assertTrue(RefinanceStatusTransition.isAllowed(REVIEWING, REJECTED));
        assertFalse(RefinanceStatusTransition.isAllowed(REQUESTED, REJECTED));
        assertFalse(RefinanceStatusTransition.isAllowed(APPROVED, REJECTED));
    }

    @Test
    void allows_failure_from_executing_and_repaying_only() {
        assertTrue(RefinanceStatusTransition.isAllowed(EXECUTING, FAILED));
        assertTrue(RefinanceStatusTransition.isAllowed(REPAYING, FAILED));
        assertFalse(RefinanceStatusTransition.isAllowed(APPROVED, FAILED));
    }

    @Test
    void retry_from_failed_can_resume_either_step_but_never_skip_ahead() {
        assertTrue(RefinanceStatusTransition.isAllowed(FAILED, EXECUTING));
        assertTrue(RefinanceStatusTransition.isAllowed(FAILED, REPAYING));
        assertFalse(RefinanceStatusTransition.isAllowed(FAILED, COMPLETED));
        assertFalse(RefinanceStatusTransition.isAllowed(FAILED, NEW_LOAN_EXECUTED));
    }

    @Test
    void terminal_states_allow_no_further_transition() {
        for (RefinanceStatus to : RefinanceStatus.values()) {
            assertFalse(RefinanceStatusTransition.isAllowed(COMPLETED, to));
            assertFalse(RefinanceStatusTransition.isAllowed(REJECTED, to));
            assertFalse(RefinanceStatusTransition.isAllowed(CANCELLED, to));
        }
    }

    @Test
    void cannot_skip_states_in_the_execution_pipeline() {
        assertFalse(RefinanceStatusTransition.isAllowed(APPROVED, NEW_LOAN_EXECUTED));
        assertFalse(RefinanceStatusTransition.isAllowed(APPROVED, REPAYING));
        assertFalse(RefinanceStatusTransition.isAllowed(APPROVED, COMPLETED));
        assertFalse(RefinanceStatusTransition.isAllowed(EXECUTING, REPAYING));
        assertFalse(RefinanceStatusTransition.isAllowed(EXECUTING, COMPLETED));
    }
}
