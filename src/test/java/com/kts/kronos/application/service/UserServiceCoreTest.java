package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceCoreTest {

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
    private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock
    private KronosMetrics kronosMetrics;

    private UUID userId;
    private UUID employeeId;
    private User existingUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        existingUser = new User(userId, "john", "hashed-password", Role.MANAGER, true, employeeId);
    }

    @Test
    @DisplayName("createUser: cria user com senha aleatória criptografada")
    void shouldCreateUserWithEncodedRandomPassword() {
        CreateUserRequest request = new CreateUserRequest("John", "MANAGER", employeeId);

        when(userProvider.existsByUsername("john")).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(buildEmployee(employeeId)));
        when(userProvider.existsByEmployeeId(employeeId)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");

        service.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("John", saved.username());
        assertEquals("encoded-random-password", saved.password());
        assertEquals(Role.MANAGER, saved.role());
        assertEquals(employeeId, saved.employeeId());
        assertEquals(0L, saved.sessionVersion());
    }

    @Test
    @DisplayName("createUser: falha quando employee já possui user")
    void shouldFailCreateUserWhenEmployeeAlreadyHasLinkedUser() {
        CreateUserRequest request = new CreateUserRequest("John", "MANAGER", employeeId);

        when(userProvider.existsByUsername("john")).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(buildEmployee(employeeId)));
        when(userProvider.existsByEmployeeId(employeeId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.createUser(request));
        verify(userProvider, never()).save(any());
    }

    @Test
    @DisplayName("toggleActivate: alterna usuário e propaga para employee")
    void shouldToggleUserAndPropagateToEmployee() {
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existingUser);

        service.toggleActivate(userId);

        verify(userProvider).save(existingUser.withActive(false).incrementSessionVersion());
        verify(employeeUseCase).toggleActivate(employeeId);
    }

    @Test
    @DisplayName("changeOwnPassword: incrementa a versão de sessão quando troca senha")
    void changeOwnPassword_shouldIncrementSessionVersion() {
        ChangePasswordRequest request = new ChangePasswordRequest("old-pass", "Abcd1234", "Abcd1234");
        existingUser = new User(userId, "john", "hashed-password", Role.MANAGER, true, employeeId, 2L, null, null, null);

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existingUser);
        when(passwordEncoder.matches("old-pass", "hashed-password")).thenReturn(true);
        when(passwordEncoder.encode("Abcd1234")).thenReturn("new-hash");

        service.changeOwnPassword(request);

        verify(userProvider).save(new User(
                userId,
                "john",
                "new-hash",
                Role.MANAGER,
                true,
                employeeId,
                3L,
                null,
                null,
                null
        ));
    }

    @Test
    @DisplayName("changeOwnPassword: falha quando senha atual é inválida")
    void shouldFailChangeOwnPasswordWhenCurrentPasswordIsInvalid() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "Abcd1234", "Abcd1234");

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existingUser);
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.changeOwnPassword(request));
        verify(userProvider, never()).save(any());
    }

    @Test
    @DisplayName("getOwnProfile: retorna usuário autenticado")
    void shouldReturnOwnProfile() {
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existingUser);

        User result = service.getOwnProfile();

        assertEquals(existingUser, result);
    }

    @Test
    @DisplayName("usernameExists: normaliza username para lowercase")
    void shouldNormalizeUsernameWhenCheckingAvailability() {
        when(userProvider.existsByUsername("john")).thenReturn(true);

        boolean exists = service.usernameExists("John");

        assertTrue(exists);
        verify(userProvider).existsByUsername("john");
    }

    private Employee buildEmployee(UUID employeeId) {
        return new Employee(
                employeeId,
                "Employee",
                "12345678901",
                "12345678901",
                "Developer",
                "employee@kts.com",
                3000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                UUID.randomUUID(),
                null,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                DayOfWeek.MONDAY,
                null,
                null
        );
    }
}
