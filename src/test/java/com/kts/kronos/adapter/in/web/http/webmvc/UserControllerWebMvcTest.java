package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.http.UserController;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.adapter.in.web.dto.user.AccessibleCompanyResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserSearchItemResponse;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUseCase useCase;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @MockitoBean
    private AuthCookieService authCookieService;

    @MockitoBean
    private AcceptTermsUseCase acceptTermsUseCase;

    @BeforeEach
    void setUpAuthCookieService() {
        when(authCookieService.expireAccessTokenCookie())
                .thenReturn(ResponseCookie.from("KRONOS_ACCESS_TOKEN", "")
                        .maxAge(Duration.ZERO)
                        .path("/")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .build());
    }

    @Test
    @DisplayName("registerUser: deve delegar criação")
    void shouldRegisterUser() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "john",
                                  "role": "MANAGER",
                                  "employeeId": "%s"
                                }
                                """.formatted(employeeId)))
                .andExpect(status().isCreated());

        verify(useCase).createUser(any());
    }

    @Test
    @DisplayName("registerUser: deve retornar 400 para role inválida")
    void shouldReturnBadRequestForInvalidRole() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "john",
                                  "role": "ADMIN",
                                  "employeeId": "%s"
                                }
                """.formatted(employeeId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("registerUser: deve traduzir erro de regra")
    void shouldTranslateExceptionWhenRegisteringUser() throws Exception {
        UUID employeeId = UUID.randomUUID();
        doThrow(new BadRequestException("Username já cadastrado"))
                .when(useCase).createUser(any());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "john",
                                  "role": "MANAGER",
                                  "employeeId": "%s"
                                }
                                """.formatted(employeeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Username já cadastrado"));
    }

    @Test
    @DisplayName("getUserByUsername: deve retornar usuário")
    void shouldGetUserByUsername() throws Exception {
        User user = user("john", true);
        when(useCase.getUserByUsername("john")).thenReturn(user);

        mockMvc.perform(get("/users/search/username/{userName}", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.userId().toString()))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("getUserByUsername: deve traduzir usuário inexistente")
    void shouldTranslateExceptionWhenGettingUserByUsername() throws Exception {
        when(useCase.getUserByUsername("john"))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado"));

        mockMvc.perform(get("/users/search/username/{userName}", "john"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("getUserById: deve retornar usuário por id")
    void shouldGetUserById() throws Exception {
        User user = user("john", true);
        when(useCase.getUserById(user.userId())).thenReturn(user);

        mockMvc.perform(get("/users/search/id/{userId}", user.userId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.userId().toString()))
                .andExpect(jsonPath("$.employeeId").value(user.employeeId().toString()));
    }

    @Test
    @DisplayName("allUsers: deve listar usuários sem filtro")
    void shouldListUsersWithoutFilter() throws Exception {
        when(useCase.listUsersResponse(null)).thenReturn(new UserListResponse(List.of(
                UserSearchItemResponse.fromDomain(user("john", true), false),
                UserSearchItemResponse.fromDomain(user("mary", false), false)
        )));

        mockMvc.perform(get("/users/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.users[0].username").value("john"))
                .andExpect(jsonPath("$.users[0].biometricConsentAccepted").value(false));

        verify(useCase).listUsersResponse(null);
    }

    @Test
    @DisplayName("allUsers: deve listar usuários com filtro active e propagar biometricConsentAccepted")
    void shouldListUsersWithActiveFilter() throws Exception {
        when(useCase.listUsersResponse(true)).thenReturn(new UserListResponse(List.of(
                UserSearchItemResponse.fromDomain(user("john", true), true)
        )));

        mockMvc.perform(get("/users/search").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].active").value(true))
                .andExpect(jsonPath("$.users[0].biometricConsentAccepted").value(true));

        verify(useCase).listUsersResponse(true);
    }

    @Test
    @DisplayName("updateUser: deve delegar atualização")
    void shouldUpdateUser() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/users/search/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "john.updated",
                                  "password": "encoded",
                                  "role": "CTO",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(useCase).updateUser(eq(userId), any());
    }

    @Test
    @DisplayName("updateUser: deve retornar 400 para role inválida")
    void shouldReturnBadRequestForInvalidUpdateRole() throws Exception {
        mockMvc.perform(patch("/users/search/{userId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateUser: deve traduzir usuário inexistente")
    void shouldTranslateExceptionWhenUpdatingUser() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Usuário não encontrado"))
                .when(useCase).updateUser(eq(userId), any());

        mockMvc.perform(patch("/users/search/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "john.updated"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("activateUser: deve alternar ativação")
    void shouldToggleUserActivation() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/users/toggle-activate/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(useCase).toggleActivate(userId);
    }

    @Test
    @DisplayName("deleteUser: deve delegar exclusão")
    void shouldDeleteUser() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(useCase).deleteUser(userId);
    }

    @Test
    @DisplayName("deleteUser: deve traduzir usuário inexistente")
    void shouldTranslateExceptionWhenDeletingUser() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Usuário não encontrado"))
                .when(useCase).deleteUser(userId);

        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("getOwnProfile: deve retornar perfil do usuário autenticado")
    void shouldGetOwnProfile() throws Exception {
        User user = user("john", true);
        when(useCase.getOwnProfileResponse()).thenReturn(UserResponse.fromDomain(user));

        mockMvc.perform(get("/users/own-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    @DisplayName("changePassword: deve retornar 204 e expirar cookie de sessão")
    void shouldChangeOwnPassword() throws Exception {
        mockMvc.perform(put("/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "oldPassword123",
                                  "newPassword": "newPassword123",
                                  "confirmPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("KRONOS_ACCESS_TOKEN=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(useCase).changeOwnPassword(any());
        verify(authCookieService).expireAccessTokenCookie();
    }

    @Test
    @DisplayName("changePassword: deve traduzir senha atual inválida")
    void shouldTranslateExceptionWhenChangingPassword() throws Exception {
        doThrow(new BadRequestException("Senha atual inválida"))
                .when(useCase).changeOwnPassword(any());

        mockMvc.perform(put("/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "oldPassword123",
                                  "newPassword": "newPassword123",
                                  "confirmPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Senha atual inválida"));
    }

    @Test
    @DisplayName("checkUsernameAvailability: retorna 200 quando username existe")
    void shouldReturnOkWhenUsernameExists() throws Exception {
        when(useCase.usernameExists("john")).thenReturn(true);

        mockMvc.perform(get("/users/check-username")
                        .param("username", "john"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("checkUsernameAvailability: retorna 404 quando username não existe")
    void shouldReturnNotFoundWhenUsernameDoesNotExist() throws Exception {
        when(useCase.usernameExists("john")).thenReturn(false);

        mockMvc.perform(get("/users/check-username")
                        .param("username", "john"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getMyCompanies: retorna lista de empresas acessíveis do usuário autenticado")
    void shouldGetMyCompanies() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(authUseCase.getAccessibleCompanies(userId)).thenReturn(List.of(
                new AccessibleCompanyResponse(companyId, "Kronos Tech", "12345678000195", "MANAGER", true, true)
        ));

        mockMvc.perform(get("/users/me/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Kronos Tech"))
                .andExpect(jsonPath("$[0].role").value("MANAGER"));
    }

    @Test
    @DisplayName("addCompanyAccess: cria acesso de empresa e retorna 201")
    void shouldAddCompanyAccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post("/users/{userId}/company-access", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n"
                                + "  \"companyId\": \"" + companyId + "\",\n"
                                + "  \"employeeId\": \"" + employeeId + "\",\n"
                                + "  \"role\": \"MANAGER\",\n"
                                + "  \"defaultCompany\": true\n"
                                + "}"))
                .andExpect(status().isCreated());

        verify(useCase).addCompanyAccess(eq(userId), any());
    }


    private static User user(String username, boolean active) {
        return new User(
                UUID.randomUUID(),
                username,
                "encoded-password",
                Role.MANAGER,
                active,
                UUID.randomUUID()
        );
    }
}
