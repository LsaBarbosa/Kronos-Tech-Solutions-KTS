package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.domain.model.Role;
import com.kts.kronos.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserUseCase useCase;

    private UUID userId;
    private UUID employeeId;
    private User user;
    private UserResponse userResponse;
    private final String managerRole = "MANAGER";
    private final String partnerRole = "PARTNER";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        user = new User(userId, "testuser", "hashedpassword", Role.PARTNER, true, employeeId);
        userResponse = UserResponse.fromDomain(user);
    }

    // MANAGER ENDPOINTS
    // --------------------------------------------------

    @Nested
    @DisplayName("Manager Operations")
    class ManagerTests {
        @Test
        @DisplayName("POST /users - Should register a user and return 201 Created")
        void registerUser_Success() throws Exception {
            CreateUserRequest request = new CreateUserRequest("newuser", "Password123", "MANAGER", employeeId);
            doNothing().when(useCase).createUser(any(CreateUserRequest.class));

            mockMvc.perform(post(USER)
                            .with(user("managerUser").roles(managerRole))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("GET /users/search/{userName} - Should return a user by username and 200 OK")
        void getUserByUsername_Success() throws Exception {
            when(useCase.getUserByUsername(user.username())).thenReturn(user);

            mockMvc.perform(get(USER + USER_BY_USERNAME, user.username())
                            .with(user("managerUser").roles(managerRole))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(userResponse)));
        }

        @Test
        @DisplayName("GET /users/search - Should return a list of all users and 200 OK")
        void allUsers_Success() throws Exception {
            when(useCase.listUsers(any())).thenReturn(Collections.singletonList(user));
            UserListResponse expectedResponse = new UserListResponse(Collections.singletonList(userResponse));

            mockMvc.perform(get(USER + USERS)
                            .with(user("managerUser").roles(managerRole))
                            .param("active", "true")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
        }

        @Test
        @DisplayName("PATCH /users/search/{userId} - Should update a user and return 200 OK")
        void updateUser_Success() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("updatedUser", null, "CTO", false);
            doNothing().when(useCase).updateUser(eq(userId), any(UpdateUserRequest.class));

            mockMvc.perform(patch(USER + UPDATE_USER, userId)
                            .with(user("managerUser").roles(managerRole))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PATCH /users/toggle-activate/{id} - Should toggle user activation and return 200 OK")
        void toggleActivate_Success() throws Exception {
            doNothing().when(useCase).toggleActivate(userId);

            mockMvc.perform(patch(USER + TOGGLE_ACTIVATE_USER, userId)
                            .with(user("managerUser").roles(managerRole))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /users/{id} - Should delete a user and return 204 No Content")
        void deleteUser_Success() throws Exception {
            doNothing().when(useCase).deleteUser(userId);

            mockMvc.perform(delete(USER + DELETE_USER, userId)
                            .with(user("managerUser").roles(managerRole))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    // ANY_EMPLOYEE ENDPOINTS (MANAGER & PARTNER)
    // --------------------------------------------------

    @Nested
    @DisplayName("Any Employee Operations")
    class AnyEmployeeTests {
        @Test
        @DisplayName("GET /users/search?userId={userId} - Should return a user by ID and 200 OK (PARTNER role)")
        void getUserId_Partner_Success() throws Exception {
            when(useCase.getUserById(userId)).thenReturn(user);

            mockMvc.perform(get(USER + USER_BY_ID, userId)
                            .with(user("partnerUser").roles(partnerRole))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(userResponse)));
        }

        @Test
        @DisplayName("GET /users/own-profile - Should return own profile and 200 OK (PARTNER role)")
        void getOwnProfile_Success() throws Exception {
            when(useCase.getOwnProfile()).thenReturn(user);

            mockMvc.perform(get(USER + OWN_USER_PROFILE)
                            .with(user("partnerUser").roles(partnerRole))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(userResponse)));
        }

        @Test
        @DisplayName("PUT /users/password - Should change own password and return 204 No Content")
        void changePassword_Success() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("old_password", "NewPassword123", "NewPassword123");
            doNothing().when(useCase).changeOwnPassword(any(ChangePasswordRequest.class));

            mockMvc.perform(put(USER + PASSWORD)
                            .with(user("partnerUser").roles(partnerRole))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }
    }
}