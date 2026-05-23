package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTenantSecurityTest {

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
    private KronosMetrics kronosMetrics;

    @Test
    @DisplayName("updateUser: manager pode operar usuário do mesmo tenant")
    void shouldAllowManagerUpdateUserFromSameTenant() {
        UUID userId = UUID.randomUUID();
        var existing = new User(userId, "old.user", "hashed", Role.PARTNER, true, UUID.randomUUID());
        var request = new UpdateUserRequest("new.user", null, "PARTNER", true);

        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existing);

        service.updateUser(userId, request);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(captor.capture());
        assertEquals(userId, captor.getValue().userId());
        assertEquals("new.user", captor.getValue().username());
    }

    @Test
    @DisplayName("updateUser: bloqueia operação cross-tenant")
    void shouldBlockManagerUpdateUserFromOtherTenant() {
        UUID userId = UUID.randomUUID();
        var request = new UpdateUserRequest("new.user", null, "PARTNER", true);
        when(domainAuthorizationService.authorizeUserAccess(userId))
                .thenThrow(new ForbiddenException("forbidden"));

        assertThrows(ForbiddenException.class, () -> service.updateUser(userId, request));
        verify(userProvider, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser: inativa recursos vinculados sem apagar historico legal")
    void shouldDeactivateUserAndLinkedEmployeeWithoutDeletingLegalHistory() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        var existing = new User(userId, "john", "hashed", Role.PARTNER, true, employeeId);

        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existing);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        service.deleteUser(userId);

        verify(userProvider).save(argThat(saved ->
                saved.userId().equals(userId)
                        && !saved.active()
                        && actorId.equals(saved.deletedBy())
                        && "USER_DELETE".equals(saved.deactivationReason())
        ));
        verify(employeeProvider).save(argThat(saved ->
                saved.employeeId().equals(employeeId)
                        && !saved.active()
                        && actorId.equals(saved.deletedBy())
                        && "USER_DELETE".equals(saved.deactivationReason())
        ));
        verify(acceptTermsUseCase, never()).revokeBiometricTerms(any(), any(), any());
        verify(documentProvider, never()).deleteByEmployeeId(any());
        verify(timeRecordProvider, never()).deleteByEmployeeId(any());
        verify(userProvider, never()).deleteById(any());
        verify(employeeProvider, never()).deleteById(any());
    }

    private static com.kts.kronos.domain.model.Employee employee(UUID employeeId) {
        return new com.kts.kronos.domain.model.Employee(
                employeeId,
                "John Doe",
                "12345678901",
                "12345678901",
                "Analista",
                "john@kts.com",
                1000.0,
                "11999999999",
                true,
                null,
                UUID.randomUUID(),
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
