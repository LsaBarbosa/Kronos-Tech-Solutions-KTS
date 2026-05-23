package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.port.out.provider.AnonymizationExecutionLogProvider;
import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AnonymizationPlanExecutorConsolidationTest {

    @Mock
    private AnonymizationExecutionLogProvider executionLogProvider;

    @InjectMocks
    private AnonymizationPlanExecutor executor;

    @Test
    void testConsolidateAllSuccessful() {
        UUID consolidatedId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant start = Instant.now();

        var result1 = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.TIME_RECORD,
                "APPLY",
                100,
                50,
                50
        );

        var result2 = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.DOCUMENT,
                "APPLY",
                50,
                30,
                20
        );

        AnonymizationConsolidatedResult consolidated = AnonymizationConsolidatedResult.consolidate(
                consolidatedId,
                employeeId,
                companyId,
                userId,
                "APPLY",
                start,
                Arrays.asList(result1, result2)
        );

        assertEquals(AnonymizationConsolidatedStatus.SUCCESS, consolidated.consolidatedStatus());
        assertEquals(150, consolidated.totalScanned());
        assertEquals(80, consolidated.totalAffected());
        assertEquals(70, consolidated.totalSkipped());
        assertEquals(0, consolidated.totalErrors());
        assertTrue(consolidated.failedDomains().isEmpty());
    }

    @Test
    void testConsolidatePartialSuccess() {
        UUID consolidatedId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant start = Instant.now();

        var result1 = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.TIME_RECORD,
                "APPLY",
                100,
                100,
                0
        );

        var result2 = AnonymizationExecutionResult.error(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.DOCUMENT,
                "APPLY",
                5,
                "S3 connection timeout"
        );

        AnonymizationConsolidatedResult consolidated = AnonymizationConsolidatedResult.consolidate(
                consolidatedId,
                employeeId,
                companyId,
                userId,
                "APPLY",
                start,
                Arrays.asList(result1, result2)
        );

        assertEquals(AnonymizationConsolidatedStatus.PARTIAL_SUCCESS, consolidated.consolidatedStatus());
        assertEquals(100, consolidated.totalScanned());
        assertEquals(100, consolidated.totalAffected());
        assertEquals(0, consolidated.totalSkipped());
        assertEquals(5, consolidated.totalErrors());
        assertTrue(consolidated.failedDomains().contains("DOCUMENT"));
    }

    @Test
    void testConsolidateAllFailed() {
        UUID consolidatedId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant start = Instant.now();

        var result1 = AnonymizationExecutionResult.error(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.TIME_RECORD,
                "APPLY",
                3,
                "Database error"
        );

        var result2 = AnonymizationExecutionResult.error(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.DOCUMENT,
                "APPLY",
                5,
                "S3 unavailable"
        );

        AnonymizationConsolidatedResult consolidated = AnonymizationConsolidatedResult.consolidate(
                consolidatedId,
                employeeId,
                companyId,
                userId,
                "APPLY",
                start,
                Arrays.asList(result1, result2)
        );

        assertEquals(AnonymizationConsolidatedStatus.FAILED, consolidated.consolidatedStatus());
        assertEquals(0, consolidated.totalScanned());
        assertEquals(0, consolidated.totalAffected());
        assertEquals(0, consolidated.totalSkipped());
        assertEquals(8, consolidated.totalErrors());
        assertEquals(2, consolidated.failedDomains().size());
    }

    @Test
    void testConsolidateWithNullResults() {
        UUID consolidatedId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant start = Instant.now();

        var result1 = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.TIME_RECORD,
                "APPLY",
                100,
                80,
                20
        );

        AnonymizationConsolidatedResult consolidated = AnonymizationConsolidatedResult.consolidate(
                consolidatedId,
                employeeId,
                companyId,
                userId,
                "APPLY",
                start,
                Arrays.asList(result1, null)
        );

        assertEquals(AnonymizationConsolidatedStatus.SUCCESS, consolidated.consolidatedStatus());
        assertEquals(100, consolidated.totalScanned());
        assertEquals(80, consolidated.totalAffected());
        assertEquals(20, consolidated.totalSkipped());
    }

    @Test
    void testConsolidateIsSuccessFlag() {
        UUID consolidatedId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant start = Instant.now();

        var result = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.TIME_RECORD,
                "APPLY",
                10,
                10,
                0
        );

        AnonymizationConsolidatedResult consolidated = AnonymizationConsolidatedResult.consolidate(
                consolidatedId,
                employeeId,
                companyId,
                userId,
                "APPLY",
                start,
                Arrays.asList(result)
        );

        assertTrue(consolidated.isSuccess());
        assertEquals(0, consolidated.failedDomains().size());
    }

    @Test
    void testConsolidateIsPartialSuccessFlag() {
        UUID consolidatedId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant start = Instant.now();

        var result1 = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.TIME_RECORD,
                "APPLY",
                10,
                10,
                0
        );

        var result2 = AnonymizationExecutionResult.error(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                AnonymizationResourceType.DOCUMENT,
                "APPLY",
                1,
                "Error"
        );

        AnonymizationConsolidatedResult consolidated = AnonymizationConsolidatedResult.consolidate(
                consolidatedId,
                employeeId,
                companyId,
                userId,
                "APPLY",
                start,
                Arrays.asList(result1, result2)
        );

        assertTrue(consolidated.isPartialSuccess());
        assertEquals(1, consolidated.failedDomains().size());
    }
}
