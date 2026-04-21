package com.kts.kronos.config;

import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.security.TokenRevocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
                "frontend.base-url-local-2=http://local2.test"
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
    private TokenRevocationService tokenRevocationService;

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void shouldBlockEndpointThatWasPreviouslyPublicByAccident() throws Exception {
        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "12345678000199"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(companyUseCase);
    }

    @Test
    void shouldKeepHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldNotExposeOperationalActuatorEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isNotFound());
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
                        .header("Origin", "http://local.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://local.test"));
    }
}
