package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BiometricArtifactAnonymizerTest {


    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private FaceStorageProvider faceStorageProvider;

    @Mock
    private FaceRecognitionProvider faceRecognitionProvider;

    @InjectMocks
    private BiometricArtifactAnonymizer anonymizer;

    @Test
    void testSupports() {
        assertEquals(AnonymizationResourceType.BIOMETRIC_ARTIFACT, anonymizer.supports());
    }

    @Test
    void testExecuteDryRunWithNoBiometricData() {
        when(employeeRepository.findById(any())).thenReturn(Optional.empty());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
    }

    @Test
    void testExecuteDryRunWithBiometricData() {
        var employee = createEmployee();
        employee.setFaceS3ObjectKey("s3://bucket/face.jpg");
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.scannedCount());
        assertEquals(0, result.affectedCount());
    }

    @Test
    void testExecuteApplyDeletesS3AndRekognition() {
        var employee = createEmployee();
        employee.setFaceS3ObjectKey("s3://bucket/face.jpg");
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.affectedCount());

        verify(faceStorageProvider, times(1)).deleteFaceImage(anyString());
        verify(faceRecognitionProvider, times(1)).deleteFacesByExternalImageId(any());
        verify(employeeRepository, times(1)).save(any());
    }

    @Test
    void testExecuteApplyClearsS3ObjectKey() {
        var employee = createEmployee();
        employee.setFaceS3ObjectKey("s3://bucket/face.jpg");
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(EmployeeEntity.class);
        verify(employeeRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertNull(saved.getFaceS3ObjectKey());
    }

    @Test
    void testExecuteApplyHandlesS3Failure() {
        var employee = createEmployee();
        employee.setFaceS3ObjectKey("s3://bucket/face.jpg");
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));
        doThrow(new RuntimeException("S3 error")).when(faceStorageProvider).deleteFaceImage(anyString());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("PARTIAL", result.status());
        assertTrue(result.notes().contains("S3 errors"));
    }

    @Test
    void biometricArtifactAnonymizer_shouldNotLogRawS3Key(CapturedOutput output) {
        var employee = createEmployee();
        String rawS3Key = "company/123/employee/456/biometric/face.jpg";
        employee.setFaceS3ObjectKey(rawS3Key);
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));
        doThrow(new RuntimeException("S3 error")).when(faceStorageProvider).deleteFaceImage(anyString());

        anonymizer.execute(createPlan(), "APPLY");

        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("faceStorageRef=" + SensitiveDataMasker.maskStorageReference(rawS3Key)));
        assertTrue(logs.contains("exception_type=RuntimeException"));
        assertFalse(logs.contains(rawS3Key));
        assertFalse(logs.contains("s3Key=" + rawS3Key));
    }

    @Test
    void testExecuteApplyHandlesRekognitionFailure() {
        var employee = createEmployee();
        employee.setFaceS3ObjectKey("s3://bucket/face.jpg");
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));
        doThrow(new RuntimeException("Rekognition error")).when(faceRecognitionProvider).deleteFacesByExternalImageId(any());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("PARTIAL", result.status());
        assertTrue(result.notes().contains("Rekognition errors"));
    }

    @Test
    void testExecuteApplyHandlesException() {
        when(employeeRepository.findById(any())).thenThrow(new RuntimeException("DB error"));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
    }

    @Test
    void testExecuteApplyWithNoEmployee() {
        when(employeeRepository.findById(any())).thenReturn(Optional.empty());

        var result = anonymizer.execute(createPlan(), "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.affectedCount());
        verify(faceStorageProvider, never()).deleteFaceImage(anyString());
        verify(faceRecognitionProvider, never()).deleteFacesByExternalImageId(any());
    }

    @Test
    void testExecuteApplyWithEmployeeHavingNoS3Key() {
        var employee = createEmployee();
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        var result = anonymizer.execute(createPlan(), "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.affectedCount());
        verify(faceStorageProvider, never()).deleteFaceImage(anyString());
    }

    @Test
    void testExecuteDryRunWithEmployeeHavingNoS3Key() {
        var employee = createEmployee();
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        var result = anonymizer.execute(createPlan(), "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
    }

    private AnonymizationPlan createPlan() {
        return new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test anonymization",
                false,
                false,
                true,
                false,
                false,
                false
        );
    }

    private EmployeeEntity createEmployee() {
        return EmployeeEntity.builder()
                .employeeId(UUID.randomUUID())
                .fullName("John Doe")
                .cpf("12345678901")
                .email("john@example.com")
                .jobPosition("Developer")
                .salary(5000.0)
                .active(true)
                .companyId(UUID.randomUUID())
                .build();
    }
}
