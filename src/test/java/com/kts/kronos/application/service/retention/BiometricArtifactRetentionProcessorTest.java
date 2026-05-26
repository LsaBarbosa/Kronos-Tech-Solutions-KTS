package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiometricArtifactRetentionProcessorTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private FaceStorageProvider faceStorageProvider;

    @Mock
    private FaceRecognitionProvider faceRecognitionProvider;

    @InjectMocks
    private BiometricArtifactRetentionProcessor processor;

    @Test
    void shouldReturnBiometricArtifactResourceType() {
        assertEquals(RetentionResourceType.BIOMETRIC_ARTIFACT, processor.supports());
    }

    @Test
    void shouldCountEligibleBiometricArtifactsInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);
        var employeeWithoutConsent = createEmployee("s3-key-1");
        var employeeWithRevokedConsent = createEmployee("s3-key-2");

        when(employeeRepository.findEligibleBiometricArtifactsByMissingConsent())
                .thenReturn(List.of(employeeWithoutConsent));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class)))
                .thenReturn(List.of(employeeWithRevokedConsent));

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(2L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(employeeRepository, times(1)).findEligibleBiometricArtifactsByMissingConsent();
        verify(employeeRepository, times(1)).findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class));
    }

    @Test
    void shouldDeleteBiometricArtifactsInApplyModeWhenS3AndRekognitionSucceed() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var employeeId = UUID.randomUUID();
        var employee = createEmployee("s3-key-1", employeeId);

        when(employeeRepository.findEligibleBiometricArtifactsByMissingConsent())
                .thenReturn(List.of(employee));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class)))
                .thenReturn(List.of());
        when(employeeRepository.clearBiometricDataByEmployeeId(employeeId))
                .thenReturn(1);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(1L, result.scannedCount());
        assertEquals(1L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(faceStorageProvider, times(1)).deleteFaceImage("s3-key-1");
        verify(faceRecognitionProvider, times(1)).deleteFacesByExternalImageId(employeeId);
        verify(employeeRepository, times(1)).clearBiometricDataByEmployeeId(employeeId);
    }

    @Test
    void shouldPreserveEmployeesWithActiveConsent() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(employeeRepository.findEligibleBiometricArtifactsByMissingConsent())
                .thenReturn(List.of());
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class)))
                .thenReturn(List.of());

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals(0L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());
    }

    @Test
    void shouldReturnPartialWhenS3DeletionFails() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var employeeId = UUID.randomUUID();
        var employee = createEmployee("s3-key-1", employeeId);

        when(employeeRepository.findEligibleBiometricArtifactsByMissingConsent())
                .thenReturn(List.of(employee));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class)))
                .thenReturn(List.of());
        doThrow(new RuntimeException("S3 error")).when(faceStorageProvider).deleteFaceImage("s3-key-1");

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(1L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("PARTIAL", result.status());
        assertNotNull(result.notes());
        assertFalse(result.notes().contains(employeeId.toString()));

        verify(faceStorageProvider, times(1)).deleteFaceImage("s3-key-1");
        verify(faceRecognitionProvider, times(0)).deleteFacesByExternalImageId(any());
        verify(employeeRepository, times(0)).clearBiometricDataByEmployeeId(any());
    }

    @Test
    void shouldReturnPartialWhenRekognitionDeletionFails() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var employeeId = UUID.randomUUID();
        var employee = createEmployee("s3-key-1", employeeId);

        when(employeeRepository.findEligibleBiometricArtifactsByMissingConsent())
                .thenReturn(List.of(employee));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class)))
                .thenReturn(List.of());
        doThrow(new RuntimeException("Rekognition error")).when(faceRecognitionProvider).deleteFacesByExternalImageId(employeeId);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(1L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("PARTIAL", result.status());
        assertNotNull(result.notes());
        assertFalse(result.notes().contains(employeeId.toString()));

        verify(faceStorageProvider, times(1)).deleteFaceImage("s3-key-1");
        verify(faceRecognitionProvider, times(1)).deleteFacesByExternalImageId(employeeId);
        verify(employeeRepository, times(0)).clearBiometricDataByEmployeeId(any());
    }

    @Test
    void shouldHandleExceptionInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(employeeRepository.findEligibleBiometricArtifactsByMissingConsent())
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

    private EmployeeEntity createEmployee(String s3Key) {
        return createEmployee(s3Key, UUID.randomUUID());
    }

    private EmployeeEntity createEmployee(String s3Key, UUID employeeId) {
        return EmployeeEntity.builder()
                .employeeId(employeeId)
                .faceS3ObjectKey(s3Key)
                .companyId(UUID.randomUUID())
                .build();
    }
}
