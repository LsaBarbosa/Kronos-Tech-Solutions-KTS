package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.*;
import com.kts.kronos.application.service.demo.DemoSandboxValidationService;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoSandboxValidationServiceTest {

    @TempDir Path tempDir;

    @Mock CompanyRepository   companyRepo;
    @Mock UserRepository      userRepo;
    @Mock EmployeeRepository  employeeRepo;
    @Mock DocumentRepository  documentRepo;
    @Mock TimeRecordRepository timeRecordRepo;
    @Mock UserCompanyAccessRepository accessRepo;
    @Mock LegalConsentRepository      consentRepo;
    @Mock DemoSandboxProperties       props;

    @InjectMocks
    DemoSandboxValidationService service;

    @BeforeEach
    void setup() {
        when(props.getSandboxKey()).thenReturn("KRONOS_TESTE");
        when(props.getUsername()).thenReturn("kronos_teste");
        when(props.getLocalStorageRoot()).thenReturn(tempDir.resolve("sandbox").toString());
    }

    // ─────────────────────────── validateAfterPurge ───────────────────────────

    @Test
    void validateAfterPurge_shouldBeCleanWhenNothingRemains() {
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.validateAfterPurge();

        assertThat(result.clean()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void validateAfterPurge_shouldDetectCompanyResidue() {
        CompanyEntity company = CompanyEntity.builder()
                .id(UUID.randomUUID()).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(company.getId())).thenReturn(List.of());
        when(accessRepo.findByCompanyId(company.getId())).thenReturn(List.of());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.validateAfterPurge();

        assertThat(result.clean()).isFalse();
        assertThat(result.issues()).anyMatch(i -> i.type().equals("COMPANY_RESIDUE"));
    }

    @Test
    void validateAfterPurge_shouldDetectEmployeeResidue() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(accessRepo.findByCompanyId(companyId)).thenReturn(List.of());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.validateAfterPurge();

        assertThat(result.clean()).isFalse();
        assertThat(result.issues()).anyMatch(i -> i.type().equals("EMPLOYEE_RESIDUE"));
    }

    @Test
    void validateAfterPurge_shouldDetectDocumentResidue() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        DocumentEntity doc      = DocumentEntity.builder().documentId(UUID.randomUUID()).employeeId(employeeId).build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of(doc));
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(accessRepo.findByCompanyId(companyId)).thenReturn(List.of());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.validateAfterPurge();

        assertThat(result.issues()).anyMatch(i -> i.type().equals("DOCUMENT_RESIDUE"));
    }

    @Test
    void validateAfterPurge_shouldDetectTimeRecordResidue() {
        UUID companyId  = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        CompanyEntity company   = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId).companyId(companyId).build();
        TimeRecordEntity record = TimeRecordEntity.builder().employeeId(employeeId).build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of(record));
        when(accessRepo.findByCompanyId(companyId)).thenReturn(List.of());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.validateAfterPurge();

        assertThat(result.issues()).anyMatch(i -> i.type().equals("TIME_RECORD_RESIDUE"));
    }

    @Test
    void validateAfterPurge_shouldDetectAccessResidue() {
        UUID companyId = UUID.randomUUID();
        CompanyEntity company  = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();
        UserCompanyAccessEntity access = UserCompanyAccessEntity.builder()
                .accessId(UUID.randomUUID()).companyId(companyId).build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of());
        when(accessRepo.findByCompanyId(companyId)).thenReturn(List.of(access));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.validateAfterPurge();

        assertThat(result.issues()).anyMatch(i -> i.type().equals("ACCESS_RESIDUE"));
    }

    @Test
    void validateAfterPurge_shouldDetectUserResidue() {
        UserEntity user = UserEntity.builder().userId(UUID.randomUUID()).username("kronos_teste").build();
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));

        var result = service.validateAfterPurge();

        assertThat(result.clean()).isFalse();
        assertThat(result.issues()).anyMatch(i -> i.type().equals("USER_RESIDUE"));
    }

    @Test
    void validateAfterPurge_shouldDetectFilesResidue() throws IOException {
        Path sandboxDir = tempDir.resolve("sandbox");
        Files.createDirectories(sandboxDir);
        when(props.getLocalStorageRoot()).thenReturn(sandboxDir.toString());

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.validateAfterPurge();

        assertThat(result.clean()).isFalse();
        assertThat(result.issues()).anyMatch(i -> i.type().equals("FILES_RESIDUE"));
    }

    @Test
    void validateAfterPurge_issuesShouldNotContainSensitiveIds() {
        UUID companyId = UUID.randomUUID();
        CompanyEntity company = CompanyEntity.builder().id(companyId).sandbox(true).sandboxKey("KRONOS_TESTE").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of());
        when(accessRepo.findByCompanyId(companyId)).thenReturn(List.of());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        var result = service.validateAfterPurge();

        // Issue messages must not expose server paths or UUIDs
        result.issues().forEach(issue -> {
            assertThat(issue.description()).doesNotContain("/opt/");
            assertThat(issue.description()).doesNotContain(companyId.toString());
        });
    }

    // ─────────────────────────── sandboxExists ────────────────────────────────

    @Test
    void sandboxExists_shouldReturnTrueWhenCompanyFound() {
        CompanyEntity company = CompanyEntity.builder().id(UUID.randomUUID()).build();
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));

        assertThat(service.sandboxExists()).isTrue();
    }

    @Test
    void sandboxExists_shouldReturnFalseWhenCompanyNotFound() {
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());

        assertThat(service.sandboxExists()).isFalse();
    }
}
