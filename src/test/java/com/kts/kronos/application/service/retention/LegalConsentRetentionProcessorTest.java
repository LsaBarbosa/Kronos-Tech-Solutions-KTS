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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void shouldCountRevokedLegalConsentsInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(legalConsentRepository.countRevokedConsentsBefore(any(Instant.class)))
                .thenReturn(25L);

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(25L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(legalConsentRepository, times(1)).countRevokedConsentsBefore(any(Instant.class));
    }

    @Test
    void shouldMinimizeRevokedLegalConsentsInApplyMode() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);

        when(legalConsentRepository.minimizeRevokedConsentsBefore(any(Instant.class), any(Instant.class), any(String.class)))
                .thenReturn(15);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(15L, result.scannedCount());
        assertEquals(15L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(legalConsentRepository, times(1)).minimizeRevokedConsentsBefore(any(Instant.class), any(Instant.class), any(String.class));
    }

    @Test
    void shouldNotModifyActiveConsents() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);

        // Only revoked consents are minimized, active ones are untouched
        when(legalConsentRepository.minimizeRevokedConsentsBefore(any(Instant.class), any(Instant.class), any(String.class)))
                .thenReturn(0);  // No revoked old consents

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());
    }

    @Test
    void shouldHandleExceptionInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(legalConsentRepository.countRevokedConsentsBefore(any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("ERROR", result.status());
        assertNotNull(result.notes());
    }

    @Test
    void shouldHandleExceptionInApplyMode() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);

        when(legalConsentRepository.minimizeRevokedConsentsBefore(any(Instant.class), any(Instant.class), any(String.class)))
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

    @Test
    void supportsApply_returnsTrue() {
        assertTrue(processor.supportsApply());
    }

}