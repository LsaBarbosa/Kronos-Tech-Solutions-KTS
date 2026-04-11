package com.kts.kronos.application.security;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainAuthorizationServiceTest {

    @InjectMocks
    private DomainAuthorizationService service;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private UserProvider userProvider;
    @Mock
    private DocumentProvider documentProvider;

    private UUID loggedEmployeeId;
    private UUID loggedUserId;
    private UUID companyAId;
    private UUID companyBId;
    private Employee authenticatedEmployee;
    private Employee sameTenantEmployee;
    private Employee otherTenantEmployee;

    @BeforeEach
    void setUp() {
        loggedEmployeeId = UUID.randomUUID();
        loggedUserId = UUID.randomUUID();
        companyAId = UUID.randomUUID();
        companyBId = UUID.randomUUID();
        authenticatedEmployee = buildEmployee(loggedEmployeeId, companyAId);
        sameTenantEmployee = buildEmployee(UUID.randomUUID(), companyAId);
        otherTenantEmployee = buildEmployee(UUID.randomUUID(), companyBId);
    }

    @Test
    @DisplayName("employeeId: partner acessa próprio colaborador")
    void shouldAllowPartnerOwnEmployeeAccess() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));

        var result = service.authorizeEmployeeAccess(null);

        assertEquals(loggedEmployeeId, result.employeeId());
    }

    @Test
    @DisplayName("employeeId: manager acessa colaborador do mesmo tenant")
    void shouldAllowManagerAccessSameTenantEmployee() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(sameTenantEmployee.employeeId())).thenReturn(Optional.of(sameTenantEmployee));

        var result = service.authorizeEmployeeAccess(sameTenantEmployee.employeeId());

        assertEquals(sameTenantEmployee.employeeId(), result.employeeId());
    }

    @Test
    @DisplayName("employeeId: manager não acessa colaborador de outro tenant")
    void shouldDenyManagerAccessOtherTenantEmployee() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(otherTenantEmployee.employeeId())).thenReturn(Optional.of(otherTenantEmployee));

        assertThrows(ForbiddenException.class, () -> service.authorizeEmployeeAccess(otherTenantEmployee.employeeId()));
    }

    @Test
    @DisplayName("userId: partner acessa apenas próprio usuário")
    void shouldAllowPartnerOwnUserAccess() {
        var ownUser = buildUser(loggedUserId, loggedEmployeeId);
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.getuserId()).thenReturn(loggedUserId);
        when(userProvider.findById(loggedUserId)).thenReturn(Optional.of(ownUser));

        var result = service.authorizeUserAccess(loggedUserId);

        assertEquals(loggedUserId, result.userId());
    }

    @Test
    @DisplayName("userId: partner não acessa usuário de terceiro")
    void shouldDenyPartnerAccessOtherUser() {
        var targetUser = buildUser(UUID.randomUUID(), sameTenantEmployee.employeeId());
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.getuserId()).thenReturn(loggedUserId);
        when(userProvider.findById(targetUser.userId())).thenReturn(Optional.of(targetUser));

        assertThrows(ForbiddenException.class, () -> service.authorizeUserAccess(targetUser.userId()));
    }

    @Test
    @DisplayName("userId: manager acessa usuário do mesmo tenant")
    void shouldAllowManagerAccessSameTenantUser() {
        var targetUser = buildUser(UUID.randomUUID(), sameTenantEmployee.employeeId());
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(userProvider.findById(targetUser.userId())).thenReturn(Optional.of(targetUser));
        when(employeeProvider.findById(sameTenantEmployee.employeeId())).thenReturn(Optional.of(sameTenantEmployee));

        var result = service.authorizeUserAccess(targetUser.userId());

        assertEquals(targetUser.userId(), result.userId());
    }

    @Test
    @DisplayName("userId: manager não acessa usuário de outro tenant")
    void shouldDenyManagerAccessOtherTenantUser() {
        var targetUser = buildUser(UUID.randomUUID(), otherTenantEmployee.employeeId());
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(userProvider.findById(targetUser.userId())).thenReturn(Optional.of(targetUser));
        when(employeeProvider.findById(otherTenantEmployee.employeeId())).thenReturn(Optional.of(otherTenantEmployee));

        assertThrows(ForbiddenException.class, () -> service.authorizeUserAccess(targetUser.userId()));
    }

    @Test
    @DisplayName("documentId: manager acessa documento de colaborador do mesmo tenant")
    void shouldAllowManagerAccessSameTenantDocument() {
        var documentId = UUID.randomUUID();
        var document = buildDocument(documentId, sameTenantEmployee.employeeId());
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(sameTenantEmployee.employeeId())).thenReturn(Optional.of(sameTenantEmployee));
        when(documentProvider.findByIdAndEmployeeId(documentId, sameTenantEmployee.employeeId()))
                .thenReturn(Optional.of(document));

        var result = service.authorizeDocumentAccess(documentId, sameTenantEmployee.employeeId());

        assertEquals(documentId, result.documentId());
    }

    @Test
    @DisplayName("documentId: manager não acessa documento cross-tenant")
    void shouldDenyManagerAccessCrossTenantDocument() {
        var documentId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(otherTenantEmployee.employeeId())).thenReturn(Optional.of(otherTenantEmployee));

        assertThrows(ForbiddenException.class,
                () -> service.authorizeDocumentAccess(documentId, otherTenantEmployee.employeeId()));
        verify(documentProvider, never()).findByIdAndEmployeeId(documentId, otherTenantEmployee.employeeId());
    }

    @Test
    @DisplayName("companyId: manager acessa apenas própria empresa")
    void shouldAllowManagerOwnCompanyAccess() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));

        var result = service.authorizeCompanyAccess(companyAId);

        assertEquals(companyAId, result);
    }

    @Test
    @DisplayName("companyId: manager não acessa empresa de outro tenant")
    void shouldDenyManagerOtherCompanyAccess() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));

        assertThrows(ForbiddenException.class, () -> service.authorizeCompanyAccess(companyBId));
    }

    @Test
    @DisplayName("companyId: cto pode acessar qualquer empresa")
    void shouldAllowCtoAccessAnyCompany() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));

        var result = service.authorizeCompanyAccess(companyBId);

        assertEquals(companyBId, result);
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Nome",
                "12345678901",
                "12345678901",
                "Dev",
                "dev@kts.com",
                1000.0,
                "11999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );
    }

    private User buildUser(UUID userId, UUID employeeId) {
        return new User(userId, "user", "password", Role.PARTNER, true, employeeId);
    }

    private Document buildDocument(UUID documentId, UUID employeeId) {
        return new Document(
                documentId,
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "documents/safe.pdf",
                LocalDateTime.now(),
                null,
                false,
                false
        );
    }
}
