package com.kts.kronos.application.service;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.anonymization.AnonymizationPlanExecutor;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAnonymizationServiceTest {


    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;
    @Mock
    private AuditService auditService;
    @Mock
    private AnonymizationPlanExecutor anonymizationPlanExecutor;

    @InjectMocks
    private EmployeeAnonymizationService service;

    @Test
    void shouldAnonymizeEmployeeAndDeactivateLinkedUserForManager() {
        UUID employeeId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Employee employee = employee(employeeId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);

        service.anonymize(employeeId, "127.0.0.1", "JUnit", actorUserId);

        ArgumentCaptor<AnonymizationPlan> planCaptor = ArgumentCaptor.forClass(AnonymizationPlan.class);
        verify(anonymizationPlanExecutor).executePlan(planCaptor.capture(), eq("APPLY"));

        AnonymizationPlan plan = planCaptor.getValue();
        assert plan.employeeId().equals(employeeId);
        assert plan.companyId().equals(employee.companyId());
        assert plan.requestedByUserId().equals(actorUserId);

        verify(auditService).registerLgpd(any(AuditAction.class), any(UUID.class), any(UUID.class), any(UUID.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void shouldRejectPartnerEvenWhenTargetingSelf() {
        UUID employeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        assertThrows(ForbiddenException.class, () -> service.anonymize(employeeId, "127.0.0.1", "JUnit", UUID.randomUUID()));

        verify(domainAuthorizationService, never()).authorizeEmployeeAccess(any());
        verify(anonymizationPlanExecutor, never()).executePlan(any(), any());
        verify(auditService, never()).registerLgpd(any(AuditAction.class), any(UUID.class), any(UUID.class), any(UUID.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void shouldAllowCtoToAnonymizeEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Employee employee = employee(employeeId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);

        service.anonymize(employeeId, "127.0.0.1", "JUnit", actorUserId);

        verify(anonymizationPlanExecutor).executePlan(any(AnonymizationPlan.class), eq("APPLY"));
        verify(auditService).registerLgpd(any(AuditAction.class), any(UUID.class), any(UUID.class), any(UUID.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void shouldRejectManagerFromOtherCompanyTarget() {
        UUID employeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenThrow(new ForbiddenException("forbidden"));

        assertThrows(ForbiddenException.class, () -> service.anonymize(employeeId, "127.0.0.1", "JUnit", UUID.randomUUID()));

        verify(anonymizationPlanExecutor, never()).executePlan(any(), any());
        verify(auditService, never()).registerLgpd(any(AuditAction.class), any(UUID.class), any(UUID.class), any(UUID.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void shouldCatchAndRethrowWhenExecutorFails() {
        UUID employeeId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Employee employee = employee(employeeId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        doThrow(new RuntimeException("executor-error"))
                .when(anonymizationPlanExecutor).executePlan(any(), anyString());

        var ex = assertThrows(RuntimeException.class,
                () -> service.anonymize(employeeId, "127.0.0.1", "JUnit", actorUserId));
        assertTrue(ex.getMessage().contains("Falha ao anonimizar"));
    }

    private Employee employee(UUID employeeId) {
        return new Employee(
                employeeId,
                "Lucas",
                "12345678901",
                "98765432100",
                "Dev",
                "lucas@kts.com",
                5000.0,
                "11999999999",
                true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                UUID.randomUUID(),
                LocalDateTime.now(),
                true,
                "faces/object.jpg",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                Set.of()
        );
    }
}
