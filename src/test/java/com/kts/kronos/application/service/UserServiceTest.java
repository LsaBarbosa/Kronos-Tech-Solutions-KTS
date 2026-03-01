package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.PasswordPolicyValidator;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserProvider userProvider;
    @Mock DocumentProvider documentProvider;
    @Mock TimeRecordProvider timeRecordProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock EmployeeUseCase employeeUseCase;
    @Mock PasswordPolicyValidator passwordPolicyValidator;

    @InjectMocks UserService service;

    @Test
    void usernameExistsReturnsTrueWhenFound() {
        when(userProvider.findByUsername("john")).thenReturn(Optional.of(mock(User.class)));

        boolean exists = service.usernameExists("John");

        assertEquals(true, exists);
    }

    @Test
    void listUsersReturnsAllForCto() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        when(employee.companyId()).thenReturn(UUID.randomUUID());

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findAll()).thenReturn(List.of());

        assertEquals(0, service.listUsers(null).size());
    }

    @Test
    void changeOwnPasswordThrowsWhenCurrentPasswordMismatch() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john", "hashed", Role.MANAGER, true, UUID.randomUUID());

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.changeOwnPassword(new ChangePasswordRequest("wrong", "NewPass@123", "NewPass@123")));
    }

    @Test
    void toggleActivateFlipsStatusAndCallsEmployeeToggle() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User existing = new User(userId, "john", "hash", Role.PARTNER, true, employeeId);

        when(userProvider.findById(userId)).thenReturn(Optional.of(existing));

        service.toggleActivate(userId);

        verify(userProvider).save(argThat(u -> !u.active() && u.userId().equals(userId)));
        verify(employeeUseCase).toggleActivate(employeeId);
    }

    @Test
    void updateUserEncodesPasswordWhenProvided() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User existing = new User(userId, "john", "old", Role.PARTNER, true, employeeId);

        when(userProvider.findById(userId)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded");

        service.updateUser(userId, new UpdateUserRequest("Jane", "NewPass@123", "MANAGER", false));

        verify(passwordPolicyValidator).validate("NewPass@123");
        verify(userProvider).save(argThat(u ->
                u.username().equals("jane") &&
                u.password().equals("encoded") &&
                u.role() == Role.MANAGER &&
                !u.active()));
    }
}
