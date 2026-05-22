package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAnonymizationServiceTest {

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private UserProvider userProvider;
    @Mock
    private FaceStorageProvider faceStorageProvider;
    @Mock
    private FaceRecognitionProvider faceRecognitionProvider;
    @Mock
    private LegalConsentProvider legalConsentProvider;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private EmployeeAnonymizationService service;

    @Test
    void shouldAnonymizeEmployeeAndDeactivateLinkedUserForManager() {
        UUID employeeId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Employee employee = employee(employeeId);
        User user = new User(UUID.randomUUID(), "lucas", "hash", Role.MANAGER, true, employeeId);
        LegalConsent consent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                user.userId(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "2026.05.21",
                Instant.parse("2026-05-21T10:00:00Z"),
                null,
                "127.0.0.1",
                "JUnit",
                UUID.randomUUID(),
                "hash",
                Instant.parse("2026-05-21T10:00:00Z"),
                null
        );

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(Optional.of(consent));
        when(legalConsentProvider.save(any(LegalConsent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.anonymize(employeeId, "127.0.0.1", "JUnit", actorUserId);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeProvider).save(employeeCaptor.capture());
        Employee anonymized = employeeCaptor.getValue();
        assertEquals("ANONYMIZED-" + employeeId, anonymized.fullName());
        assertTrue(anonymized.cpf().startsWith("ANON"));
        assertEquals(14, anonymized.cpf().length());
        assertEquals("anon-" + employeeId + "@deleted.local", anonymized.email());
        assertNull(anonymized.pis());
        assertNull(anonymized.phone());
        assertNull(anonymized.address());
        assertNull(anonymized.faceS3ObjectKey());
        assertFalse(anonymized.active());
        assertEquals("LGPD_ANONYMIZATION", anonymized.deactivationReason());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().active());
        assertEquals("LGPD_ANONYMIZATION", userCaptor.getValue().deactivationReason());

        verify(faceStorageProvider).deleteFaceImage("faces/object.jpg");
        verify(faceRecognitionProvider).deleteFacesByExternalImageId(employeeId);
        verify(legalConsentProvider).save(any(LegalConsent.class));

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).registerLgpd(
                eq(AuditAction.LGPD_DATA_ANONYMIZED),
                eq(employeeId),
                eq(anonymized.companyId()),
                eq("EMPLOYEE"),
                eq(employeeId.toString()),
                eq("HIGH"),
                detailsCaptor.capture(),
                eq("127.0.0.1"),
                eq("JUnit")
        );
        assertTrue(detailsCaptor.getValue().contains(employeeId.toString()));
    }

    @Test
    void shouldRejectPartnerEvenWhenTargetingSelf() {
        UUID employeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        assertThrows(ForbiddenException.class, () -> service.anonymize(employeeId, "127.0.0.1", "JUnit", UUID.randomUUID()));

        verify(domainAuthorizationService, never()).authorizeEmployeeAccess(any());
        verify(employeeProvider, never()).save(any());
        verify(userProvider, never()).save(any());
        verify(auditService, never()).registerLgpd(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldAllowCtoToAnonymizeEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Employee employee = employee(employeeId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(Optional.empty());

        service.anonymize(employeeId, "127.0.0.1", "JUnit", actorUserId);

        verify(employeeProvider).save(any(Employee.class));
        verify(auditService).registerLgpd(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectManagerFromOtherCompanyTarget() {
        UUID employeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenThrow(new ForbiddenException("forbidden"));

        assertThrows(ForbiddenException.class, () -> service.anonymize(employeeId, "127.0.0.1", "JUnit", UUID.randomUUID()));

        verify(employeeProvider, never()).save(any());
        verify(userProvider, never()).save(any());
        verify(auditService, never()).registerLgpd(any(), any(), any(), any(), any(), any(), any(), any(), any());
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
