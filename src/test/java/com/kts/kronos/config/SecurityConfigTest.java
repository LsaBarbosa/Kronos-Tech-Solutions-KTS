package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.in.web.exceptions.JsonAccessDeniedHandler;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;

    @Mock
    private TokenBlacklistProvider tokenBlacklistProvider;

    @Mock
    private UserProvider userProvider;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private JsonAccessDeniedHandler jsonAccessDeniedHandler;

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                jwtUtils,
                customUserDetailsService,
                userProvider,
                delegatedAuthenticationEntryPoint,
                tokenBlacklistProvider,
                authCookieService,
                jsonAccessDeniedHandler,
                handlerExceptionResolver
        );

        ReflectionTestUtils.setField(securityConfig, "recordUrl", "http://record.local");
        ReflectionTestUtils.setField(securityConfig, "plataformUrl", "http://platform.local");
        ReflectionTestUtils.setField(securityConfig, "local", "http://local.test");
        ReflectionTestUtils.setField(securityConfig, "local_2", "http://local2.test");
    }

    @Test
    void shouldExposeExpectedCorsConfiguration() {
        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/documents"));

        assertNotNull(configuration);
        assertEquals(
                List.of(
                        "http://record.local",
                        "http://platform.local",
                        "http://local.test",
                        "http://local2.test"
                ),
                configuration.getAllowedOrigins()
        );
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"), configuration.getAllowedMethods());
        assertEquals(List.of("*"), configuration.getAllowedHeaders());
        assertTrue(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }

    @Test
    void shouldReturnBCryptPasswordEncoder() {
        var encoder = securityConfig.passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
        String encoded = encoder.encode("secret123");
        assertTrue(encoder.matches("secret123", encoded));
    }

    @Test
    void shouldDelegateAuthenticationManagerCreation() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        AuthenticationManager result = securityConfig.authManager(authenticationConfiguration);

        assertSame(authenticationManager, result);
    }
}
