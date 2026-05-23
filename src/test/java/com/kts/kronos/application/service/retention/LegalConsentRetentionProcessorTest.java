package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.LegalConsentRepository;
import com.kts.kronos.domain.model.RetentionExecutionResult;
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
class LegalConsentRetentionProcessorTest {

    @Mock
    private LegalConsentRepository legalConsentRepository;

    @InjectMocks
    private LegalConsentRetentionProcessor processor;

    @Test
    void shouldReturnLegalConsentResourceType() {
        assertEquals(RetentionResourceType.LEGAL_CONSENT, processor.supports());
    }

    @Test
    void shouldCountLegalConsentsInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(legalConsentRepository.countCreatedBefore(any(Instant.class)))
                .thenReturn(100L);

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(100L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(legalConsentRepository, times(1)).countCreatedBefore(any(Instant.class));
    }

    @Test
    void shouldDeleteLegalConsentsInApplyMode() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);

        when(legalConsentRepository.deleteCreatedBefore(any(Instant.class)))
                .thenReturn(50);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(50L, result.scannedCount());
        assertEquals(50L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(legalConsentRepository, times(1)).deleteCreatedBefore(any(Instant.class));
    }

    @Test
    void shouldHandleExceptionInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(legalConsentRepository.countCreatedBefore(any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("ERROR", result.status());
        assertNotNull(result.notes());
    }

    @Test
    void shouldHandleExceptionInApplyMode() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);

        when(legalConsentRepository.deleteCreatedBefore(any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

        var result = processor.execute(policy, "APPLY");

        assertEquals("ERROR", result.status());
        assertNotNull(result.notes());
    }

    private RetentionPolicy createPolicy(RetentionExecutionMode mode) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "LEGAL_CONSENT_POLICY",
                "Delete old legal consents",
                "LEGAL_CONSENT",
                90,
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
