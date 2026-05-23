package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
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
class BiometricArtifactRetentionProcessorTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private BiometricArtifactRetentionProcessor processor;

    @Test
    void shouldReturnBiometricArtifactResourceType() {
        assertEquals(RetentionResourceType.BIOMETRIC_ARTIFACT, processor.supports());
    }

    @Test
    void shouldCountBiometricArtifactsInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(employeeRepository.countByFaceS3ObjectKeyIsNotNullAndCreatedAtBefore(any(Instant.class)))
                .thenReturn(25L);

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(25L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(employeeRepository, times(1)).countByFaceS3ObjectKeyIsNotNullAndCreatedAtBefore(any(Instant.class));
    }

    @Test
    void shouldClearBiometricDataInApplyMode() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);

        when(employeeRepository.clearBiometricDataBefore(any(Instant.class)))
                .thenReturn(15);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(15L, result.scannedCount());
        assertEquals(15L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(employeeRepository, times(1)).clearBiometricDataBefore(any(Instant.class));
    }

    @Test
    void shouldHandleExceptionInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(employeeRepository.countByFaceS3ObjectKeyIsNotNullAndCreatedAtBefore(any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("ERROR", result.status());
        assertNotNull(result.notes());
    }

    private RetentionPolicy createPolicy(RetentionExecutionMode mode) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "BIOMETRIC_ARTIFACT_POLICY",
                "Delete old biometric artifacts",
                "BIOMETRIC_ARTIFACT",
                365,
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
