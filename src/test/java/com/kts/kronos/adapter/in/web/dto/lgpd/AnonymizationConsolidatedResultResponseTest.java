package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnonymizationConsolidatedResultResponseTest {

    @Test
    void shouldMapFromConsolidatedResult() {
        var startedAt = Instant.parse("2026-01-01T10:00:00Z");
        var finishedAt = Instant.parse("2026-01-01T10:00:01Z");

        var domainResult = AnonymizationExecutionResult.success(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationResourceType.USER, "DRY_RUN", 5, 5, 0
        );

        var consolidatedResult = new AnonymizationConsolidatedResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationConsolidatedStatus.SUCCESS, "DRY_RUN",
                startedAt, finishedAt, 5, 5, 0, 0,
                List.of(domainResult), List.of(), List.of()
        );

        var response = AnonymizationConsolidatedResultResponse.from(consolidatedResult);

        assertEquals("SUCCESS", response.consolidatedStatus());
        assertEquals("DRY_RUN", response.executionMode());
        assertEquals(1000L, response.durationMs());
        assertNotNull(response.summary());
        assertEquals(5, response.summary().totalScanned());
        assertEquals(5, response.summary().totalAffected());
        assertEquals(0, response.summary().totalSkipped());
        assertEquals(0, response.summary().totalErrors());
        assertEquals(1, response.domainResults().size());
        assertEquals("USER", response.domainResults().get(0).resourceType());
    }

    @Test
    void shouldFilterNullDomainResults() {
        var startedAt = Instant.parse("2026-01-01T10:00:00Z");
        var finishedAt = Instant.parse("2026-01-01T10:00:02Z");

        var consolidatedResult = new AnonymizationConsolidatedResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationConsolidatedStatus.PARTIAL_SUCCESS, "DRY_RUN",
                startedAt, finishedAt, 0, 0, 0, 0,
                List.of(), List.of("USER"), List.of("Warning")
        );

        var response = AnonymizationConsolidatedResultResponse.from(consolidatedResult);

        assertEquals("PARTIAL_SUCCESS", response.consolidatedStatus());
        assertTrue(response.domainResults().isEmpty());
        assertEquals(List.of("USER"), response.failedDomains());
        assertEquals(List.of("Warning"), response.warnings());
    }

    @Test
    void shouldFilterNullElementInDomainResultsList() {
        // domainResults list contains null entry → filter(r -> r != null) = FALSE branch covered
        var startedAt = Instant.parse("2026-01-01T10:00:00Z");
        var finishedAt = Instant.parse("2026-01-01T10:00:03Z");

        var domainResult = AnonymizationExecutionResult.success(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationResourceType.EMPLOYEE, "DRY_RUN", 3, 3, 0
        );

        // Arrays.asList allows null; List.of would throw NullPointerException
        var domainResults = Arrays.asList(domainResult, null);

        var consolidatedResult = new AnonymizationConsolidatedResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationConsolidatedStatus.SUCCESS, "DRY_RUN",
                startedAt, finishedAt, 3, 3, 0, 0,
                domainResults, List.of(), List.of()
        );

        var response = AnonymizationConsolidatedResultResponse.from(consolidatedResult);

        // null filtered out → only 1 domain result in response
        assertEquals(1, response.domainResults().size());
        assertEquals("EMPLOYEE", response.domainResults().get(0).resourceType());
    }
}
