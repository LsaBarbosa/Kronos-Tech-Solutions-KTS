package com.kts.kronos.application;

import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.UserService;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
    @DisplayName("deleteUser: remove recursos vinculados do usuário autorizado")
    void shouldDeleteUserAndLinkedEmployeeData() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var existing = new User(userId, "john", "hashed", Role.PARTNER, true, employeeId);

        when(domainAuthorizationService.authorizeUserAccess(userId)).thenReturn(existing);

        service.deleteUser(userId);

        var inOrder = inOrder(documentProvider, timeRecordProvider, userProvider, employeeProvider);
        inOrder.verify(documentProvider).deleteByEmployeeId(employeeId);
        inOrder.verify(timeRecordProvider).deleteByEmployeeId(employeeId);
        inOrder.verify(userProvider).deleteById(userId);
        inOrder.verify(employeeProvider).deleteById(employeeId);
    }
}
