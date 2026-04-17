package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.out.persistence.TimeRecordApprovalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimeRecordApprovalCleanupSchedulerTest {

    @Mock
    private TimeRecordApprovalRepository repository;

    @Test
    @DisplayName("cleanupOldApprovals: remove aprovações com threshold de 31 dias")
    void shouldDeleteApprovalsOlderThanThirtyOneDays() {
        TimeRecordApprovalCleanupScheduler scheduler = new TimeRecordApprovalCleanupScheduler(repository);

        LocalDateTime lowerBound = LocalDateTime.now(com.kts.kronos.constants.Messages.SAO_PAULO).minusDays(31).minusSeconds(2);
        scheduler.cleanupOldApprovals();
        LocalDateTime upperBound = LocalDateTime.now(com.kts.kronos.constants.Messages.SAO_PAULO).minusDays(31).plusSeconds(2);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByCreatedAtBefore(captor.capture());

        LocalDateTime threshold = captor.getValue();
        assertTrue(!threshold.isBefore(lowerBound) && !threshold.isAfter(upperBound));
    }

    @Test
    @DisplayName("cleanupOldApprovals: absorve exceção do repository")
    void shouldSwallowRepositoryException() {
        TimeRecordApprovalCleanupScheduler scheduler = new TimeRecordApprovalCleanupScheduler(repository);
        doThrow(new RuntimeException("db error"))
                .when(repository).deleteByCreatedAtBefore(org.mockito.ArgumentMatchers.any());

        assertDoesNotThrow(scheduler::cleanupOldApprovals);
    }
}