package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.AuthController;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class AuthControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @Test
    void shouldLoginSuccessfully() throws Exception {
        when(authUseCase.login("user", "pass")).thenReturn("jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user",
                                  "password": "pass"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        when(authUseCase.login("user", "wrong-pass"))
                .thenThrow(new BadCredentialsException("bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user",
                                  "password": "wrong-pass"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuário ou senha inválidos"));
    }

    @Test
    void shouldLoginFaceSuccessfully() throws Exception {
        when(authUseCase.loginFace("base64-image", true)).thenReturn("face-token");

        mockMvc.perform(post("/auth/login-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faceImageBase64": "base64-image",
                                  "livenessPassed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("face-token"));
    }

    @Test
    void shouldReturnBadRequestWhenFacePayloadIsBlank() throws Exception {
        mockMvc.perform(post("/auth/login-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faceImageBase64": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].name").value("faceImageBase64"));
    }

    @Test
    void shouldRecoverPasswordWithoutResponseBody() throws Exception {
        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "12345678901",
                                  "email": "user@test.com"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(authUseCase).recoverPassword(any(RecoverPasswordRequest.class));
    }

    @Test
    void shouldTranslateRecoverPasswordException() throws Exception {
        doThrow(new BadRequestException("E-mail não confere"))
                .when(authUseCase).recoverPassword(any(RecoverPasswordRequest.class));

        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "12345678901",
                                  "email": "user@test.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("E-mail não confere"));
    }

    @Test
    void shouldResetPasswordWithoutResponseBody() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "reset-token",
                                  "newPassword": "Abcd1234",
                                  "confirmPassword": "Abcd1234"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(authUseCase).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void shouldTranslateResetPasswordException() throws Exception {
        doThrow(new BadRequestException("Token inválido"))
                .when(authUseCase).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "reset-token",
                                  "newPassword": "Abcd1234",
                                  "confirmPassword": "Abcd1234"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Token inválido"));
    }
}
