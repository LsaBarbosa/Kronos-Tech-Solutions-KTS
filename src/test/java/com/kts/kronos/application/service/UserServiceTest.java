package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.PasswordPolicyValidator;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    void createUserThrowsWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createUser(new CreateUserRequest("John", "MANAGER", employeeId)));
    }

    @Test
    void createUserThrowsWhenUsernameAlreadyExists() {
        UUID employeeId = UUID.randomUUID();
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
        when(userProvider.findByUsername("john")).thenReturn(Optional.of(mock(User.class)));

        assertThrows(BadRequestException.class,
                () -> service.createUser(new CreateUserRequest("John", "MANAGER", employeeId)));
    }

    @Test
    void createUserSavesWithEncodedRandomPassword() {
        UUID employeeId = UUID.randomUUID();
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
        when(userProvider.findByUsername("john")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        service.createUser(new CreateUserRequest("John", "PARTNER", employeeId));

        verify(userProvider).save(argThat(u ->
                u.username().equals("John") &&
                        u.password().equals("encoded") &&
                        u.role() == Role.PARTNER &&
                        u.employeeId().equals(employeeId) &&
                        u.active()));
    }

    @Test
    void getUserByUsernameThrowsWhenUserNotFound() {
        when(userProvider.findByUsername("john")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUserByUsername("John"));
    }

    @Test
    void getUserByUsernameReturnsUserForCtoWithoutEmployeeValidation() {
        UUID employeeId = UUID.randomUUID();
        User user = new User(UUID.randomUUID(), "john", "hash", Role.MANAGER, true, employeeId);

        when(userProvider.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");

        assertEquals(user, service.getUserByUsername("John"));
        verify(employeeProvider, never()).findById(any());
    }

    @Test
    void getUserByUsernameThrowsWhenAuthenticatedEmployeeNotFound() {
        UUID authEmployeeId = UUID.randomUUID();
        User user = new User(UUID.randomUUID(), "john", "hash", Role.MANAGER, true, UUID.randomUUID());

        when(userProvider.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(authEmployeeId);
        when(employeeProvider.findById(authEmployeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUserByUsername("John"));
    }

    @Test
    void getUserByUsernameThrowsWhenTargetEmployeeNotFound() {
        UUID authEmployeeId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        Employee authEmployee = mock(Employee.class);

        User user = new User(UUID.randomUUID(), "john", "hash", Role.MANAGER, true, targetEmployeeId);
        when(userProvider.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(authEmployeeId);
        when(employeeProvider.findById(authEmployeeId)).thenReturn(Optional.of(authEmployee));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUserByUsername("John"));
    }

    @Test
    void getUserByUsernameThrowsWhenCompanyDoesNotMatch() {
        UUID authEmployeeId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        Employee authEmployee = mock(Employee.class);
        Employee targetEmployee = mock(Employee.class);
        when(authEmployee.companyId()).thenReturn(UUID.randomUUID());
        when(targetEmployee.companyId()).thenReturn(UUID.randomUUID());

        User user = new User(UUID.randomUUID(), "john", "hash", Role.MANAGER, true, targetEmployeeId);
        when(userProvider.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(authEmployeeId);
        when(employeeProvider.findById(authEmployeeId)).thenReturn(Optional.of(authEmployee));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.of(targetEmployee));

        assertThrows(ResourceNotFoundException.class, () -> service.getUserByUsername("John"));
    }

    @Test
    void getUserByUsernameReturnsWhenCompanyMatches() {
        UUID authEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        Employee authEmployee = mock(Employee.class);
        Employee targetEmployee = mock(Employee.class);
        when(authEmployee.companyId()).thenReturn(companyId);
        when(targetEmployee.companyId()).thenReturn(companyId);

        User user = new User(UUID.randomUUID(), "john", "hash", Role.MANAGER, true, targetEmployeeId);
        when(userProvider.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(authEmployeeId);
        when(employeeProvider.findById(authEmployeeId)).thenReturn(Optional.of(authEmployee));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.of(targetEmployee));

        assertEquals(user, service.getUserByUsername("John"));
    }

    @Test
    void getUserByIdUsesTokenUserIdForPartner() {
        UUID tokenUserId = UUID.randomUUID();
        UUID requestedId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        User user = new User(tokenUserId, "john", "hash", Role.PARTNER, true, employeeId);
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.getuserId()).thenReturn(tokenUserId);
        when(userProvider.findById(tokenUserId)).thenReturn(Optional.of(user));
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);

        Employee employee = mock(Employee.class);
        UUID companyId = UUID.randomUUID();
        when(employee.companyId()).thenReturn(companyId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        assertEquals(user, service.getUserById(requestedId));
        verify(userProvider, never()).findById(requestedId);
    }

    @Test
    void getUserByIdThrowsWhenNotFound() {
        UUID userId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(userProvider.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUserById(userId));
    }

    @Test
    void listUsersThrowsWhenAuthenticatedEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listUsers(true));
    }

    @Test
    void listUsersReturnsAllForCtoWhenActiveIsNull() {
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
    void listUsersReturnsByActiveForCtoWhenFilterProvided() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        when(employee.companyId()).thenReturn(UUID.randomUUID());

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findByActive(false)).thenReturn(List.of());

        assertEquals(0, service.listUsers(false).size());
    }

    @Test
    void listUsersReturnsByCompanyForNonCto() {
        UUID authEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = mock(Employee.class);
        when(employee.companyId()).thenReturn(companyId);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(authEmployeeId);
        when(employeeProvider.findById(authEmployeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of());

        assertEquals(0, service.listUsers(true).size());
    }

    @Test
    void updateUserThrowsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userProvider.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateUser(userId, new UpdateUserRequest("john", null, null, null)));
    }

    @Test
    void updateUserKeepsExistingValuesWhenNotProvided() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User existing = new User(userId, "john", "old", Role.PARTNER, true, employeeId);
        when(userProvider.findById(userId)).thenReturn(Optional.of(existing));

        service.updateUser(userId, new UpdateUserRequest(null, null, null, null));

        verify(userProvider).save(argThat(u ->
                u.userId().equals(userId) &&
                        u.username().equals("john") &&
                        u.password().equals("old") &&
                        u.role() == Role.PARTNER &&
                        u.active() &&
                        u.employeeId().equals(employeeId)));
        verifyNoInteractions(passwordPolicyValidator);
    }


    @Test
    void updateUserDoesNotEncodeWhenPasswordIsBlank() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User existing = new User(userId, "john", "old", Role.PARTNER, true, employeeId);
        when(userProvider.findById(userId)).thenReturn(Optional.of(existing));

        service.updateUser(userId, new UpdateUserRequest("johnny", "   ", "PARTNER", true));

        verify(userProvider).save(argThat(u -> u.password().equals("old") && u.username().equals("johnny")));
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(passwordPolicyValidator);
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

    @Test
    void deleteUserThrowsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userProvider.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUser(userId));
    }

    @Test
    void deleteUserDeletesAllRelatedData() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User user = new User(userId, "john", "old", Role.PARTNER, true, employeeId);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));

        service.deleteUser(userId);

        verify(documentProvider).deleteByEmployeeId(employeeId);
        verify(timeRecordProvider).deleteByEmployeeId(employeeId);
        verify(userProvider).deleteById(userId);
        verify(employeeProvider).deleteById(employeeId);
    }

    @Test
    void toggleActivateThrowsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userProvider.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.toggleActivate(userId));
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
    void toggleActivateTurnsFalseIntoTrue() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User existing = new User(userId, "john", "hash", Role.PARTNER, false, employeeId);

        when(userProvider.findById(userId)).thenReturn(Optional.of(existing));

        service.toggleActivate(userId);

        verify(userProvider).save(argThat(User::active));
        verify(employeeUseCase).toggleActivate(employeeId);
    }

    @Test
    void changeOwnPasswordThrowsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(userProvider.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.changeOwnPassword(new ChangePasswordRequest("old", "NewPass@123", "NewPass@123")));
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
    void changeOwnPasswordThrowsWhenNewPasswordIsNull() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john", "hashed", Role.MANAGER, true, UUID.randomUUID());

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hashed")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> service.changeOwnPassword(new ChangePasswordRequest("old", null, "anything")));
    }

    @Test
    void changeOwnPasswordThrowsWhenConfirmationIsInvalid() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john", "hashed", Role.MANAGER, true, UUID.randomUUID());

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hashed")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> service.changeOwnPassword(new ChangePasswordRequest("old", "NewPass@123", "Different")));
    }

    @Test
    void changeOwnPasswordUpdatesPasswordWhenValid() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john", "hashed", Role.MANAGER, true, UUID.randomUUID());

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded");

        service.changeOwnPassword(new ChangePasswordRequest("old", "NewPass@123", "NewPass@123"));

        verify(passwordPolicyValidator).validate("NewPass@123");
        verify(userProvider).save(argThat(saved -> saved.password().equals("encoded") && saved.userId().equals(userId)));
    }

    @Test
    void getOwnProfileReturnsAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john", "hash", Role.MANAGER, true, UUID.randomUUID());
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));

        assertEquals(user, service.getOwnProfile());
    }

    @Test
    void usernameExistsReturnsTrueWhenFoundAndFalseWhenMissing() {
        when(userProvider.findByUsername("john")).thenReturn(Optional.of(mock(User.class)));
        assertTrue(service.usernameExists("John"));

        when(userProvider.findByUsername("jane")).thenReturn(Optional.empty());
        assertFalse(service.usernameExists("Jane"));
    }
}
