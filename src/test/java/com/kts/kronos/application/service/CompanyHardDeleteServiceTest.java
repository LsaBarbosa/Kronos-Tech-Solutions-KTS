package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
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

        var result = service.hardDelete(companyId, cnpj, "Empresa Teste");

        // External: Rekognition + S3 for each employee
        verify(faceRecognitionProvider).deleteFacesByExternalImageId(empId1);
        verify(faceRecognitionProvider).deleteFacesByExternalImageId(empId2);
        verify(faceStorageProvider).deleteFaceImage("face-key-1");
        verify(faceStorageProvider).deleteFaceImage("face-key-2");

        // DB: RESTRICT cleanup per employee
        verify(anonymizationRepository).deleteByEmployeeId(empId1);
        verify(anonymizationRepository).deleteByEmployeeId(empId2);
        verify(lgpdRequestRepository).deleteByEmployeeId(empId1);
        verify(lgpdRequestRepository).deleteByEmployeeId(empId2);
        verify(legalConsentRepository).deleteByEmployeeId(empId1);
        verify(legalConsentRepository).deleteByEmployeeId(empId2);

        // DB: bulk cleanup
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

        service.hardDelete(companyId, cnpj, "Empresa Sem Face");

        verify(faceRecognitionProvider).deleteFacesByExternalImageId(empId);
        verifyNoInteractions(faceStorageProvider);
    }

    @Test
    void hardDelete_whenRekognitionFails_continuesAndAccumulatesFailure() {
        UUID empId = UUID.randomUUID();
        var emp = employeeEntity(empId, "face-key");
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
        doThrow(new RuntimeException("AWS error")).when(faceRecognitionProvider)
                .deleteFacesByExternalImageId(empId);

        var result = service.hardDelete(companyId, cnpj, "Empresa Teste");

        // Should still delete from S3 and DB
        verify(faceStorageProvider).deleteFaceImage("face-key");
        verify(companyRepository).deleteById(companyId);
        assertThat(result.externalCleanupFailures()).isEqualTo(1);
        assertThat(result.externalFailureDetails()).hasSize(1);
    }

    @Test
    void hardDelete_whenS3Fails_continuesAndAccumulatesFailure() {
        UUID empId = UUID.randomUUID();
        var emp = employeeEntity(empId, "face-key");
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(emp));
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

        var callOrder = inOrder(faceRecognitionProvider, faceStorageProvider,
                anonymizationRepository, lgpdRequestRepository, legalConsentRepository,
                userCompanyAccessRepository, employeeRepository, companyRepository);

        service.hardDelete(companyId, cnpj, "Empresa Teste");

        // External before DB
        callOrder.verify(faceRecognitionProvider).deleteFacesByExternalImageId(empId);
        callOrder.verify(faceStorageProvider).deleteFaceImage("face-key");
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
}
