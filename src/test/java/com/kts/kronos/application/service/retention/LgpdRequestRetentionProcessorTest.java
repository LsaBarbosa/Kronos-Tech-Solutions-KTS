package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LgpdRequestRetentionProcessorTest {

    @Mock
    private LgpdRequestRepository lgpdRequestRepository;

    @InjectMocks
    private LgpdRequestRetentionProcessor processor;

    @Test
    void shouldReturnLgpdRequestResourceType() {
        assertEquals(RetentionResourceType.LGPD_REQUEST, processor.supports());
    }

    @Test
    void shouldCountLgpdRequestsInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(lgpdRequestRepository.countCreatedBefore(any(Instant.class)))
                .thenReturn(75L);

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(75L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(lgpdRequestRepository, times(1)).countCreatedBefore(any(Instant.class));
    }

    @Test
    void shouldDeleteLgpdRequestsInApplyMode() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);

        when(lgpdRequestRepository.deleteCreatedBefore(any(Instant.class)))
                .thenReturn(60);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(60L, result.scannedCount());
        assertEquals(60L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(lgpdRequestRepository, times(1)).deleteCreatedBefore(any(Instant.class));
    }

    @Test
    void shouldHandleExceptionDuringExecution() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);

        when(lgpdRequestRepository.deleteCreatedBefore(any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

        var result = processor.execute(policy, "APPLY");

        assertEquals("ERROR", result.status());
        assertNotNull(result.notes());
    }

    private RetentionPolicy createPolicy(RetentionExecutionMode mode) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "LGPD_REQUEST_POLICY",
                "Delete old LGPD requests",
                "LGPD_REQUEST",
                180,
                mode,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );
    }
}
