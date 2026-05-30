package com.kts.kronos.application.service;

import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LgpdSlaPolicyServiceTest {

    private final LgpdSlaPolicyService service = new LgpdSlaPolicyService();

    @Test
    void shouldApplyDefaultArt18SlaToAdditionalRequestTypes() {
        Instant createdAt = Instant.parse("2026-05-29T10:00:00Z");

        assertEquals(
                createdAt.plus(15, ChronoUnit.DAYS),
                service.calculateDueAt(LgpdRequestType.CONSENT_INFORMATION, createdAt));
        assertEquals(
                createdAt.plus(15, ChronoUnit.DAYS),
                service.calculateDueAt(LgpdRequestType.OPPOSITION, createdAt));
        assertEquals(
                createdAt.plus(15, ChronoUnit.DAYS),
                service.calculateDueAt(LgpdRequestType.AUTOMATED_DECISION_REVIEW, createdAt));
    }
}
