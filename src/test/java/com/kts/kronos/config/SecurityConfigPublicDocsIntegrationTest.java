package com.kts.kronos.config;

import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityTestApplication.class,
        properties = {
                "frontend.base-url-record=http://record.local",
                "frontend.base-url-plataform=http://platform.local",
                "frontend.base-url-local=http://local.test",
                "frontend.base-url-local-2=http://local2.test",
                "app.security.public-docs-enabled=true"
        }
)
@AutoConfigureMockMvc
class SecurityConfigPublicDocsIntegrationTest {

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
    private UserProvider userProvider;

    @MockitoBean
    private ObservabilityStatusUseCase observabilityStatusUseCase;

    @Test
    void shouldPermitPublicDocsWhenExplicitlyEnabled() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }
}
