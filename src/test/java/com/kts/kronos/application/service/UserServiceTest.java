package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserProvider userProvider;
    @Mock
    private DocumentProvider documentProvider;
    @Mock
    private TimeRecordProvider timeRecordProvider;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private EmployeeUseCase employeeUseCase;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;
    @Mock
    private AcceptTermsUseCase acceptTermsUseCase;
    @Mock
    private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock
    private KronosMetrics kronosMetrics;

    @Test
    @DisplayName("createUser: deve rejeitar username ja existente")
    void shouldRejectExistingUsernameOnCreate() {
        when(userProvider.existsByUsername("manager@kts.com")).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> service.createUser(new CreateUserRequest("Manager@KTS.com", "MANAGER", UUID.randomUUID()))
        );
    }

    @Test
    @DisplayName("createUser: deve rejeitar colaborador inexistente")
    void shouldRejectMissingEmployeeOnCreate() {
        UUID employeeId = UUID.randomUUID();
        when(userProvider.existsByUsername("manager@kts.com")).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.createUser(new CreateUserRequest("Manager@KTS.com", "MANAGER", employeeId))
        );
    }

    @Test
    @DisplayName("createUser: deve salvar usuario com senha sistemica")
    void shouldCreateUserWithGeneratedPassword() {
        UUID employeeId = UUID.randomUUID();
        when(userProvider.existsByUsername("manager@kts.com")).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, UUID.randomUUID())));
        when(userProvider.existsByEmployeeId(employeeId)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-random");

        service.createUser(new CreateUserRequest("Manager@KTS.com", "MANAGER", employeeId));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(captor.capture());
        assertEquals("Manager@KTS.com", captor.getValue().username());
        assertEquals("hashed-random", captor.getValue().password());
        assertEquals(Role.MANAGER, captor.getValue().role());
        assertEquals(employeeId, captor.getValue().employeeId());
        assertEquals(0L, captor.getValue().sessionVersion());
    }

    @Test
    @DisplayName("createUser: corrida de username duplicado deve virar 409")
    void shouldTranslateDuplicateUsernameRaceToConflictOnCreate() {
        UUID employeeId = UUID.randomUUID();
        when(userProvider.existsByUsername("manager@kts.com")).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, UUID.randomUUID())));
        when(userProvider.existsByEmployeeId(employeeId)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-random");
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(userProvider).save(any(User.class));

        assertThrows(
                ConflictException.class,
                () -> service.createUser(new CreateUserRequest("Manager@KTS.com", "MANAGER", employeeId))
        );
    }

    @Test
    @DisplayName("createUser: deve rejeitar colaborador ja vinculado")
    void shouldRejectEmployeeAlreadyLinkedOnCreate() {
        UUID employeeId = UUID.randomUUID();
        when(userProvider.existsByUsername("manager@kts.com")).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, UUID.randomUUID())));
        when(userProvider.existsByEmployeeId(employeeId)).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> service.createUser(new CreateUserRequest("Manager@KTS.com", "MANAGER", employeeId))
        );
    }

    @Test
    @DisplayName("getUserByUsername/getUserById: devem delegar autorizacao de dominio")
    void shouldDelegateUserLookupsToDomainAuthorization() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UUID.randomUUID(), Role.MANAGER, true);
        when(domainAuthorizationService.authorizeUserAccessByUsername("manager@kts.com")).thenReturn(user);
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(user);

        assertEquals(user, service.getUserByUsername("manager@kts.com"));
        assertEquals(user, service.getUserById(userId));
    }

    @Test
    @DisplayName("listUsers: CTO deve listar todos ou filtrar por ativo")
    void shouldListUsersAsCto() {
        List<User> all = List.of(user(UUID.randomUUID(), UUID.randomUUID(), Role.MANAGER, true));
        List<User> inactive = List.of(user(UUID.randomUUID(), UUID.randomUUID(), Role.PARTNER, false));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(userProvider.findAll()).thenReturn(all);
        when(userProvider.findByActive(false)).thenReturn(inactive);

        assertEquals(all, service.listUsers(null));
        assertEquals(inactive, service.listUsers(false));
    }

    @Test
    @DisplayName("listUsers: perfil tenant deve retornar vazio quando empresa nao tem colaboradores")
    void shouldReturnEmptyTenantUsersWhenCompanyHasNoEmployees() {
        UUID companyId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeCompanyAccess(null)).thenReturn(companyId);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of());

        assertTrue(service.listUsers(null).isEmpty());
        verify(userProvider, never()).findByEmployeeIds(any());
    }

    @Test
    @DisplayName("listUsers: PARTNER deve enxergar somente usuarios MANAGER")
    void shouldFilterPartnerTenantUsersToManagers() {
        UUID companyId = UUID.randomUUID();
        UUID managerEmployeeId = UUID.randomUUID();
        UUID partnerEmployeeId = UUID.randomUUID();
        User manager = user(UUID.randomUUID(), managerEmployeeId, Role.MANAGER, true);
        User partner = user(UUID.randomUUID(), partnerEmployeeId, Role.PARTNER, true);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(domainAuthorizationService.authorizeCompanyAccess(null)).thenReturn(companyId);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(
                employee(managerEmployeeId, companyId),
                employee(partnerEmployeeId, companyId)
        ));
        when(userProvider.findByEmployeeIdsAndActive(Set.of(managerEmployeeId, partnerEmployeeId), true))
                .thenReturn(List.of(manager, partner));

        assertEquals(List.of(manager), service.listUsers(true));
    }

    @Test
    @DisplayName("updateUser: deve atualizar campos informados")
    void shouldUpdateUserWithProvidedFields() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User existing = user(userId, employeeId, Role.MANAGER, true);
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existing);
        when(passwordEncoder.encode("Abcdef12")).thenReturn("hashed-new");

        service.updateUser(userId, new UpdateUserRequest("New@KTS.com", "Abcdef12", "PARTNER", false));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("new@kts.com", saved.username());
        assertEquals("hashed-new", saved.password());
        assertEquals(Role.PARTNER, saved.role());
        assertEquals(false, saved.active());
        assertEquals(employeeId, saved.employeeId());
    }

    @Test
    @DisplayName("updateUser: corrida de username duplicado deve virar 409")
    void shouldTranslateDuplicateUsernameRaceToConflictOnUpdate() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(user(userId, employeeId, Role.MANAGER, true));
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(userProvider).save(any(User.class));

        assertThrows(
                ConflictException.class,
                () -> service.updateUser(userId, new UpdateUserRequest("duplicado@kts.com", null, null, null))
        );
    }

    @Test
    @DisplayName("updateUser: deve manter campos atuais quando request vier vazio")
    void shouldKeepExistingUserFieldsWhenUpdateRequestIsPartial() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, UUID.randomUUID(), Role.MANAGER, true);
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existing);

        service.updateUser(userId, new UpdateUserRequest(null, " ", null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(captor.capture());
        assertEquals(existing.username(), captor.getValue().username());
        assertEquals(existing.password(), captor.getValue().password());
        assertEquals(existing.role(), captor.getValue().role());
        assertEquals(existing.active(), captor.getValue().active());
    }

    @Test
    @DisplayName("updateUser: deve rejeitar senha fora da politica")
    void shouldRejectWeakPasswordOnUpdate() {
        UUID userId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeUserAccess(userId))
                .thenReturn(user(userId, UUID.randomUUID(), Role.MANAGER, true));

        assertThrows(
                BadRequestException.class,
                () -> service.updateUser(userId, new UpdateUserRequest(null, "weak", null, null))
        );
    }

    @Test
    @DisplayName("deleteUser: deve inativar usuario e colaborador preservando dados legais")
    void shouldDeactivateUserAndPreserveDependentData() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(user(userId, employeeId, Role.MANAGER, true));
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, UUID.randomUUID())));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);

        service.deleteUser(userId);

        verify(userProvider).save(argThat(saved ->
                saved.userId().equals(userId)
                        && !saved.active()
                        && actorId.equals(saved.deletedBy())
                        && "USER_DELETE".equals(saved.deactivationReason())
                        && saved.deletedAt() != null
        ));
        verify(employeeProvider).save(argThat(saved ->
                saved.employeeId().equals(employeeId)
                        && !saved.active()
                        && actorId.equals(saved.deletedBy())
                        && "USER_DELETE".equals(saved.deactivationReason())
                        && saved.deletedAt() != null
        ));
        verify(acceptTermsUseCase, never()).revokeBiometricTerms(any(), any(), any());
        verify(documentProvider, never()).deleteByEmployeeId(any());
        verify(timeRecordProvider, never()).deleteByEmployeeId(any());
        verify(userProvider, never()).deleteById(any());
        verify(employeeProvider, never()).deleteById(any());
    }

    @Test
    @DisplayName("toggleActivate: deve inverter usuario e colaborador")
    void shouldToggleUserAndEmployeeActivation() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(user(userId, employeeId, Role.MANAGER, false));

        service.toggleActivate(userId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(captor.capture());
        assertTrue(captor.getValue().active());
        verify(employeeUseCase).toggleActivate(employeeId);
    }

    @Test
    @DisplayName("changeOwnPassword: deve validar senha atual e confirmacao")
    void shouldValidateOwnPasswordChangeErrors() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, UUID.randomUUID(), Role.MANAGER, true);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.password())).thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () -> service.changeOwnPassword(new ChangePasswordRequest("wrong", "Abcdef12", "Abcdef12"))
        );

        when(passwordEncoder.matches("current", user.password())).thenReturn(true);
        assertThrows(
                BadRequestException.class,
                () -> service.changeOwnPassword(new ChangePasswordRequest("current", null, "Abcdef12"))
        );
    }

    @Test
    @DisplayName("changeOwnPassword: deve atualizar senha propria e incrementar sessionVersion")
    void changeOwnPassword_shouldIncrementSessionVersion() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "manager@kts.com", "stored-hash", Role.MANAGER, true, UUID.randomUUID(), 4L, null, null, null);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(user);
        when(passwordEncoder.matches("current", user.password())).thenReturn(true);
        when(passwordEncoder.encode("Abcdef12")).thenReturn("hashed");

        service.changeOwnPassword(new ChangePasswordRequest("current", "Abcdef12", "Abcdef12"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(captor.capture());
        assertEquals("hashed", captor.getValue().password());
        assertEquals(user.username(), captor.getValue().username());
        assertEquals(5L, captor.getValue().sessionVersion());
    }

    @Test
    @DisplayName("usernameExists: deve normalizar username")
    void shouldNormalizeUsernameExists() {
        when(userProvider.existsByUsername("manager@kts.com")).thenReturn(true);

        assertTrue(service.usernameExists("Manager@KTS.com"));
    }

    private static User user(UUID userId, UUID employeeId, Role role, boolean active) {
        return new User(userId, "manager@kts.com", "stored-hash", role, active, employeeId);
    }

    private static Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Ana Paula",
                "12345678901",
                "12345678901",
                "Analista",
                "ana@kts.com",
                1000.0,
                "11999999999",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                companyId,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
