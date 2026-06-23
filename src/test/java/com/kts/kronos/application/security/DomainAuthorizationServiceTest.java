package com.kts.kronos.application.security;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
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
    @DisplayName("employeeId: CTO acessa qualquer colaborador")
    void shouldAllowCtoAccessAnyEmployee() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(otherTenantEmployee.employeeId())).thenReturn(Optional.of(otherTenantEmployee));

        var result = service.authorizeEmployeeAccess(otherTenantEmployee.employeeId());

        assertEquals(otherTenantEmployee.employeeId(), result.employeeId());
    }

    @Test
    @DisplayName("employeeId: partner não acessa colaborador de terceiro")
    void shouldDenyPartnerAccessOtherEmployee() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(sameTenantEmployee.employeeId())).thenReturn(Optional.of(sameTenantEmployee));

        assertThrows(ForbiddenException.class, () -> service.authorizeEmployeeAccess(sameTenantEmployee.employeeId()));
    }

    @Test
    @DisplayName("employeeId: falha quando colaborador alvo não existe")
    void shouldFailWhenTargetEmployeeDoesNotExist() {
        UUID targetEmployeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.authorizeEmployeeAccess(targetEmployeeId));
    }

    @Test
    @DisplayName("employeeId: partner acessa próprio colaborador")
    void shouldAllowPartnerOwnEmployeeAccess() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));

        var result = service.authorizeEmployeeAccess(null);

        assertEquals(loggedEmployeeId, result.employeeId());
    }

    @Test
    @DisplayName("employeeId: manager acessa colaborador do mesmo tenant")
    void shouldAllowManagerAccessSameTenantEmployee() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(sameTenantEmployee.employeeId())).thenReturn(Optional.of(sameTenantEmployee));

        var result = service.authorizeEmployeeAccess(sameTenantEmployee.employeeId());

        assertEquals(sameTenantEmployee.employeeId(), result.employeeId());
    }

    @Test
    @DisplayName("employeeId: manager não acessa colaborador de outro tenant")
    void shouldDenyManagerAccessOtherTenantEmployee() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(otherTenantEmployee.employeeId())).thenReturn(Optional.of(otherTenantEmployee));

        assertThrows(ForbiddenException.class, () -> service.authorizeEmployeeAccess(otherTenantEmployee.employeeId()));
    }

    @Test
    @DisplayName("userId: CTO acessa qualquer usuário")
    void shouldAllowCtoAccessAnyUser() {
        var targetUser = buildUser(UUID.randomUUID(), otherTenantEmployee.employeeId());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(userProvider.findById(targetUser.userId())).thenReturn(Optional.of(targetUser));

        assertEquals(targetUser.userId(), service.authorizeUserAccess(targetUser.userId()).userId());
    }

    @Test
    @DisplayName("userId: falha quando usuário não existe")
    void shouldFailWhenTargetUserDoesNotExist() {
        UUID missingUserId = UUID.randomUUID();
        when(userProvider.findById(missingUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.authorizeUserAccess(missingUserId));
    }

    @Test
    @DisplayName("username: normaliza entrada e autoriza usuário resolvido")
    void shouldAuthorizeUserByLowercaseUsername() {
        var ownUser = buildUser(loggedUserId, loggedEmployeeId);
        when(userProvider.findByUsername("user@example.com")).thenReturn(Optional.of(ownUser));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(loggedUserId);

        assertEquals(loggedUserId, service.authorizeUserAccessByUsername("USER@EXAMPLE.COM").userId());
    }

    @Test
    @DisplayName("username: falha quando usuário não existe")
    void shouldFailWhenUsernameDoesNotExist() {
        when(userProvider.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.authorizeUserAccessByUsername("MISSING"));
    }

    @Test
    @DisplayName("userId: manager falha quando employee do usuário não existe")
    void shouldFailManagerUserAccessWhenTargetEmployeeDoesNotExist() {
        var targetUser = buildUser(UUID.randomUUID(), UUID.randomUUID());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(userProvider.findById(targetUser.userId())).thenReturn(Optional.of(targetUser));
        when(employeeProvider.findById(targetUser.employeeId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.authorizeUserAccess(targetUser.userId()));
    }

    @Test
    @DisplayName("userId: partner acessa apenas próprio usuário")
    void shouldAllowPartnerOwnUserAccess() {
        var ownUser = buildUser(loggedUserId, loggedEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(loggedUserId);
        when(userProvider.findById(loggedUserId)).thenReturn(Optional.of(ownUser));

        var result = service.authorizeUserAccess(loggedUserId);

        assertEquals(loggedUserId, result.userId());
    }

    @Test
    @DisplayName("userId: partner não acessa usuário de terceiro")
    void shouldDenyPartnerAccessOtherUser() {
        var targetUser = buildUser(UUID.randomUUID(), sameTenantEmployee.employeeId());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(loggedUserId);
        when(userProvider.findById(targetUser.userId())).thenReturn(Optional.of(targetUser));

        assertThrows(ForbiddenException.class, () -> service.authorizeUserAccess(targetUser.userId()));
    }

    @Test
    @DisplayName("userId: manager acessa usuário do mesmo tenant")
    void shouldAllowManagerAccessSameTenantUser() {
        var targetUser = buildUser(UUID.randomUUID(), sameTenantEmployee.employeeId());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
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
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(userProvider.findById(targetUser.userId())).thenReturn(Optional.of(targetUser));
        when(employeeProvider.findById(otherTenantEmployee.employeeId())).thenReturn(Optional.of(otherTenantEmployee));

        assertThrows(ForbiddenException.class, () -> service.authorizeUserAccess(targetUser.userId()));
    }

    @Test
    @DisplayName("documentId: sem employeeId informado autoriza pelo dono do documento")
    void shouldAuthorizeDocumentByDocumentOwnerWhenEmployeeIdIsMissing() {
        var documentId = UUID.randomUUID();
        var document = buildDocument(documentId, sameTenantEmployee.employeeId());
        when(documentProvider.findById(documentId)).thenReturn(document);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(sameTenantEmployee.employeeId())).thenReturn(Optional.of(sameTenantEmployee));

        assertEquals(documentId, service.authorizeDocumentAccess(documentId, null).documentId());
    }

    @Test
    @DisplayName("documentId: falha quando documento do colaborador não existe")
    void shouldFailWhenDocumentForEmployeeDoesNotExist() {
        var documentId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(sameTenantEmployee.employeeId())).thenReturn(Optional.of(sameTenantEmployee));
        when(documentProvider.findByIdAndEmployeeId(documentId, sameTenantEmployee.employeeId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.authorizeDocumentAccess(documentId, sameTenantEmployee.employeeId()));
    }

    @Test
    @DisplayName("documentId: manager acessa documento de colaborador do mesmo tenant")
    void shouldAllowManagerAccessSameTenantDocument() {
        var documentId = UUID.randomUUID();
        var document = buildDocument(documentId, sameTenantEmployee.employeeId());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
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
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));
        when(employeeProvider.findById(otherTenantEmployee.employeeId())).thenReturn(Optional.of(otherTenantEmployee));

        assertThrows(ForbiddenException.class,
                () -> service.authorizeDocumentAccess(documentId, otherTenantEmployee.employeeId()));
        verify(documentProvider, never()).findByIdAndEmployeeId(documentId, otherTenantEmployee.employeeId());
    }

    @Test
    @DisplayName("companyId: null usa empresa do autenticado")
    void shouldUseAuthenticatedCompanyWhenCompanyIdIsNull() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));

        assertEquals(companyAId, service.authorizeCompanyAccess(null));
    }

    @Test
    @DisplayName("companyId: manager acessa apenas própria empresa")
    void shouldAllowManagerOwnCompanyAccess() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));

        var result = service.authorizeCompanyAccess(companyAId);

        assertEquals(companyAId, result);
    }

    @Test
    @DisplayName("companyId: manager não acessa empresa de outro tenant")
    void shouldDenyManagerOtherCompanyAccess() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(authenticatedEmployee));

        assertThrows(ForbiddenException.class, () -> service.authorizeCompanyAccess(companyBId));
    }

    @Test
    @DisplayName("requireEmployeeFromCompany: retorna colaborador quando pertence à empresa")
    void shouldRequireEmployeeFromCompany() {
        when(employeeProvider.findById(sameTenantEmployee.employeeId())).thenReturn(Optional.of(sameTenantEmployee));

        assertEquals(sameTenantEmployee.employeeId(),
                service.requireEmployeeFromCompany(sameTenantEmployee.employeeId(), companyAId, "not found", "forbidden").employeeId());
    }

    @Test
    @DisplayName("requireEmployeeFromCompany: falha quando colaborador não existe ou pertence a outra empresa")
    void shouldFailRequireEmployeeFromCompany() {
        UUID missingEmployeeId = UUID.randomUUID();
        when(employeeProvider.findById(missingEmployeeId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.requireEmployeeFromCompany(missingEmployeeId, companyAId, "not found", "forbidden"));

        when(employeeProvider.findById(otherTenantEmployee.employeeId())).thenReturn(Optional.of(otherTenantEmployee));
        assertThrows(ForbiddenException.class,
                () -> service.requireEmployeeFromCompany(otherTenantEmployee.employeeId(), companyAId, "not found", "forbidden"));
    }

    @Test
    @DisplayName("authenticatedEmployee: falha quando colaborador autenticado não existe")
    void shouldFailWhenAuthenticatedEmployeeDoesNotExist() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.authorizeCompanyAccess(null));
    }

    @Test
    @DisplayName("companyId: cto pode acessar qualquer empresa")
    void shouldAllowCtoAccessAnyCompany() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        // CTO com requestedCompanyId não-nulo não chama getAuthenticatedEmployee()

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
    @Test
    void shouldBlockManagerAccessToEmployeeFromOtherTenant() {
        UUID managerEmployeeId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        UUID companyAId = UUID.randomUUID();
        UUID companyBId = UUID.randomUUID();

        Employee managerEmployee = buildEmployee(managerEmployeeId, companyAId);
        Employee targetEmployee = buildEmployee(targetEmployeeId, companyBId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(managerEmployee));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.of(targetEmployee));

        assertThrows(ForbiddenException.class, () -> service.authorizeEmployeeAccess(targetEmployeeId));
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
