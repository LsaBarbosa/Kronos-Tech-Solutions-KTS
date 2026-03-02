package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.out.persistence.TimeRecordApprovalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimeRecordApprovalCleanupSchedulerTest {

    @Mock TimeRecordApprovalRepository repository;
    @InjectMocks TimeRecordApprovalCleanupScheduler scheduler;

    @Test
    void cleanupOldApprovalsShouldDeleteWhenNoError() {
        scheduler.cleanupOldApprovals();
        verify(repository).deleteByCreatedAtBefore(any());
    }

    @Test
    void cleanupOldApprovalsShouldSwallowException() {
        doThrow(new RuntimeException("db error")).when(repository).deleteByCreatedAtBefore(any());

        assertDoesNotThrow(() -> scheduler.cleanupOldApprovals());
        verify(repository).deleteByCreatedAtBefore(any());
    }
}
