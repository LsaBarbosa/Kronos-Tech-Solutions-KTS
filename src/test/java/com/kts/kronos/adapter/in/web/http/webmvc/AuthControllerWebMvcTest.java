package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinResponse;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.AuthController;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.PasswordlessCheckinUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import jakarta.annotation.Resource;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, AuthCookieService.class})
class AuthControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private PasswordlessCheckinUseCase passwordlessCheckinUseCase;

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
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("KRONOS_ACCESS_TOKEN=jwt-token"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax"),
                        org.hamcrest.Matchers.containsString("Path=/")
                )));
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
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("KRONOS_ACCESS_TOKEN=face-token"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));
    }

    @Test
    void shouldReturnForbiddenWhenBiometricConsentIsNotAcceptedForFaceLogin() throws Exception {
        doThrow(new TermsNotAcceptedException(
                "Consentimento biométrico ativo é obrigatório para login facial.",
                "https://termo.kronossolutions.tech/"
        )).when(authUseCase).loginFace("base64-image", true);

        mockMvc.perform(post("/auth/login-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faceImageBase64": "base64-image",
                                  "livenessPassed": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TERMS_NOT_ACCEPTED"))
                .andExpect(jsonPath("$.detail").value("Consentimento biométrico ativo é obrigatório para login facial."))
                .andExpect(jsonPath("$.redirectUrl").value("https://termo.kronossolutions.tech/"));
    }

    @Test
    void shouldCheckinFaceSuccessfully() throws Exception {
        when(passwordlessCheckinUseCase.checkinFace(any())).thenReturn(new FaceCheckinResponse(
                "Login realizado com sucesso.",
                "Entrada às 08:01! (NSR: 123)",
                "CHECKIN",
                10,
                OffsetDateTime.parse("2026-06-26T08:01:00-03:00")
        ));

        mockMvc.perform(post("/auth/checkin-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faceImageBase64": "base64-image",
                                  "latitude": -22.90,
                                  "longitude": -43.20,
                                  "accuracy": 18.5,
                                  "livenessPassed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginMessage").value("Login realizado com sucesso."))
                .andExpect(jsonPath("$.recordMessage").value("Entrada às 08:01! (NSR: 123)"))
                .andExpect(jsonPath("$.actionType").value("CHECKIN"))
                .andExpect(jsonPath("$.autoLogoutAfterSeconds").value(10))
                .andExpect(jsonPath("$.recordedAt").value("2026-06-26T08:01:00-03:00"));
    }

    @Test
    void shouldReturnForbiddenWhenBiometricConsentIsNotAcceptedForCheckinFace() throws Exception {
        doThrow(new TermsNotAcceptedException(
                "Consentimento biométrico ativo é obrigatório para login facial.",
                "https://termo.kronossolutions.tech/"
        )).when(passwordlessCheckinUseCase).checkinFace(any());

        mockMvc.perform(post("/auth/checkin-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faceImageBase64": "base64-image",
                                  "latitude": -22.90,
                                  "longitude": -43.20
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TERMS_NOT_ACCEPTED"))
                .andExpect(jsonPath("$.detail").value("Consentimento biométrico ativo é obrigatório para login facial."))
                .andExpect(jsonPath("$.redirectUrl").value("https://termo.kronossolutions.tech/"));
    }

    @Test
    void shouldReturnBadRequestWhenCheckinFacePayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/checkin-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faceImageBase64": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
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
    void shouldAcceptMaskedCpfAndLongEmailForRecoverPassword() throws Exception {
        String longEmail = "usuario.recuperacao.senha.extenso.exemplo@empresa.com";

        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "cpf": "123.456.789-01",
                                  "email": "%s"
                                }
                                """, longEmail)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ArgumentCaptor<RecoverPasswordRequest> captor = ArgumentCaptor.forClass(RecoverPasswordRequest.class);
        verify(authUseCase).recoverPassword(captor.capture());
        assertThat(captor.getValue().cpf()).isEqualTo("123.456.789-01");
        assertThat(captor.getValue().email()).isEqualTo(longEmail);
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
