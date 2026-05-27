package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.TermsController;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, AuthCookieService.class})
class TermsControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private AcceptTermsUseCase acceptTermsUseCase;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @Test
    void shouldAcceptBiometricTermsAndReturnRenewedToken() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        var consentStatus = new BiometricConsentStatus(
                true,
                "2026.05.21",
                "current-hash",
                "2026.05.21",
                "current-hash",
                false
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("lucas");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(acceptTermsUseCase.getBiometricConsentStatus(employeeId)).thenReturn(consentStatus);
        when(acceptTermsUseCase.acceptBiometricTerms(employeeId, userId, "127.0.0.1", "JUnit", "2026.05.21", "current-hash"))
                .thenReturn(new com.kts.kronos.domain.model.BiometricConsentAcceptanceResult(
                        employeeId,
                        userId,
                        0L,
                        consentStatus
                ));
        when(jwtUtils.generateToken(employeeId, "lucas", "MANAGER", userId, consentStatus, 0L))
                .thenReturn("renewed-token");
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        mockMvc.perform(post("/terms/accept-biometric")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "version": "2026.05.21",
                                  "contentHashSha256": "current-hash"
                                }
                                """)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(true))
                .andExpect(jsonPath("$.acceptedVersion").value("2026.05.21"))
                .andExpect(jsonPath("$.acceptedHash").value("current-hash"))
                .andExpect(jsonPath("$.currentVersion").value("2026.05.21"))
                .andExpect(jsonPath("$.currentHash").value("current-hash"))
                .andExpect(jsonPath("$.requiresNewAcceptance").value(false))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("KRONOS_ACCESS_TOKEN=renewed-token"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));

        verify(acceptTermsUseCase).acceptBiometricTerms(employeeId, userId, "127.0.0.1", "JUnit", "2026.05.21", "current-hash");
    }

    @Test
    void shouldReturnAcceptedStatus() throws Exception {
        UUID employeeId = UUID.randomUUID();

        var consentStatus = new BiometricConsentStatus(
                true,
                "2026.05.21",
                "current-hash",
                "2026.05.21",
                "current-hash",
                false
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptTermsUseCase.getBiometricConsentStatus(employeeId)).thenReturn(consentStatus);

        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(true))
                .andExpect(jsonPath("$.acceptedVersion").value("2026.05.21"))
                .andExpect(jsonPath("$.acceptedHash").value("current-hash"))
                .andExpect(jsonPath("$.currentVersion").value("2026.05.21"))
                .andExpect(jsonPath("$.currentHash").value("current-hash"))
                .andExpect(jsonPath("$.requiresNewAcceptance").value(false));
    }

    @Test
    void shouldReturnCurrentBiometricTerm() throws Exception {
        when(acceptTermsUseCase.getCurrentBiometricTerm()).thenReturn(new LegalText(
                UUID.randomUUID(),
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "2026.05.21",
                "Termo de Consentimento Biométrico",
                "Conteúdo do termo",
                "current-hash",
                true,
                Instant.parse("2026-05-21T09:00:00Z"),
                Instant.parse("2026-05-21T09:05:00Z")
        ));

        mockMvc.perform(get("/terms/biometric/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("BIOMETRIC_CONSENT_TERM"))
                .andExpect(jsonPath("$.version").value("2026.05.21"))
                .andExpect(jsonPath("$.contentHashSha256").value("current-hash"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRevokeBiometricTermsAndReturnRenewedToken() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        var revokedStatus = new BiometricConsentStatus(
                false,
                null,
                null,
                "2026.05.21",
                "current-hash",
                true
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptTermsUseCase.revokeBiometricTerms(employeeId, "127.0.0.1", "JUnit"))
                .thenReturn(new com.kts.kronos.domain.model.BiometricConsentRevocationResult(
                        employeeId,
                        userId,
                        0L,
                        revokedStatus
                ));
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        mockMvc.perform(delete("/terms/revoke-biometric")
                        .header("Authorization", "Bearer token")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(false))
                .andExpect(jsonPath("$.currentVersion").value("2026.05.21"))
                .andExpect(jsonPath("$.currentHash").value("current-hash"))
                .andExpect(jsonPath("$.requiresNewAcceptance").value(true))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Max-Age=0"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));

        verify(acceptTermsUseCase).revokeBiometricTerms(employeeId, "127.0.0.1", "JUnit");
    }

    @Test
    void shouldTranslateExceptionWhenAcceptingBiometricTerms() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("lucas");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        doThrow(new BadRequestException("Termo já aceito"))
                .when(acceptTermsUseCase).acceptBiometricTerms(employeeId, userId, "127.0.0.1", "JUnit", "2026.05.21", "current-hash");

        mockMvc.perform(post("/terms/accept-biometric")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "version": "2026.05.21",
                                  "contentHashSha256": "current-hash"
                                }
                                """)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Termo já aceito"));

        verify(acceptTermsUseCase).acceptBiometricTerms(employeeId, userId, "127.0.0.1", "JUnit", "2026.05.21", "current-hash");
    }
}
