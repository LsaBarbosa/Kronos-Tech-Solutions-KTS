package com.kts.kronos.config;

import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
import com.kts.kronos.observability.domain.ObservabilityStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
@SpringBootTest(
        classes = SecurityTestApplication.class,
        properties = {
                "frontend.base-url-record=http://record.local",
                "frontend.base-url-plataform=http://platform.local",
                "frontend.base-url-local=http://local.test",
                "frontend.base-url-local-2=http://local2.test",
                "frontend.allowed-origins=http://localhost:5173,http://localhost:5174,http://localhost:5175,http://platform.local,http://record.local"
        }
)
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private CompanyUseCase companyUseCase;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private TokenBlacklistProvider tokenBlacklistProvider;

    @MockitoBean
    private ObservabilityStatusUseCase observabilityStatusUseCase;

    @Test
    void shouldKeepLoginEndpointPublic() throws Exception {
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
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("KRONOS_ACCESS_TOKEN=jwt-token"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));
    }

    @Test
    void shouldExposeCsrfTokenWithExpectedHeaderAndCookie() throws Exception {
        mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("KRONOS_CSRF_TOKEN=")));
    }

    @Test
    void shouldRejectAuthenticatedMutationWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/companies")
                        .with(user("cto").roles("CTO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Token CSRF ausente ou inválido."));

        verifyNoInteractions(companyUseCase);
    }

    @Test
    void shouldAllowAuthenticatedMutationWithCsrfToken() throws Exception {
        mockMvc.perform(post("/companies")
                        .with(user("cto").roles("CTO"))
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kronos",
                                  "cnpj": "60077790000135",
                                  "email": "ops@kronos.com",
                                  "address": {
                                    "postalCode": "01001000",
                                    "number": "100"
                                  },
                                  "location": {
                                    "latitude": -23.5,
                                    "longitude": -46.6
                                  }
                                }
                                """))
                .andExpect(status().isCreated());

        verify(companyUseCase).createCompany(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldLogoutWithoutBearerAndExpireAuthCookie() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("KRONOS_ACCESS_TOKEN="),
                        org.hamcrest.Matchers.containsString("Max-Age=0"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));
    }

    @Test
    void shouldBlockEndpointThatWasPreviouslyPublicByAccident() throws Exception {
        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "12345678000199"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(companyUseCase);
    }

    @Test
    void shouldRejectProtectedEndpointWithInvalidAuthCookie() throws Exception {
        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "12345678000199")
                        .cookie(new Cookie("KRONOS_ACCESS_TOKEN", "invalid-or-expired-token")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(companyUseCase);
    }

    @Test
    void shouldExposeObservabilityStatusPublic() throws Exception {
        when(observabilityStatusUseCase.getStatus()).thenReturn(new ObservabilityStatus(
                "kronos-backend",
                "UP",
                "local",
                java.time.OffsetDateTime.parse("2026-05-19T14:00:00-03:00")
        ));

        mockMvc.perform(get("/observability/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("kronos-backend"))
                .andExpect(jsonPath("$.environment").value("local"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldKeepActuatorHealthProtectedOnMainPort() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldKeepUsersOwnProfileProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/users/own-profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldKeepTimeRecordCheckinProtectedWithoutToken() throws Exception {
        mockMvc.perform(post("/records/checkin"))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() == 401
                        || result.getResponse().getStatus() == 403));
    }

    @Test
    void shouldKeepDocumentsProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldKeepLegalAfdProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/legal/afd"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotExposeOperationalActuatorEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGenerateCorrelationIdHeaderWhenMissing() throws Exception {
        when(observabilityStatusUseCase.getStatus()).thenReturn(new ObservabilityStatus(
                "kronos-backend",
                "UP",
                "local",
                java.time.OffsetDateTime.parse("2026-05-19T14:00:00-03:00")
        ));

        mockMvc.perform(get("/observability/status"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void shouldPreserveIncomingCorrelationIdHeader() throws Exception {
        when(observabilityStatusUseCase.getStatus()).thenReturn(new ObservabilityStatus(
                "kronos-backend",
                "UP",
                "local",
                java.time.OffsetDateTime.parse("2026-05-19T14:00:00-03:00")
        ));

        mockMvc.perform(get("/observability/status")
                        .header("X-Correlation-Id", "corr-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "corr-123"));
    }

    @Test
    void shouldDisableOpenApiByDefault() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isNotFound());
    }
    @Test
    void shouldAllowCorsPreflightWithoutAuthentication() throws Exception {
        mockMvc.perform(options("/companies/check-cnpj")
                        .header("Origin", "http://localhost:5175")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5175"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
