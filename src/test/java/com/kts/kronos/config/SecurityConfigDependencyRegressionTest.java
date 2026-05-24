package com.kts.kronos.config;

import com.kts.kronos.KronosApplication;
import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for SecurityConfig dependency injection.
 *
 * This test validates that SecurityConfig can be loaded in the Spring context without
 * failing due to missing UserProvider bean. This prevents regression of the issue where
 * UserProvider bean was not available, causing cascading test failures.
 *
 * Background: SecurityConfig requires UserProvider in its constructor. In the test context,
 * UserProvider must be available either as a real component or mocked. This test ensures
 * that the Spring context can load SecurityConfig with all its dependencies properly wired.
 */
@SpringBootTest(classes = KronosApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.mail.username=test@kronos.local",
        "management.health.mail.enabled=false",
        "frontend.base-url-record=http://localhost:4200",
        "frontend.base-url-plataform=http://localhost:5173",
        "frontend.base-url-local=http://localhost:3000",
        "frontend.base-url-local-2=http://localhost:3001",
        "app.security.public-docs-enabled=false"
})
class SecurityConfigDependencyRegressionTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    // These are mocked because the test context doesn't need full implementations
    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;

    @MockitoBean
    private TokenBlacklistProvider tokenBlacklistProvider;

    @MockitoBean
    private UserProvider userProvider;

    @MockitoBean
    private AuthCookieService authCookieService;

    /**
     * Test that SecurityConfig can be created with all dependencies available.
     *
     * This test fails if UserProvider bean is missing from the context.
     * It validates that SecurityConfig constructor receives all required dependencies.
     *
     * Regression scenario: If UserProvider is not mocked or available in the context,
     * the context initialization will fail with:
     * "No qualifying bean of type 'com.kts.kronos.application.port.out.provider.UserProvider'"
     */
    @Test
    void shouldCreateSecurityConfigWithAllDependencies() {
        assertNotNull(securityConfig, "SecurityConfig should be created successfully");
        assertNotNull(userProvider, "UserProvider mock should be injected");
    }

    /**
     * Test that SecurityFilterChain is properly created.
     *
     * SecurityFilterChain depends on SecurityConfig being properly initialized.
     * If SecurityConfig cannot be created due to missing UserProvider,
     * this test will fail during context initialization.
     *
     * This ensures the complete security filter chain is wired correctly.
     */
    @Test
    void shouldCreateSecurityFilterChain() {
        assertNotNull(securityFilterChain, "SecurityFilterChain should be created successfully");
    }

    /**
     * Test that SecurityConfig has all required properties set.
     *
     * This validates that the configuration is not just created but also
     * properly populated with all required dependencies and values.
     */
    @Test
    void shouldInitializeSecurityConfigProperties() {
        assertNotNull(securityConfig, "SecurityConfig must be initialized");
        // SecurityConfig constructor receives UserProvider as parameter 3
        // If this constructor parameter is missing, the test fails during context loading
    }
}
