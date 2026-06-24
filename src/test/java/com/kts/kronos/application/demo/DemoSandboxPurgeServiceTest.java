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

import static org.junit.jupiter.api.Assumptions.assumeFalse;

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

    // ─── purgeSandboxArtifacts branches ──────────────────────────────────────

    @Test
    void shouldSkipFaceStorageWhenFaceKeyIsNull() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .employeeId(employeeId).companyId(companyId)
                .faceS3ObjectKey(null).build();
        CompanyEntity company = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        UserEntity user = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        verify(faceStorageProvider, never()).deleteFaceImage(any());
    }

    @Test
    void shouldSkipFaceStorageWhenFaceKeyIsBlank() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .employeeId(employeeId).companyId(companyId)
                .faceS3ObjectKey("   ").build();
        CompanyEntity company = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        UserEntity user = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        verify(faceStorageProvider, never()).deleteFaceImage(any());
    }

    @Test
    void shouldContinuePurgeWhenFaceRecognitionProviderThrows() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .employeeId(employeeId).companyId(companyId)
                .faceS3ObjectKey(null).build();
        CompanyEntity company = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        UserEntity user = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);
        doThrow(new RuntimeException("Rekognition unavailable"))
                .when(faceRecognitionProvider).deleteFacesByExternalImageId(any());

        assertThatCode(() -> service.purgeAll()).doesNotThrowAnyException();
        verify(employeeRepo).deleteAll(List.of(employee));
    }

    @Test
    void shouldCallFaceStorageWhenFaceKeyPresent() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .employeeId(employeeId).companyId(companyId)
                .faceS3ObjectKey("faces/employee.jpg").build();
        CompanyEntity company = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        UserEntity user = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        verify(faceStorageProvider).deleteFaceImage("faces/employee.jpg");
    }

    @Test
    void shouldContinuePurgeWhenFaceStorageProviderThrows() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        EmployeeEntity employee = EmployeeEntity.builder()
                .employeeId(employeeId).companyId(companyId)
                .faceS3ObjectKey("faces/employee.jpg").build();
        CompanyEntity company = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        UserEntity user = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);
        doThrow(new RuntimeException("S3 face storage unavailable"))
                .when(faceStorageProvider).deleteFaceImage(any());

        assertThatCode(() -> service.purgeAll()).doesNotThrowAnyException();
        verify(employeeRepo).deleteAll(List.of(employee));
    }

    // ─── deleteApprovalsForEmployee branch ───────────────────────────────────

    @Test
    void shouldDeleteApprovalWhenExistsById() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Long timeRecordId = 42L;

        TimeRecordEntity record = TimeRecordEntity.builder()
                .timeRecordId(timeRecordId).employeeId(employeeId).build();
        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of(record));
        when(approvalRepo.existsById(timeRecordId)).thenReturn(true);
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        var result = service.purgeAll();

        assertThat(result.approvals()).isEqualTo(1);
        verify(approvalRepo).deleteById(timeRecordId);
    }

    // ─── line 73: company present, user absent ───────────────────────────────

    @Test
    void shouldSkipUserDeletionWhenUserNotFoundButCompanyExists() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        var result = service.purgeAll();

        assertThat(result.users()).isZero();
        verify(userRepo, never()).delete(any());
    }

    // ─── line 108: null/blank storagePath → skip S3 ──────────────────────────

    @Test
    void shouldSkipNullStoragePath() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        DocumentEntity nullPathDoc = DocumentEntity.builder()
                .documentId(UUID.randomUUID()).employeeId(employeeId)
                .storagePath(null).type(DocumentType.PAYSLIP).build();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(nullPathDoc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        verify(bucketStorageProvider, never()).deleteFile(any(), any());
    }

    @Test
    void shouldSkipBlankStoragePath() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        DocumentEntity blankPathDoc = DocumentEntity.builder()
                .documentId(UUID.randomUUID()).employeeId(employeeId)
                .storagePath("   ").type(DocumentType.PAYSLIP).build();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(blankPathDoc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        verify(bucketStorageProvider, never()).deleteFile(any(), any());
    }

    // ─── line 113: path traversal outside sandbox root → S3 ─────────────────

    @Test
    void shouldCallS3WhenPathResolvesOutsideSandboxRoot() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        // Path traversal resolves outside tempDir sandbox root → startsWith(sandboxRoot) = false
        String traversalPath = "../../../etc/traversal.pdf";

        DocumentEntity traversalDoc = DocumentEntity.builder()
                .documentId(UUID.randomUUID()).employeeId(employeeId)
                .storagePath(traversalPath).type(DocumentType.PAYSLIP).build();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(traversalDoc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        service.purgeAll();

        // Path resolves outside sandbox root → treated as S3 key
        verify(bucketStorageProvider).deleteFile(DocumentType.PAYSLIP, traversalPath);
    }

    // ─── line 146: approval does NOT exist for time record ───────────────────

    @Test
    void shouldSkipApprovalDeletionWhenApprovalDoesNotExist() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Long timeRecordId = 99L;

        TimeRecordEntity record = TimeRecordEntity.builder()
                .timeRecordId(timeRecordId).employeeId(employeeId).build();
        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of(record));
        when(approvalRepo.existsById(timeRecordId)).thenReturn(false);
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        var result = service.purgeAll();

        assertThat(result.approvals()).isZero();
        verify(approvalRepo, never()).deleteById(any());
    }

    // ─── deleteDocumentsForEmployee malformed-path branch ────────────────────

    @Test
    void shouldFallThroughToS3WhenPathIsMalformed() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        // Null character causes InvalidPathException in Path.resolve()
        String malformedPath = "company bad";

        DocumentEntity malformedDoc = DocumentEntity.builder()
                .documentId(UUID.randomUUID()).employeeId(employeeId)
                .storagePath(malformedPath).type(DocumentType.PAYSLIP).build();

        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        UserEntity user         = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(malformedDoc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(0);

        // Should not throw; falls through to S3 attempt after InvalidPathException
        assertThatCode(() -> service.purgeAll()).doesNotThrowAnyException();
        verify(bucketStorageProvider).deleteFile(DocumentType.PAYSLIP, malformedPath);
    }

    // ─── deleteSandboxFiles branches ─────────────────────────────────────────

    @Test
    void shouldReturnZeroFilesWhenSandboxDirDoesNotExist() {
        when(props.getLocalStorageRoot()).thenReturn(tempDir.resolve("nonexistent").toString());
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.purgeAll();

        assertThat(result.files()).isZero();
    }

    @Test
    void shouldHandleIOExceptionDuringFileDeletion() throws IOException {
        assumeFalse("root".equals(System.getProperty("user.name")),
                "Skipped when running as root: file permission restrictions don't apply");

        Path sandboxDir = tempDir.resolve("iotest");
        Path subDir = sandboxDir.resolve("sub");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("locked.txt"), "content");

        when(props.getLocalStorageRoot()).thenReturn(sandboxDir.toString());
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        // Make subDir non-writable → Files.delete(locked.txt) throws AccessDeniedException
        subDir.toFile().setWritable(false);
        try {
            assertThatCode(() -> service.purgeAll()).doesNotThrowAnyException();
        } finally {
            subDir.toFile().setWritable(true);
        }
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
