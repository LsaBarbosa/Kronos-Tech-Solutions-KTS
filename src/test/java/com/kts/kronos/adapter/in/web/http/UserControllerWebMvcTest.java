package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.application.port.in.usecase.UserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUseCase useCase;

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
                .andExpect(status().isOk());

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
}