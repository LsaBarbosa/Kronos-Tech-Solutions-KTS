package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyHardDeleteServiceCoverageTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private LegalConsentRepository legalConsentRepository;
    @Mock private LgpdRequestRepository lgpdRequestRepository;
    @Mock private AnonymizationConsolidatedResultRepository anonymizationRepository;
    @Mock private UserCompanyAccessRepository userCompanyAccessRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private FaceStorageProvider faceStorageProvider;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;
    @Mock private DocumentRepository documentRepository;
    @Mock private BucketStorageProvider bucketStorageProvider;

    @InjectMocks
    private CompanyHardDeleteService service;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
    }

    // ── L72: faceKey != null && !isBlank() = FALSE (blank key) ──────────────────

    @Test
    void hardDelete_withBlankFaceKey_skipsFaceStorageDeletion() {
        UUID empId = UUID.randomUUID();
        EmployeeEntity emp = new EmployeeEntity();
        emp.setEmployeeId(empId);
        emp.setFaceS3ObjectKey("   ");

        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId)).thenReturn(List.of());

        service.hardDelete(companyId, "12345678000100", "Empresa Blank Face");

        verify(faceStorageProvider, never()).deleteFaceImage(anyString());
    }

    // ── L85: doc.getStoragePath() == null → condition FALSE ─────────────────────

    @Test
    void hardDelete_withNullDocumentStoragePath_skipsS3Deletion() {
        UUID empId = UUID.randomUUID();
        EmployeeEntity emp = new EmployeeEntity();
        emp.setEmployeeId(empId);
        emp.setFaceS3ObjectKey(null);

        DocumentEntity docNullPath = new DocumentEntity();
        docNullPath.setType(DocumentType.PAYSLIP);
        docNullPath.setStoragePath(null);

        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId))
            .thenReturn(List.of(docNullPath));

        service.hardDelete(companyId, "12345678000100", "Empresa Null Doc Path");

        verify(bucketStorageProvider, never()).deleteFile(any(), any());
    }

    // ── L85: doc.getStoragePath() != null && isBlank() → condition FALSE ─────────

    @Test
    void hardDelete_withBlankDocumentStoragePath_skipsS3Deletion() {
        UUID empId = UUID.randomUUID();
        EmployeeEntity emp = new EmployeeEntity();
        emp.setEmployeeId(empId);
        emp.setFaceS3ObjectKey(null);

        DocumentEntity docBlankPath = new DocumentEntity();
        docBlankPath.setType(DocumentType.PAYSLIP);
        docBlankPath.setStoragePath("   ");

        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId))
            .thenReturn(List.of(docBlankPath));

        service.hardDelete(companyId, "12345678000100", "Empresa Blank Doc Path");

        verify(bucketStorageProvider, never()).deleteFile(any(), any());
    }
}
