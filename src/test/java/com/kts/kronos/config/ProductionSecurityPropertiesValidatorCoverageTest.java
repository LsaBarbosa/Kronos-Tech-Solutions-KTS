package com.kts.kronos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers remaining branches in ProductionSecurityPropertiesValidator:
 * - validateCORS: !isProduction TRUE early-return branch (L74, L75, L76)
 * - validateOriginFormat: url.getHost().isEmpty() TRUE branch (L149, L150-L152)
 * - validateAwsCredentials: awsAccessKey.isEmpty() and awsSecretKey.isEmpty() branches (L197, L198)
 * - validateAwsCredentials: hasAccessKey=true, hasSecretKey=false reaching else-if (L200, L202)
 */
class ProductionSecurityPropertiesValidatorCoverageTest {

    private Environment environment;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private void invokePrivate(Object obj, String methodName, Class<?>[] paramTypes, Object... args) throws Throwable {
        Method m = obj.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        try {
            m.invoke(obj, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    // ── validateCORS: !isProduction TRUE → early return (L74 TRUE, L75, L76) ──

    @Test
    void validateCORS_whenNotProduction_logsAndReturns() throws Exception {
        // Create validator with isProduction=false (no "prod" in active profiles)
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        var validator = new ProductionSecurityPropertiesValidator(environment);
        // validator.isProduction == false

        // Calling validateCORS() directly: hits the `if (!isProduction)` TRUE branch → return
        assertDoesNotThrow(() -> invokePrivate(validator, "validateCORS", new Class<?>[]{}));
    }

    // ── validateOriginFormat: url.getHost().isEmpty() TRUE branch (L149, L150-L152) ──

    @Test
    void validateOriginFormat_withEmptyHost_throwsIllegalState() throws Throwable {
        // "https:///" parses OK but getHost() returns "" → isEmpty() = true → throw
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        var validator = new ProductionSecurityPropertiesValidator(environment);

        assertThrows(IllegalStateException.class,
                () -> invokePrivate(validator, "validateOriginFormat", new Class[]{String.class}, "https:///"));
    }

    // ── validateAwsCredentials: awsAccessKey="" (isEmpty branch, L197) ────────

    @Test
    void validateAwsCredentials_withEmptyAccessKeyAndEmptySecretKey_iamMode() throws Throwable {
        // awsAccessKey="" → !isEmpty=false → hasAccessKey=false
        // awsSecretKey="" → !isEmpty=false → hasSecretKey=false
        // → IAM role mode → no throw
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("aws.access-key-id")).thenReturn("");
        when(environment.getProperty("aws.secret-access-key")).thenReturn("");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        var validator = new ProductionSecurityPropertiesValidator(environment);

        assertDoesNotThrow(() -> invokePrivate(validator, "validateAwsCredentials", new Class<?>[]{}));
    }

    // ── validateAwsCredentials: hasAccessKey=true, hasSecretKey=false → throws (L200, L202) ──

    @Test
    void validateAwsCredentials_withAccessKeyOnlyNotNull_throws() throws Throwable {
        // awsAccessKey non-empty → hasAccessKey=true
        // awsSecretKey=null → hasSecretKey=false
        // if (true && false) → FALSE; else if (!true && !false) = (false && ...) → FALSE; → else → throw
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("aws.access-key-id")).thenReturn("AKIA123456789");
        when(environment.getProperty("aws.secret-access-key")).thenReturn(null);
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        var validator = new ProductionSecurityPropertiesValidator(environment);

        assertThrows(IllegalStateException.class,
                () -> invokePrivate(validator, "validateAwsCredentials", new Class<?>[]{}));
    }

    // ── Full chain tests that reach validateAwsCredentials via validateProductionConfiguration ─

    @Test
    void fullValidation_withAccessKeyOnlyViaPublicMethod_throws() throws Exception {
        // Covers L200 and L202 via the public validateProductionConfiguration() flow
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-32chars");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("aws.access-key-id")).thenReturn("AKIA123456789");
        when(environment.getProperty("aws.secret-access-key")).thenReturn(null);
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");

        var validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }
    // ── validateOriginFormat: url.getQuery() != null && isEmpty() TRUE → B_FALSE branch (L135) ──

    @Test
    void validateOriginFormat_withEmptyQueryString_noThrow() throws Throwable {
        // "https://example.com?" → getQuery() = "" (not null) → isEmpty() = TRUE → !isEmpty() = FALSE
        // → condition FALSE → no throw → covers L135 A_TRUE+B_FALSE branch
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        var validator = new ProductionSecurityPropertiesValidator(environment);

        // Should not throw: empty query string does not violate the policy
        assertDoesNotThrow(() -> invokePrivate(validator, "validateOriginFormat",
                new Class[]{String.class}, "https://example.com?"));
    }

    // ── validateOriginFormat: url.getRef() != null && isEmpty() TRUE → B_FALSE branch (L142) ──

    @Test
    void validateOriginFormat_withEmptyFragment_noThrow() throws Throwable {
        // "https://example.com#" → getRef() = "" (not null) → isEmpty() = TRUE → !isEmpty() = FALSE
        // → condition FALSE → no throw → covers L142 A_TRUE+B_FALSE branch
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        var validator = new ProductionSecurityPropertiesValidator(environment);

        assertDoesNotThrow(() -> invokePrivate(validator, "validateOriginFormat",
                new Class[]{String.class}, "https://example.com#"));
    }

}
