package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa filtros de segurança para focar na lógica do Controller
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserUseCase userUseCase;

    private static final String BASE_URL = "/users";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID EMP_ID = UUID.randomUUID();
    private User userMock;

    @BeforeEach
    void setup() {
        // Objeto de domínio padrão
        userMock = new User(USER_ID, "usuario.teste", "hash-senha", Role.MANAGER, true, EMP_ID);
    }

    // ==================================================================================
    // 1. CRIAR USUÁRIO (POST)
    // ==================================================================================

    @Test
    @DisplayName("Deve criar usuário com sucesso (200 OK)")
    void shouldRegisterUserSuccessfully() throws Exception {
        CreateUserRequest request = new CreateUserRequest("novo.user", "MANAGER", EMP_ID);

        doNothing().when(userUseCase).createUser(any(CreateUserRequest.class));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userUseCase).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se payload de criação for inválido")
    void shouldReturn400WhenCreatePayloadInvalid() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest("", "ROLE_INVALIDA", null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se username já existir (Regra de Negócio)")
    void shouldReturn400WhenUsernameAlreadyExists() throws Exception {
        CreateUserRequest request = new CreateUserRequest("duplicado", "PARTNER", EMP_ID);

        doThrow(new BadRequestException(USERNAME_ALREADY_EXIST))
                .when(userUseCase).createUser(any());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(USERNAME_ALREADY_EXIST));
    }

    // ==================================================================================
    // 2. BUSCAR USUÁRIOS (GET)
    // ==================================================================================

    @Test
    @DisplayName("Deve buscar usuário por Username com sucesso (200 OK)")
    void shouldGetUserByUsernameSuccessfully() throws Exception {
        when(userUseCase.getUserByUsername("usuario.teste")).thenReturn(userMock);

        mockMvc.perform(get(BASE_URL + "/search/username/{userName}", "usuario.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("usuario.teste"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao buscar por username inexistente")
    void shouldReturn404WhenGetUserByUsernameDoesNotExist() throws Exception {
        when(userUseCase.getUserByUsername("inexistente"))
                .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/search/username/{userName}", "inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso (200 OK)")
    void shouldGetUserByIdSuccessfully() throws Exception {
        when(userUseCase.getUserById(USER_ID)).thenReturn(userMock);

        mockMvc.perform(get(BASE_URL + "/search/id/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    @DisplayName("Deve listar todos os usuários (200 OK)")
    void shouldListAllUsersSuccessfully() throws Exception {
        when(userUseCase.listUsers(true)).thenReturn(List.of(userMock));

        mockMvc.perform(get(BASE_URL + "/search")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].username").value("usuario.teste"));
    }

    @Test
    @DisplayName("Deve listar usuários sem filtro de ativo (200 OK)")
    void shouldListAllUsersWithoutActiveFilterSuccessfully() throws Exception {
        when(userUseCase.listUsers(null)).thenReturn(List.of(userMock));

        mockMvc.perform(get(BASE_URL + "/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].userId").value(USER_ID.toString()));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao listar usuários sem resultados")
    void shouldReturn404WhenListUsersHasNoResult() throws Exception {
        when(userUseCase.listUsers(true)).thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/search").param("active", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se usuário não for encontrado")
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userUseCase.getUserById(USER_ID)).thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/search/id/{userId}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    // ==================================================================================
    // 3. ATUALIZAR E DELETAR (PATCH / DELETE)
    // ==================================================================================

    @Test
    @DisplayName("Deve atualizar usuário com sucesso (200 OK)")
    void shouldUpdateUserSuccessfully() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("novo.nome", null, "PARTNER", true);

        doNothing().when(userUseCase).updateUser(eq(USER_ID), any(UpdateUserRequest.class));

        mockMvc.perform(patch(BASE_URL + "/search/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request ao atualizar usuário com payload inválido")
    void shouldReturn400WhenUpdatePayloadIsInvalid() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("", "", "INVALID_ROLE", true);

        mockMvc.perform(patch(BASE_URL + "/search/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao atualizar usuário inexistente")
    void shouldReturn404WhenUpdateUserDoesNotExist() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("novo.nome", null, "PARTNER", true);
        doThrow(new ResourceNotFoundException(USER_NOT_FOUND))
                .when(userUseCase).updateUser(eq(USER_ID), any(UpdateUserRequest.class));

        mockMvc.perform(patch(BASE_URL + "/search/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve ativar/desativar usuário com sucesso (200 OK)")
    void shouldToggleUserActivationSuccessfully() throws Exception {
        doNothing().when(userUseCase).toggleActivate(USER_ID);

        mockMvc.perform(patch(BASE_URL + "/toggle-activate/{userId}", USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao ativar/desativar usuário inexistente")
    void shouldReturn404WhenToggleUserActivationForMissingUser() throws Exception {
        doThrow(new ResourceNotFoundException(USER_NOT_FOUND)).when(userUseCase).toggleActivate(USER_ID);

        mockMvc.perform(patch(BASE_URL + "/toggle-activate/{userId}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve deletar usuário com sucesso (200 OK)")
    void shouldDeleteUserSuccessfully() throws Exception {
        doNothing().when(userUseCase).deleteUser(USER_ID);

        mockMvc.perform(delete(BASE_URL + "/{userId}", USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao deletar usuário inexistente")
    void shouldReturn404WhenDeleteUserDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException(USER_NOT_FOUND)).when(userUseCase).deleteUser(USER_ID);

        mockMvc.perform(delete(BASE_URL + "/{userId}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    // ==================================================================================
    // 4. PERFIL E SENHA (OWN PROFILE)
    // ==================================================================================

    @Test
    @DisplayName("Deve obter o próprio perfil (200 OK)")
    void shouldGetOwnProfileSuccessfully() throws Exception {
        when(userUseCase.getOwnProfile()).thenReturn(userMock);

        mockMvc.perform(get(BASE_URL + "/own-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("usuario.teste"));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao obter próprio perfil inexistente")
    void shouldReturn404WhenGetOwnProfileDoesNotExist() throws Exception {
        when(userUseCase.getOwnProfile()).thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/own-profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve alterar a própria senha com sucesso (204 No Content)")
    void shouldChangePasswordSuccessfully() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("SenhaVelha1!", "SenhaNova1!", "SenhaNova1!");

        doNothing().when(userUseCase).changeOwnPassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put(BASE_URL + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se a senha atual estiver incorreta")
    void shouldReturn400WhenCurrentPasswordIsWrong() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("Errada", "Nova", "Nova");

        doThrow(new BadRequestException(INVALID_PASSWORD))
                .when(userUseCase).changeOwnPassword(any());

        mockMvc.perform(put(BASE_URL + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID_PASSWORD));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao alterar senha de usuário não encontrado")
    void shouldReturn404WhenChangingPasswordForMissingUser() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("SenhaVelha1!", "SenhaNova1!", "SenhaNova1!");

        doThrow(new ResourceNotFoundException(USER_NOT_FOUND))
                .when(userUseCase).changeOwnPassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put(BASE_URL + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    // ==================================================================================
    // 5. CHECK USERNAME (GET)
    // ==================================================================================

    @Test
    @DisplayName("Deve retornar 200 OK se o username já existir (Indisponível)")
    void shouldReturn200IfUsernameExists() throws Exception {
        String username = "usuario.existente";
        when(userUseCase.usernameExists(username)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/check-username")
                        .param("username", username))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se o username não existir (Disponível)")
    void shouldReturn404IfUsernameDoesNotExists() throws Exception {
        String username = "usuario.livre";
        when(userUseCase.usernameExists(username)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/check-username")
                        .param("username", username))
                .andExpect(status().isNotFound());
    }
}
