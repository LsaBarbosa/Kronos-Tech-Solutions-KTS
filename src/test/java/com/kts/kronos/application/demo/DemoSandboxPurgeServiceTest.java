package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.*;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.service.demo.*;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoSandboxPurgeServiceTest {

    @TempDir Path tempDir;

    @Mock CompanyRepository companyRepo;
    @Mock UserRepository userRepo;
    @Mock EmployeeRepository employeeRepo;
    @Mock UserCompanyAccessRepository accessRepo;
    @Mock TimeRecordRepository timeRecordRepo;
    @Mock TimeRecordApprovalRepository approvalRepo;
    @Mock DocumentRepository documentRepo;
    @Mock LegalConsentRepository consentRepo;
    @Mock FaceRecognitionProvider faceRecognitionProvider;
    @Mock FaceStorageProvider faceStorageProvider;
    @Mock BucketStorageProvider bucketStorageProvider;
    @Mock DemoSandboxProperties props;

    @InjectMocks
    DemoSandboxPurgeService service;

    @BeforeEach
    void setup() {
        when(props.getSandboxKey()).thenReturn("KRONOS_TESTE");
        when(props.getUsername()).thenReturn("kronos_teste");
        when(props.getLocalStorageRoot()).thenReturn(tempDir.toString());
    }

    @Test
    void shouldBeIdempotentWhenSandboxAlreadyEmpty() {
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.purgeAll();

        assertThat(result.companies()).isZero();
        assertThat(result.users()).isZero();
        assertThat(result.employees()).isZero();
    }

    @Test
    void shouldDeleteSandboxDataWhenExists() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId     = UUID.randomUUID();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(userId).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(1);

        var result = service.purgeAll();

        assertThat(result.companies()).isEqualTo(1);
        assertThat(result.users()).isEqualTo(1);
        assertThat(result.employees()).isEqualTo(1);

        // User deleted BEFORE employee (FK cascade safety)
        var inOrder = inOrder(userRepo, employeeRepo);
        inOrder.verify(userRepo).delete(user);
        inOrder.verify(employeeRepo).deleteAll(List.of(employee));
        verify(companyRepo).delete(company);
    }

    @Test
    void shouldDeleteOrphanUserWhenCompanyAlreadyGone() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().userId(userId).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));

        var result = service.purgeAll();

        assertThat(result.users()).isEqualTo(1);
        assertThat(result.companies()).isZero();
        verify(userRepo).delete(user);
    }

    @Test
    void shouldNotDeleteRealCompany() {
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase(any())).thenReturn(Optional.empty());

        service.purgeAll();

        verify(companyRepo, never()).deleteAll(any());
        verify(companyRepo, never()).deleteById(any());
        verify(companyRepo, never()).delete(argThat(c -> !c.isSandbox()));
    }

    @Test
    void shouldSkipS3DeletionForLocalSandboxDocument() throws IOException {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        // Create a real local file under tempDir (simulating a sandbox-seeded doc)
        String relativePath = "company/" + companyId + "/documents/payslip/doc1.pdf";
        Path localFile = tempDir.resolve(relativePath);
        Files.createDirectories(localFile.getParent());
        Files.writeString(localFile, "synthetic");

        DocumentEntity localDoc = DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(employeeId)
                .storagePath(relativePath)
                .type(DocumentType.PAYSLIP)
                .build();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(localDoc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        // Local file exists under sandbox root → S3 provider must NOT be called
        verify(bucketStorageProvider, never()).deleteFile(any(), any());
    }

    @Test
    void shouldCallS3DeletionForRemoteDocument() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        // S3 key (relative, but not present in local filesystem)
        String s3Key = "company/" + companyId + "/employee/" + employeeId + "/PAYSLIP/2024/01/uuid-slip.pdf";

        DocumentEntity s3Doc = DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(employeeId)
                .storagePath(s3Key)
                .type(DocumentType.PAYSLIP)
                .build();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(s3Doc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        // File not found locally → must call S3 provider
        verify(bucketStorageProvider).deleteFile(DocumentType.PAYSLIP, s3Key);
    }

    @Test
    void shouldSkipS3DeletionForLegacyAbsolutePath() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        // Legacy absolute path (pre-fix format)
        String absolutePath = "/opt/kronos/sandbox/kronos-teste/company/" + companyId + "/documents/payslip/doc.pdf";

        DocumentEntity legacyDoc = DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(employeeId)
                .storagePath(absolutePath)
                .type(DocumentType.PAYSLIP)
                .build();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(legacyDoc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        // Absolute path → skipped (filesystem walker handles legacy files)
        verify(bucketStorageProvider, never()).deleteFile(any(), any());
    }

    @Test
    void shouldContinuePurgeWhenS3DeletionFails() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        String s3Key = "company/remote/doc.pdf";

        DocumentEntity s3Doc = DocumentEntity.builder()
                .documentId(UUID.randomUUID()).employeeId(employeeId)
                .storagePath(s3Key).type(DocumentType.PAYSLIP).build();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(s3Doc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);
        doThrow(new RuntimeException("S3 unavailable")).when(bucketStorageProvider).deleteFile(any(), any());

        // S3 failure must not abort the purge
        assertThatCode(() -> service.purgeAll()).doesNotThrowAnyException();
        verify(documentRepo).deleteAll(List.of(s3Doc));
    }
}
