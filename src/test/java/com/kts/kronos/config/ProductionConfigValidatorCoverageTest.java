package com.kts.kronos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers branches only reachable by calling private methods directly via reflection.
 * These branches are dead code when validate() is called normally:
 * - validateJwtSecretLength: `jwtSecret == null` (JWT_SECRET is always non-null after validateRequiredVariable passes)
 * - validateMailConfiguration: `isBlank()` branches (tests use null, not blank)
 * - validateSecretTerm: `isBlank()` branch
 * - validateCorsConfiguration: `isBlank()` branch
 */
class ProductionConfigValidatorCoverageTest {

    private Environment env;
    private ProductionConfigValidator validator;

    @BeforeEach
    void setUp() {
        env = mock(Environment.class);
        validator = new ProductionConfigValidator(env);
    }

    private void invokePrivate(String methodName) throws Exception {
        Method m = ProductionConfigValidator.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        try {
            m.invoke(validator);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof IllegalStateException ise) throw ise;
            throw e;
        }
    }

    // ── validateJwtSecretLength: jwtSecret == null (null branch, dead via validate()) ──

    @Test
    void validateJwtSecretLength_withNullJwtSecret_throws() {
        when(env.getProperty("JWT_SECRET")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> invokePrivate("validateJwtSecretLength"));
    }

    // ── validateMailConfiguration: isBlank() TRUE branches ───────────────────

    @Test
    void validateMailConfiguration_withBlankMailHost_throws() {
        when(env.getProperty("MAIL_HOST")).thenReturn("  ");
        when(env.getProperty("MAIL_PORT")).thenReturn("587");
        when(env.getProperty("MAIL_USERNAME")).thenReturn("user@example.com");
        when(env.getProperty("MAIL_PASSWORD")).thenReturn("pass");
        assertThrows(IllegalStateException.class, () -> invokePrivate("validateMailConfiguration"));
    }

    @Test
    void validateMailConfiguration_withBlankMailPort_throws() {
        when(env.getProperty("MAIL_HOST")).thenReturn("smtp.example.com");
        when(env.getProperty("MAIL_PORT")).thenReturn("  ");
        when(env.getProperty("MAIL_USERNAME")).thenReturn("user@example.com");
        when(env.getProperty("MAIL_PASSWORD")).thenReturn("pass");
        assertThrows(IllegalStateException.class, () -> invokePrivate("validateMailConfiguration"));
    }

    @Test
    void validateMailConfiguration_withBlankMailUsername_throws() {
        when(env.getProperty("MAIL_HOST")).thenReturn("smtp.example.com");
        when(env.getProperty("MAIL_PORT")).thenReturn("587");
        when(env.getProperty("MAIL_USERNAME")).thenReturn("  ");
        when(env.getProperty("MAIL_PASSWORD")).thenReturn("pass");
        assertThrows(IllegalStateException.class, () -> invokePrivate("validateMailConfiguration"));
    }

    @Test
    void validateMailConfiguration_withBlankMailPassword_throws() {
        when(env.getProperty("MAIL_HOST")).thenReturn("smtp.example.com");
        when(env.getProperty("MAIL_PORT")).thenReturn("587");
        when(env.getProperty("MAIL_USERNAME")).thenReturn("user@example.com");
        when(env.getProperty("MAIL_PASSWORD")).thenReturn("  ");
        assertThrows(IllegalStateException.class, () -> invokePrivate("validateMailConfiguration"));
    }

    // ── validateSecretTerm: isBlank() TRUE branch ─────────────────────────────

    @Test
    void validateSecretTerm_withBlankSecretTerm_throws() {
        when(env.getProperty("SECRET_TERM")).thenReturn("   ");
        assertThrows(IllegalStateException.class, () -> invokePrivate("validateSecretTerm"));
    }

    // ── validateCorsConfiguration: isBlank() TRUE branch ─────────────────────

    @Test
    void validateCorsConfiguration_withBlankFrontendAllowedOrigins_throws() {
        when(env.getProperty("FRONTEND_ALLOWED_ORIGINS")).thenReturn("  ");
        assertThrows(IllegalStateException.class, () -> invokePrivate("validateCorsConfiguration"));
    }
}
