package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.*;
import com.kts.kronos.application.service.demo.*;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoSandboxPurgeServiceTest {

    @Mock CompanyRepository companyRepo;
    @Mock UserRepository userRepo;
    @Mock EmployeeRepository employeeRepo;
    @Mock UserCompanyAccessRepository accessRepo;
    @Mock TimeRecordRepository timeRecordRepo;
    @Mock TimeRecordApprovalRepository approvalRepo;
    @Mock DocumentRepository documentRepo;
    @Mock LegalConsentRepository consentRepo;
    @Mock DemoSandboxSessionInvalidationService sessionInvalidation;
    @Mock DemoSandboxProperties props;

    @InjectMocks
    DemoSandboxPurgeService service;

    @BeforeEach
    void setup() {
        when(props.getSandboxKey()).thenReturn("KRONOS_TESTE");
        when(props.getUsername()).thenReturn("kronos_teste");
        when(props.getLocalStorageRoot()).thenReturn("/tmp/kronos-sandbox-test-" + UUID.randomUUID());
    }

    @Test
    void shouldBeIdempotentWhenSandboxAlreadyEmpty() {
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());
        when(sessionInvalidation.invalidateSandboxUserSession("kronos_teste")).thenReturn(0);

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

        CompanyEntity company = CompanyEntity.builder().id(companyId).sandbox(true)
                .sandboxKey("KRONOS_TESTE").build();
        EmployeeEntity employee = EmployeeEntity.builder().employeeId(employeeId)
                .companyId(companyId).build();
        UserEntity user = UserEntity.builder().userId(userId).username("kronos_teste").build();

        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.of(company));
        when(employeeRepo.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userRepo.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId)).thenReturn(List.of());
        when(timeRecordRepo.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(consentRepo.deleteByEmployeeId(employeeId)).thenReturn(0);
        when(accessRepo.deleteByCompanyId(companyId)).thenReturn(1);
        when(sessionInvalidation.invalidateSandboxUserSession("kronos_teste")).thenReturn(1);

        var result = service.purgeAll();

        assertThat(result.companies()).isEqualTo(1);
        assertThat(result.users()).isEqualTo(1);
        assertThat(result.employees()).isEqualTo(1);

        verify(companyRepo).delete(company);
        verify(userRepo).delete(user);
        verify(employeeRepo).deleteAll(List.of(employee));
    }

    @Test
    void shouldNotDeleteRealCompany() {
        when(companyRepo.findBySandboxKey("KRONOS_TESTE")).thenReturn(Optional.empty());
        when(userRepo.findByUsernameIgnoreCase(any())).thenReturn(Optional.empty());
        when(sessionInvalidation.invalidateSandboxUserSession(any())).thenReturn(0);

        service.purgeAll();

        verify(companyRepo, never()).deleteAll(any());
        verify(companyRepo, never()).deleteById(any());
        verify(companyRepo, never()).delete(argThat(c -> !c.isSandbox()));
    }
}
