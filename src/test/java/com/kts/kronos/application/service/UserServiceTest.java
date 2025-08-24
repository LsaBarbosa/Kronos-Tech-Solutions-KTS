package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Role;
import com.kts.kronos.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {
    @Mock
    private UserProvider userProvider;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @InjectMocks
    private UserService userService;

    private UUID userId;
    private UUID employeeId;
    private User user;
    private Employee employee;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        user = new User(userId, "testuser", "hashedpassword", Role.PARTNER, true, employeeId);
        employee = new Employee(employeeId, "Test Employee", "12345678901", "Dev", "test@email.com", 5000.0, "11999999999", true, null, UUID.randomUUID());
    }

    @Nested
    @DisplayName("Create User")
    class CreateUserTests {
        @Test
        @DisplayName("Should create a user successfully")
        void createUser_Success() {
            var request = new CreateUserRequest("newuser", "Password123", "MANAGER", employeeId);
            when(userProvider.findByUsername(anyString())).thenReturn(Optional.empty());
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
            when(passwordEncoder.encode(request.password())).thenReturn("hashedpassword");
            doNothing().when(userProvider).save(any(User.class));

            assertDoesNotThrow(() -> userService.createUser(request));
            verify(userProvider, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when username already exists")
        void createUser_UsernameAlreadyExists_ThrowsException() {
            var request = new CreateUserRequest("testuser", "Password123", "MANAGER", employeeId);
            when(userProvider.findByUsername(request.username())).thenReturn(Optional.of(user));

            BadRequestException thrown = assertThrows(BadRequestException.class, () -> userService.createUser(request));
            assertEquals(USERNAME_ALREADY_EXIST, thrown.getMessage());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when employee is not found")
        void createUser_EmployeeNotFound_ThrowsException() {
            var request = new CreateUserRequest("newuser", "Password123", "MANAGER", employeeId);
            when(userProvider.findByUsername(request.username())).thenReturn(Optional.empty());
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.createUser(request));
            assertEquals(EMPLOYEE_NOT_FOUND, thrown.getMessage());
        }

        @Test
        @DisplayName("Should throw BadRequestException for invalid password policy")
        void createUser_InvalidPasswordPolicy_ThrowsException() {
            var request = new CreateUserRequest("newuser", "weakpwd", "MANAGER", employeeId);
            when(userProvider.findByUsername(anyString())).thenReturn(Optional.empty());
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

            BadRequestException thrown = assertThrows(BadRequestException.class, () -> userService.createUser(request));
            assertEquals(INVALID_PASSWORD_POLICY, thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("Read User")
    class ReadUserTests {
        @Test
        @DisplayName("Should get user by username successfully")
        void getUserByUsername_Success() {
            when(userProvider.findByUsername(user.username())).thenReturn(Optional.of(user));

            User foundUser = userService.getUserByUsername(user.username());
            assertEquals(user, foundUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user by username is not found")
        void getUserByUsername_NotFound_ThrowsException() {
            when(userProvider.findByUsername(anyString())).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.getUserByUsername("nonexistent"));
            assertEquals(USER_NOT_FOUND, thrown.getMessage());
        }

        @Test
        @DisplayName("Should get user by ID successfully")
        void getUserById_Success() {
            when(jwtAuthenticatedUser.isWithEmployeeId(userId)).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));

            User foundUser = userService.getUserById(userId);
            assertEquals(user, foundUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user by ID is not found")
        void getUserById_NotFound_ThrowsException() {
            when(jwtAuthenticatedUser.isWithEmployeeId(any(UUID.class))).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(userId));
            assertEquals(USER_NOT_FOUND + userId, thrown.getMessage());
        }

        @Test
        @DisplayName("Should get all users when active is null")
        void listUsers_ActiveIsNull_ReturnsAll() {
            when(userProvider.findAll()).thenReturn(Collections.singletonList(user));
            List<User> users = userService.listUsers(null);
            assertFalse(users.isEmpty());
            verify(userProvider, times(1)).findAll();
        }

        @Test
        @DisplayName("Should get active users when active is true")
        void listUsers_ActiveIsTrue_ReturnsActive() {
            when(userProvider.findByActive(true)).thenReturn(Collections.singletonList(user));
            List<User> users = userService.listUsers(true);
            assertFalse(users.isEmpty());
            verify(userProvider, times(1)).findByActive(true);
        }

        @Test
        @DisplayName("Should get inactive users when active is false")
        void listUsers_ActiveIsFalse_ReturnsInactive() {
            when(userProvider.findByActive(false)).thenReturn(Collections.emptyList());
            List<User> users = userService.listUsers(false);
            assertTrue(users.isEmpty());
            verify(userProvider, times(1)).findByActive(false);
        }

        @Test
        @DisplayName("Should get own profile successfully")
        void getOwnProfile_Success() {
            when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));

            User ownProfile = userService.getOwnProfile();
            assertEquals(user, ownProfile);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when own profile is not found")
        void getOwnProfile_NotFound_ThrowsException() {
            when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.getOwnProfile());
            assertEquals(USER_NOT_FOUND, thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("Update User")
    class UpdateUserTests {
        @Test
        @DisplayName("Should update user successfully")
        void updateUser_Success() {
            var request = new UpdateUserRequest("updateduser", "NewPassword123", "CTO", false);
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("newhashedpassword");
            doNothing().when(userProvider).save(any(User.class));

            assertDoesNotThrow(() -> userService.updateUser(userId, request));
            verify(userProvider, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should not update password if not provided")
        void updateUser_NoPasswordChange() {
            var request = new UpdateUserRequest("updateduser", null, "CTO", false);
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));
            doNothing().when(userProvider).save(any(User.class));

            assertDoesNotThrow(() -> userService.updateUser(userId, request));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user to update is not found")
        void updateUser_UserNotFound_ThrowsException() {
            var request = new UpdateUserRequest("updateduser", "NewPassword123", "CTO", false);
            when(userProvider.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(userId, request));
            assertEquals(USER_NOT_FOUND, thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("Delete User")
    class DeleteUserTests {
        @Test
        @DisplayName("Should delete user successfully")
        void deleteUser_Success() {
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
            doNothing().when(userProvider).deleteById(userId);
            doNothing().when(employeeProvider).deleteById(employeeId);

            assertDoesNotThrow(() -> userService.deleteUser(userId));
            verify(userProvider, times(1)).deleteById(userId);
            verify(employeeProvider, times(1)).deleteById(employeeId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user to delete is not found")
        void deleteUser_UserNotFound_ThrowsException() {
            when(userProvider.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(userId));
            assertEquals(USER_NOT_FOUND, thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("Toggle Activate")
    class ToggleActivateTests {
        @Test
        @DisplayName("Should toggle user active status successfully")
        void toggleActivate_Success() {
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
            doNothing().when(userProvider).save(any(User.class));
            doNothing().when(employeeProvider).save(any(Employee.class));

            assertDoesNotThrow(() -> userService.toggleActivate(userId));
            verify(userProvider, times(1)).save(any(User.class));
            verify(employeeProvider, times(1)).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user is not found")
        void toggleActivate_UserNotFound_ThrowsException() {
            when(userProvider.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.toggleActivate(userId));
            assertEquals(USER_NOT_FOUND, thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("Change Own Password")
    class ChangeOwnPasswordTests {
        private ChangePasswordRequest request;

        @BeforeEach
        void setupChangePassword() {
            request = new ChangePasswordRequest("currentPassword", "NewPassword123", "NewPassword123");
        }

        @Test
        @DisplayName("Should change own password successfully")
        void changeOwnPassword_Success() {
            when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.of(user.withActive(true)));
            when(passwordEncoder.matches(request.currentPassword(), user.password())).thenReturn(true);
            when(passwordEncoder.encode(request.newPassword())).thenReturn("newhashedpassword");
            doNothing().when(userProvider).save(any(User.class));

            assertDoesNotThrow(() -> userService.changeOwnPassword(request));
            verify(userProvider, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user is not found")
        void changeOwnPassword_UserNotFound_ThrowsException() {
            when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.changeOwnPassword(request));
            assertEquals(USER_NOT_FOUND, thrown.getMessage());
        }

        @Test
        @DisplayName("Should throw BadRequestException for incorrect current password")
        void changeOwnPassword_IncorrectCurrentPassword_ThrowsException() {
            when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.currentPassword(), user.password())).thenReturn(false);

            BadRequestException thrown = assertThrows(BadRequestException.class, () -> userService.changeOwnPassword(request));
            assertEquals(INVALID_PASSWORD, thrown.getMessage());
        }

        @Test
        @DisplayName("Should throw BadRequestException for password confirmation mismatch")
        void changeOwnPassword_ConfirmationMismatch_ThrowsException() {
            var invalidRequest = new ChangePasswordRequest("currentPassword", "NewPassword123", "DifferentPassword");
            when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(invalidRequest.currentPassword(), user.password())).thenReturn(true);

            BadRequestException thrown = assertThrows(BadRequestException.class, () -> userService.changeOwnPassword(invalidRequest));
            assertEquals(INVALID_CONFIRM_PASSWORD, thrown.getMessage());
        }

        @Test
        @DisplayName("Should throw BadRequestException for invalid new password policy")
        void changeOwnPassword_InvalidNewPasswordPolicy_ThrowsException() {
            var invalidRequest = new ChangePasswordRequest("currentPassword", "weak", "weak");
            when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
            when(userProvider.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(invalidRequest.currentPassword(), user.password())).thenReturn(true);

            BadRequestException thrown = assertThrows(BadRequestException.class, () -> userService.changeOwnPassword(invalidRequest));
            assertEquals(INVALID_PASSWORD_POLICY, thrown.getMessage());
        }
    }
}