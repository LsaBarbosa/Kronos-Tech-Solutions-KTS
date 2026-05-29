package com.kts.kronos.integration;

import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.TermsController;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.BiometricConsentAcceptanceResult;
import com.kts.kronos.domain.model.BiometricConsentRevocationResult;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, BiometricConsentFlowIntegrationTest.MethodSecurityTestConfig.class})
@DisplayName("Biometric Consent Flow Integration Tests")
class BiometricConsentFlowIntegrationTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final UUID TEST_EMPLOYEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String BIOMETRIC_TERM_HASH = "4f6b7f5fd2ab55c4f0cc92b0c2d8a0f4f1c54fdcfe91d3984a5d7d0ad6b97f31";
    private static final AtomicBoolean CONSENT_ACCEPTED = new AtomicBoolean(false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcceptTermsUseCase acceptTermsUseCase;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private AuthCookieService authCookieService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setupTestData() {
        CONSENT_ACCEPTED.set(false);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(TEST_EMPLOYEE_ID);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(TEST_USER_ID);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("testuser");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");

        when(acceptTermsUseCase.getCurrentBiometricTerm()).thenReturn(new LegalText(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "1.0",
                "TERMO DE CONSENTIMENTO PARA TRATAMENTO DE DADOS BIOMÉTRICOS",
                "Conteúdo do termo biométrico",
                BIOMETRIC_TERM_HASH,
                true,
                Instant.parse("2026-05-01T10:00:00Z"),
                Instant.parse("2026-05-01T10:00:00Z")
        ));
        when(acceptTermsUseCase.getBiometricConsentStatus(TEST_EMPLOYEE_ID))
                .thenAnswer(invocation -> statusFromCurrentState());
        when(acceptTermsUseCase.getConsentHistory(TEST_EMPLOYEE_ID)).thenReturn(List.of());

        when(acceptTermsUseCase.acceptBiometricTerms(
                eq(TEST_EMPLOYEE_ID),
                eq(TEST_USER_ID),
                anyString(),
                anyString(),
                eq("1.0"),
                eq(BIOMETRIC_TERM_HASH)
        )).thenAnswer(invocation -> {
            CONSENT_ACCEPTED.set(true);
            return new BiometricConsentAcceptanceResult(
                    TEST_EMPLOYEE_ID,
                    TEST_USER_ID,
                    1L,
                    statusFromCurrentState()
            );
        });

        when(acceptTermsUseCase.revokeBiometricTerms(
                eq(TEST_EMPLOYEE_ID),
                anyString(),
                anyString()
        )).thenAnswer(invocation -> {
            CONSENT_ACCEPTED.set(false);
            return new BiometricConsentRevocationResult(
                    TEST_EMPLOYEE_ID,
                    TEST_USER_ID,
                    2L,
                    statusFromCurrentState()
            );
        });

        when(jwtUtils.generateToken(
                eq(TEST_EMPLOYEE_ID),
                eq("testuser"),
                eq("MANAGER"),
                eq(TEST_USER_ID),
                org.mockito.ArgumentMatchers.any(BiometricConsentStatus.class),
                eq(1L)
        )).thenReturn("renewed-token");
        when(authCookieService.createAccessTokenCookie("renewed-token"))
                .thenReturn(ResponseCookie.from("KRONOS_ACCESS_TOKEN", "renewed-token").build());
        when(authCookieService.clearAccessTokenCookie())
                .thenReturn(ResponseCookie.from("KRONOS_ACCESS_TOKEN", "").maxAge(0).build());
    }

    private String acceptBiometricJson() {
        return "{\"version\":\"1.0\",\"contentHashSha256\":\"" + BIOMETRIC_TERM_HASH + "\"}";
    }

    @Test
    @DisplayName("Should return current biometric term details")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldReturnCurrentBiometricTermDetails() throws Exception {
        mockMvc.perform(get("/terms/biometric/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").isNotEmpty())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.contentHashSha256").value(BIOMETRIC_TERM_HASH))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Should check consent status and return boolean")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldCheckConsentStatusReturnsBoolean() throws Exception {
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(false))
                .andExpect(jsonPath("$.currentVersion").value("1.0"))
                .andExpect(jsonPath("$.currentHash").value(BIOMETRIC_TERM_HASH))
                .andExpect(jsonPath("$.requiresNewAcceptance").value(true));
    }

    @Test
    @DisplayName("Should accept biometric term successfully")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldAcceptBiometricTermSuccessfully() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                        .contentType("application/json")
                        .content(acceptBiometricJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(true))
                .andExpect(jsonPath("$.requiresNewAcceptance").value(false));
    }

    @Test
    @DisplayName("Should reflect status change after acceptance")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldReflectStatusChangeAfterAcceptance() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                        .contentType("application/json")
                        .content(acceptBiometricJson()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(true));
    }

    @Test
    @DisplayName("Should revoke biometric consent successfully")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldRevokeBiometricConsentSuccessfully() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                        .contentType("application/json")
                        .content(acceptBiometricJson()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(false));
    }

    @Test
    @DisplayName("Should reflect status change after revocation")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldReflectStatusChangeAfterRevocation() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                        .contentType("application/json")
                        .content(acceptBiometricJson()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(true));

        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(false));
    }

    @Test
    @DisplayName("Should handle multiple accept/revoke cycles")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldHandleMultipleAcceptRevokeCycles() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/terms/accept-biometric")
                            .contentType("application/json")
                            .content(acceptBiometricJson()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/terms/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.biometricConsentAccepted").value(true));

            mockMvc.perform(delete("/terms/revoke-biometric"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/terms/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.biometricConsentAccepted").value(false));
        }
    }

    @Test
    @DisplayName("Should retrieve consent history")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldRetrieveConsentHistory() throws Exception {
        mockMvc.perform(get("/terms/consents/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should return unauthorized when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/terms/accept-biometric")
                        .contentType("application/json")
                        .content(acceptBiometricJson()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject invalid accept request with missing fields")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldRejectInvalidAcceptRequest() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                        .contentType("application/json")
                        .content("{\"version\":\"1.0\"}"))
                .andExpect(status().isBadRequest());
    }

    private BiometricConsentStatus statusFromCurrentState() {
        if (CONSENT_ACCEPTED.get()) {
            return new BiometricConsentStatus(
                    true,
                    "1.0",
                    BIOMETRIC_TERM_HASH,
                    "1.0",
                    BIOMETRIC_TERM_HASH,
                    false
            );
        }
        return new BiometricConsentStatus(
                false,
                null,
                null,
                "1.0",
                BIOMETRIC_TERM_HASH,
                true
        );
    }
}
