package com.kts.kronos.application.service;

import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class LgpdSlaPolicyServiceTest {

    private final LgpdSlaPolicyService service = new LgpdSlaPolicyService();

    @Test
    void shouldApply15DaySlaForAllStandardTypes() {
        Instant createdAt = Instant.parse("2026-05-29T10:00:00Z");
        Instant expected15 = createdAt.plus(15, ChronoUnit.DAYS);

        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.CONFIRM_PROCESSING, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.ACCESS, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.CORRECTION, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.ANONYMIZATION, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.BLOCKING, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.DELETION, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.PORTABILITY, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.SHARING_INFORMATION, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.CONSENT_INFORMATION, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.OPPOSITION, createdAt));
        assertEquals(expected15, service.calculateDueAt(LgpdRequestType.AUTOMATED_DECISION_REVIEW, createdAt));
    }

    @Test
    void shouldApply2DaySlaForConsentRevocation() {
        Instant createdAt = Instant.parse("2026-05-29T10:00:00Z");
        assertEquals(createdAt.plus(2, ChronoUnit.DAYS),
                service.calculateDueAt(LgpdRequestType.CONSENT_REVOCATION, createdAt));
    }

    @Test
    void shouldReturnTrueWhenOverdue() {
        Instant pastDue = Instant.now().minus(1, ChronoUnit.DAYS);
        assertTrue(service.isOverdue(pastDue));
    }

    @Test
    void shouldReturnFalseWhenNotOverdue() {
        Instant futureDue = Instant.now().plus(1, ChronoUnit.DAYS);
        assertFalse(service.isOverdue(futureDue));
    }

    @Test
    void shouldReturnFalseWhenDueAtIsNull() {
        assertFalse(service.isOverdue(null));
    }

    @Test
    void shouldReturnZeroDaysRemainingWhenDueAtIsNull() {
        assertEquals(0, service.daysRemaining(null));
    }

    @Test
    void shouldReturnPositiveDaysRemainingForFutureDue() {
        Instant futureDue = Instant.now().plus(10, ChronoUnit.DAYS);
        assertTrue(service.daysRemaining(futureDue) > 0);
    }

    @Test
    void shouldReturnNormalPriorityWhenDueAtIsNull() {
        assertEquals("NORMAL", service.priorityBySla(null));
    }

    @Test
    void shouldReturnOverduePriorityWhenPastDue() {
        Instant pastDue = Instant.now().minus(2, ChronoUnit.DAYS);
        assertEquals("OVERDUE", service.priorityBySla(pastDue));
    }

    @Test
    void shouldReturnUrgentPriorityWithin3Days() {
        Instant soonDue = Instant.now().plus(2, ChronoUnit.DAYS);
        assertEquals("URGENT", service.priorityBySla(soonDue));
    }

    @Test
    void shouldReturnHighPriorityWithin7Days() {
        Instant medDue = Instant.now().plus(5, ChronoUnit.DAYS);
        assertEquals("HIGH", service.priorityBySla(medDue));
    }

    @Test
    void shouldReturnNormalPriorityWhenMoreThan7DaysRemaining() {
        Instant farDue = Instant.now().plus(14, ChronoUnit.DAYS);
        assertEquals("NORMAL", service.priorityBySla(farDue));
    }
}
