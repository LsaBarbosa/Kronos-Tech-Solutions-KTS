package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.port.out.provider.LegalTextProvider;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private LegalTextProvider legalTextProvider;

    @InjectMocks
    private BiometricArtifactRetentionProcessor processor;

    @Test
    void shouldReturnBiometricArtifactResourceType() {
        assertEquals(RetentionResourceType.BIOMETRIC_ARTIFACT, processor.supports());
    }

    @Test
    void shouldNotFailDryRunWhenConsentBasedPolicyHasNullRetentionDays() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);
        var employeeWithoutConsent = createEmployee("s3-key-1");

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(anyString(), anyString()))
                .thenReturn(List.of(employeeWithoutConsent));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class), anyString(), anyString()))
                .thenReturn(List.of());

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(1L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());
    }

    @Test
    void shouldTreatEmployeesWithoutValidCurrentConsentAsEligible() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);
        var employeeWithoutConsent = createEmployee("s3-key-1");

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(anyString(), anyString()))
                .thenReturn(List.of(employeeWithoutConsent));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class), anyString(), anyString()))
                .thenReturn(List.of());

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals(1L, result.scannedCount());
        assertEquals("SUCCESS", result.status());

        verify(employeeRepository, times(1)).findEligibleBiometricArtifactsWithoutValidCurrentConsent("v2", "hash-v2");
        verify(employeeRepository, times(1)).findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class), anyString(), anyString());
    }

    @Test
    void shouldDeleteBiometricArtifactsInApplyModeWhenS3AndRekognitionSucceed() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var employeeId = UUID.randomUUID();
        var employee = createEmployee("s3-key-1", employeeId);

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(anyString(), anyString()))
                .thenReturn(List.of(employee));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class), anyString(), anyString()))
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

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(anyString(), anyString()))
                .thenReturn(List.of());
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class), anyString(), anyString()))
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

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(anyString(), anyString()))
                .thenReturn(List.of(employee));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class), anyString(), anyString()))
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

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(anyString(), anyString()))
                .thenReturn(List.of(employee));
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class), anyString(), anyString()))
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

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("ERROR", result.status());
        assertNotNull(result.notes());
    }

    @Test
    void shouldApplyRevokedConsentGracePeriod() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);
        ReflectionTestUtils.setField(processor, "revokedConsentGraceDays", 3);

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(anyString(), anyString()))
                .thenReturn(List.of());
        when(employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(any(Instant.class), anyString(), anyString()))
                .thenReturn(List.of());

        processor.execute(policy, "DRY_RUN");

        var minCutoff = Instant.now().minusSeconds((3L * 24 * 60 * 60) + 5);
        var maxCutoff = Instant.now().minusSeconds((3L * 24 * 60 * 60) - 5);
        verify(employeeRepository).findEligibleBiometricArtifactsByRevokedConsent(
                argThat(cutoff -> !cutoff.isBefore(minCutoff) && !cutoff.isAfter(maxCutoff)),
                eq("v2"),
                eq("hash-v2")
        );
    }

    private RetentionPolicy createPolicy(RetentionExecutionMode mode) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                "Retenção de artefatos biométricos por consentimento vigente",
                RetentionPolicyType.CONSENT_BASED,
                "BIOMETRIC_ARTIFACT",
                null,
                mode,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    private LegalText currentBiometricTerm() {
        return new LegalText(
                UUID.randomUUID(),
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "v2",
                "Biometric consent",
                "content",
                "hash-v2",
                true,
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
