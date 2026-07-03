package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyHardDeleteServiceTest {

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
    private String cnpj;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        cnpj = "12345678000195";
    }

    @Test
    void hardDelete_withEmployeesAndBiometrics_cleansExternalThenDb() {
        UUID empId1 = UUID.randomUUID();
        UUID empId2 = UUID.randomUUID();
        var emp1 = employeeEntity(empId1, "face-key-1");
        var emp2 = employeeEntity(empId2, "face-key-2");
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp1, emp2));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId1)).thenReturn(List.of());
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId2)).thenReturn(List.of());

        var result = service.hardDelete(companyId, cnpj, "Empresa Teste");

        verify(faceRecognitionProvider).deleteFacesByExternalImageId(empId1);
        verify(faceRecognitionProvider).deleteFacesByExternalImageId(empId2);
        verify(faceStorageProvider).deleteFaceImage("face-key-1");
        verify(faceStorageProvider).deleteFaceImage("face-key-2");
        verify(anonymizationRepository).deleteByEmployeeId(empId1);
        verify(anonymizationRepository).deleteByEmployeeId(empId2);
        verify(lgpdRequestRepository).deleteByEmployeeId(empId1);
        verify(lgpdRequestRepository).deleteByEmployeeId(empId2);
        verify(legalConsentRepository).deleteByEmployeeId(empId1);
        verify(legalConsentRepository).deleteByEmployeeId(empId2);
        verify(userCompanyAccessRepository).deleteByCompanyId(companyId);
        verify(employeeRepository).deleteAll(List.of(emp1, emp2));
        verify(companyRepository).deleteById(companyId);

        assertThat(result.employeesDeleted()).isEqualTo(2);
        assertThat(result.externalCleanupFailures()).isEqualTo(0);
    }

    @Test
    void hardDelete_withNoEmployees_deletesOnlyCompany() {
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of());

        var result = service.hardDelete(companyId, cnpj, "Empresa Vazia");

        verifyNoInteractions(faceStorageProvider, faceRecognitionProvider,
                documentRepository, bucketStorageProvider,
                legalConsentRepository, lgpdRequestRepository, anonymizationRepository);
        verify(userCompanyAccessRepository).deleteByCompanyId(companyId);
        verify(companyRepository).deleteById(companyId);
        assertThat(result.employeesDeleted()).isEqualTo(0);
        assertThat(result.externalCleanupFailures()).isEqualTo(0);
    }

    @Test
    void hardDelete_withNoFaceKey_skipsS3ButCallsRekognition() {
        UUID empId = UUID.randomUUID();
        var emp = employeeEntity(empId, null); // no face key
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId)).thenReturn(List.of());

        service.hardDelete(companyId, cnpj, "Empresa Sem Face");

        verify(faceRecognitionProvider).deleteFacesByExternalImageId(empId);
        verifyNoInteractions(faceStorageProvider);
    }

    @Test
    void hardDelete_withDocuments_deletesDocumentFilesFromS3() {
        UUID empId = UUID.randomUUID();
        var emp = employeeEntity(empId, null);
        var doc1 = documentEntity("docs/holerite.pdf", DocumentType.PAYSLIP);
        var doc2 = documentEntity("docs/ferias.pdf", DocumentType.TIME_OFF);
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId))
                .thenReturn(List.of(doc1, doc2));

        var result = service.hardDelete(companyId, cnpj, "Empresa Com Docs");

        verify(bucketStorageProvider).deleteFile(DocumentType.PAYSLIP, "docs/holerite.pdf");
        verify(bucketStorageProvider).deleteFile(DocumentType.TIME_OFF, "docs/ferias.pdf");
        assertThat(result.externalCleanupFailures()).isEqualTo(0);
    }

    @Test
    void hardDelete_whenRekognitionFails_continuesAndAccumulatesFailure() {
        UUID empId = UUID.randomUUID();
        var emp = employeeEntity(empId, "face-key");
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId)).thenReturn(List.of());
        doThrow(new RuntimeException("AWS error")).when(faceRecognitionProvider)
                .deleteFacesByExternalImageId(empId);

        var result = service.hardDelete(companyId, cnpj, "Empresa Teste");

        verify(faceStorageProvider).deleteFaceImage("face-key");
        verify(companyRepository).deleteById(companyId);
        assertThat(result.externalCleanupFailures()).isEqualTo(1);
        assertThat(result.externalFailureDetails()).hasSize(1);
    }

    @Test
    void hardDelete_whenS3DocumentFails_continuesAndAccumulatesFailure() {
        UUID empId = UUID.randomUUID();
        var emp = employeeEntity(empId, null);
        var doc = documentEntity("docs/arquivo.pdf", DocumentType.PAYSLIP);
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId)).thenReturn(List.of(doc));
        doThrow(new RuntimeException("S3 error")).when(bucketStorageProvider)
                .deleteFile(DocumentType.PAYSLIP, "docs/arquivo.pdf");

        var result = service.hardDelete(companyId, cnpj, "Empresa Teste");

        verify(companyRepository).deleteById(companyId);
        assertThat(result.externalCleanupFailures()).isEqualTo(1);
    }

    @Test
    void hardDelete_whenS3FaceFails_continuesAndAccumulatesFailure() {
        UUID empId = UUID.randomUUID();
        var emp = employeeEntity(empId, "face-key");
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId)).thenReturn(List.of());
        doThrow(new RuntimeException("S3 error")).when(faceStorageProvider)
                .deleteFaceImage("face-key");

        var result = service.hardDelete(companyId, cnpj, "Empresa Teste");

        verify(companyRepository).deleteById(companyId);
        assertThat(result.externalCleanupFailures()).isEqualTo(1);
    }

    @Test
    void hardDelete_externalOrderIsBeforeDb() {
        UUID empId = UUID.randomUUID();
        var emp = employeeEntity(empId, "face-key");
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(documentRepository.findByEmployeeIdOrderByUploadedAtDesc(empId)).thenReturn(List.of());

        var callOrder = inOrder(faceRecognitionProvider, faceStorageProvider,
                documentRepository, anonymizationRepository, lgpdRequestRepository,
                legalConsentRepository, userCompanyAccessRepository,
                employeeRepository, companyRepository);

        service.hardDelete(companyId, cnpj, "Empresa Teste");

        callOrder.verify(faceRecognitionProvider).deleteFacesByExternalImageId(empId);
        callOrder.verify(faceStorageProvider).deleteFaceImage("face-key");
        callOrder.verify(documentRepository).findByEmployeeIdOrderByUploadedAtDesc(empId);
        callOrder.verify(anonymizationRepository).deleteByEmployeeId(empId);
        callOrder.verify(lgpdRequestRepository).deleteByEmployeeId(empId);
        callOrder.verify(legalConsentRepository).deleteByEmployeeId(empId);
        callOrder.verify(userCompanyAccessRepository).deleteByCompanyId(companyId);
        callOrder.verify(employeeRepository).deleteAll(List.of(emp));
        callOrder.verify(companyRepository).deleteById(companyId);
    }

    private EmployeeEntity employeeEntity(UUID employeeId, String faceKey) {
        EmployeeEntity e = new EmployeeEntity();
        e.setEmployeeId(employeeId);
        e.setFaceS3ObjectKey(faceKey);
        return e;
    }

    private DocumentEntity documentEntity(String storagePath, DocumentType type) {
        DocumentEntity d = new DocumentEntity();
        d.setDocumentId(UUID.randomUUID());
        d.setStoragePath(storagePath);
        d.setType(type);
        return d;
    }
}
