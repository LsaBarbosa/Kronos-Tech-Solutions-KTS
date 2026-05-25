package com.kts.kronos.adapter.in.web.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
@DisplayName("Token Refresh Integration Tests")
class TokenRefreshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private AuthCookieService authCookieService;

    private static final String REFRESH_ENDPOINT = "/auth/refresh";
    private static final String TEST_TOKEN = "expired.test.token";
    private static final String NEW_TOKEN = "new.valid.token";

    @Test
    @DisplayName("Should successfully refresh valid expired token")
    void refreshWithValidExpiredToken() throws Exception {
        when(authCookieService.extractToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(TEST_TOKEN));
        when(authUseCase.refreshToken(TEST_TOKEN)).thenReturn(NEW_TOKEN);
        when(authCookieService.createAccessTokenCookie(NEW_TOKEN))
                .thenReturn(org.springframework.http.ResponseCookie.from("KRONOS_ACCESS_TOKEN", NEW_TOKEN)
                        .build());

        mockMvc.perform(post(REFRESH_ENDPOINT))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("Should return 401 when no token in cookie")
    void refreshWithNoToken() throws Exception {
        when(authCookieService.extractToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(post(REFRESH_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 when token is still valid")
    void refreshWithStillValidToken() throws Exception {
        when(authCookieService.extractToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(TEST_TOKEN));
        when(authUseCase.refreshToken(TEST_TOKEN))
                .thenThrow(new BadRequestException("Token ainda é válido. Nenhuma renovação necessária."));

        mockMvc.perform(post(REFRESH_ENDPOINT))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when token is tampered")
    void refreshWithTamperedToken() throws Exception {
        when(authCookieService.extractToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(TEST_TOKEN));
        when(authUseCase.refreshToken(TEST_TOKEN))
                .thenThrow(new BadRequestException("Token inválido ou corrompido."));

        mockMvc.perform(post(REFRESH_ENDPOINT))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when token is blacklisted")
    void refreshWithBlacklistedToken() throws Exception {
        when(authCookieService.extractToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(TEST_TOKEN));
        when(authUseCase.refreshToken(TEST_TOKEN))
                .thenThrow(new BadRequestException("Token foi revogado."));

        mockMvc.perform(post(REFRESH_ENDPOINT))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when session is invalidated")
    void refreshWithInvalidatedSession() throws Exception {
        when(authCookieService.extractToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(TEST_TOKEN));
        when(authUseCase.refreshToken(TEST_TOKEN))
                .thenThrow(new BadRequestException("Sessão invalidada. Faça login novamente."));

        mockMvc.perform(post(REFRESH_ENDPOINT))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 403 when user is inactive")
    void refreshWithInactiveUser() throws Exception {
        when(authCookieService.extractToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(TEST_TOKEN));
        when(authUseCase.refreshToken(TEST_TOKEN))
                .thenThrow(new ForbiddenException("Conta de usuário inativa."));

        mockMvc.perform(post(REFRESH_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 when user not found")
    void refreshWithUserNotFound() throws Exception {
        when(authCookieService.extractToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(TEST_TOKEN));
        when(authUseCase.refreshToken(TEST_TOKEN))
                .thenThrow(new BadRequestException("Usuário não encontrado."));

        mockMvc.perform(post(REFRESH_ENDPOINT))
                .andExpect(status().isBadRequest());
    }
}
