package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AnonymizationConsolidatedResultEntity;
import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AnonymizationConsolidatedResultRepositoryTest {

    @Autowired
    private AnonymizationConsolidatedResultRepository repository;

    @Test
    void shouldSaveAndRetrieveByRequestId() {
        UUID consolidatedExecutionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID requestedByUserId = UUID.randomUUID();
        Instant now = Instant.now();

        AnonymizationConsolidatedResultEntity entity = new AnonymizationConsolidatedResultEntity(
                consolidatedExecutionId,
                requestId,
                employeeId,
                companyId,
                requestedByUserId,
                AnonymizationConsolidatedStatus.SUCCESS,
                "APPLY",
                100L,
                50L,
                10L,
                0L,
                null,
                null,
                now,
                now,
                now
        );

        repository.save(entity);

        Optional<AnonymizationConsolidatedResultEntity> retrieved = repository.findByRequestId(requestId);

        assertTrue(retrieved.isPresent());
        assertEquals(consolidatedExecutionId, retrieved.get().getConsolidatedExecutionId());
        assertEquals(requestId, retrieved.get().getRequestId());
        assertEquals(employeeId, retrieved.get().getEmployeeId());
        assertEquals(companyId, retrieved.get().getCompanyId());
        assertEquals(AnonymizationConsolidatedStatus.SUCCESS, retrieved.get().getConsolidatedStatus());
    }

    @Test
    void shouldReturnEmptyWhenRequestIdNotFound() {
        UUID nonexistentRequestId = UUID.randomUUID();

        Optional<AnonymizationConsolidatedResultEntity> retrieved = repository.findByRequestId(nonexistentRequestId);

        assertTrue(retrieved.isEmpty());
    }

    @Test
    void shouldFindLatestByEmployeeAndCompanyOrderedByFinishedAt() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID requestedByUserId = UUID.randomUUID();
        Instant now = Instant.now();

        // Save first result (older)
        AnonymizationConsolidatedResultEntity first = new AnonymizationConsolidatedResultEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                employeeId,
                companyId,
                requestedByUserId,
                AnonymizationConsolidatedStatus.SUCCESS,
                "APPLY",
                50L, 25L, 5L, 0L,
                null, null,
                now.minusSeconds(100),
                now.minusSeconds(50),
                now.minusSeconds(50)
        );
        repository.save(first);

        // Save second result (newer)
        Instant laterTime = now.plusSeconds(100);
        AnonymizationConsolidatedResultEntity second = new AnonymizationConsolidatedResultEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                employeeId,
                companyId,
                requestedByUserId,
                AnonymizationConsolidatedStatus.PARTIAL_SUCCESS,
                "APPLY",
                100L, 75L, 10L, 5L,
                "USER", "Some warning",
                laterTime.minusSeconds(100),
                laterTime,
                laterTime
        );
        repository.save(second);

        Optional<AnonymizationConsolidatedResultEntity> latest = repository.findLatestByEmployeeAndCompany(employeeId, companyId);

        assertTrue(latest.isPresent());
        assertEquals(second.getConsolidatedExecutionId(), latest.get().getConsolidatedExecutionId());
        assertEquals(AnonymizationConsolidatedStatus.PARTIAL_SUCCESS, latest.get().getConsolidatedStatus());
    }

    @Test
    void shouldReturnEmptyWhenNoRecordsForEmployeeCompany() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Optional<AnonymizationConsolidatedResultEntity> result = repository.findLatestByEmployeeAndCompany(employeeId, companyId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldStoreForeignKeyReferences() {
        UUID consolidatedExecutionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID requestedByUserId = UUID.randomUUID();
        Instant now = Instant.now();

        AnonymizationConsolidatedResultEntity entity = new AnonymizationConsolidatedResultEntity(
                consolidatedExecutionId,
                requestId,
                employeeId,
                companyId,
                requestedByUserId,
                AnonymizationConsolidatedStatus.FAILED,
                "APPLY",
                100L, 0L, 0L, 100L,
                "EMPLOYEE,USER",
                "Employee anonymization failed\nUser anonymization failed",
                now,
                now,
                now
        );

        repository.save(entity);

        Optional<AnonymizationConsolidatedResultEntity> retrieved = repository.findByRequestId(requestId);

        assertTrue(retrieved.isPresent());
        assertEquals("EMPLOYEE,USER", retrieved.get().getFailedDomains());
        assertEquals("Employee anonymization failed\nUser anonymization failed", retrieved.get().getWarnings());
    }
}
