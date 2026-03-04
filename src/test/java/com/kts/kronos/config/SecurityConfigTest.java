package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;
    @Mock
    private AuthCookieService authCookieService;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                jwtUtils,
                customUserDetailsService,
                delegatedAuthenticationEntryPoint,
                new RateLimitProperties(120, 120, 1, 20, 20, 1),
                authCookieService
        );
        ReflectionTestUtils.setField(securityConfig, "activeProfiles", "");
        ReflectionTestUtils.setField(securityConfig, "defaultProfiles", "dev");
    }

    @Test
    void shouldConfigureCorsWithExplicitOriginsCredentialsAndExpectedHeadersAndMethods() {
        ReflectionTestUtils.setField(securityConfig, "recordUrl", "https://registro.kronos.app/");
        ReflectionTestUtils.setField(securityConfig, "plataformUrl", "https://plataforma.kronos.app");
        ReflectionTestUtils.setField(securityConfig, "local", "http://localhost:5173");
        ReflectionTestUtils.setField(securityConfig, "local_2", "http://127.0.0.1:5173");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getAllowedOrigins())
                .containsExactly(
                        "https://registro.kronos.app",
                        "https://plataforma.kronos.app",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173"
                );
        assertThat(cors.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(cors.getAllowedHeaders())
                .containsExactly(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With",
                        "Cache-Control",
                        "Pragma"
                );
    }

    @Test
    void shouldRejectWildcardOriginWhenCredentialsAreEnabled() {
        ReflectionTestUtils.setField(securityConfig, "recordUrl", "*");
        ReflectionTestUtils.setField(securityConfig, "plataformUrl", "");
        ReflectionTestUtils.setField(securityConfig, "local", "");
        ReflectionTestUtils.setField(securityConfig, "local_2", "");

        assertThatThrownBy(() -> securityConfig.corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wildcard '*'");
    }

    @Test
    void shouldRejectOriginWithPathOrWithoutHost() {
        ReflectionTestUtils.setField(securityConfig, "recordUrl", "https://frontend.kronos.app/app");
        ReflectionTestUtils.setField(securityConfig, "plataformUrl", "");
        ReflectionTestUtils.setField(securityConfig, "local", "");
        ReflectionTestUtils.setField(securityConfig, "local_2", "");

        assertThatThrownBy(() -> securityConfig.corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not contain path segments");
    }

    @Test
    void shouldRejectLocalhostOriginInHomologProfile() {
        ReflectionTestUtils.setField(securityConfig, "recordUrl", "https://registro.kronos.app");
        ReflectionTestUtils.setField(securityConfig, "plataformUrl", "https://localhost:5173");
        ReflectionTestUtils.setField(securityConfig, "local", "");
        ReflectionTestUtils.setField(securityConfig, "local_2", "");
        ReflectionTestUtils.setField(securityConfig, "activeProfiles", "homolog");

        assertThatThrownBy(() -> securityConfig.corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não pode usar origin local");
    }

    @Test
    void shouldRejectNonHttpsOriginInProductionProfile() {
        ReflectionTestUtils.setField(securityConfig, "recordUrl", "http://registro.kronos.app");
        ReflectionTestUtils.setField(securityConfig, "plataformUrl", "");
        ReflectionTestUtils.setField(securityConfig, "local", "");
        ReflectionTestUtils.setField(securityConfig, "local_2", "");
        ReflectionTestUtils.setField(securityConfig, "activeProfiles", "prod");

        assertThatThrownBy(() -> securityConfig.corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exige frontend origin com HTTPS");
    }
}
