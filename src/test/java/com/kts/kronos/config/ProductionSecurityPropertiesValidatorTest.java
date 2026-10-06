package com.kts.kronos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.core.env.Environment;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductionSecurityPropertiesValidator Unit Tests")
public class ProductionSecurityPropertiesValidatorTest {

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

    @Test
    @DisplayName("1. Non-prod profile skips validation")
    void testNonProdProfileSkipsValidation() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("2. Prod with secure configuration passes")
    void testProdWithSecureConfig() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("frontend.allowed-origins", "*")).thenReturn("https://kronostechsolutions.com");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("aws.access-key-id")).thenReturn("test-key");
        when(environment.getProperty("aws.secret-access-key")).thenReturn("test-secret");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("3. Cookie secure false in prod fails")
    void testCookieSecureFalse() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("4. JWT secret absent in prod fails")
    void testJwtSecretAbsent() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn(null);
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("5. JWT secret too short in prod fails")
    void testJwtSecretTooShort() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("short");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("6. Swagger enabled in prod fails")
    void testSwaggerEnabled() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", true);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("7. API docs enabled in prod fails")
    void testApiDocsEnabled() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("8. Actuator env endpoint exposed fails")
    void testActuatorEnvEndpoint() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info,env");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("9. Actuator heapdump endpoint exposed fails")
    void testActuatorHeapdumpEndpoint() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info,heapdump");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("10. Actuator wildcard exposed fails")
    void testActuatorWildcard() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("*");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("11. AWS credentials absent warns but doesn't fail")
    void testAwsCredentialsAbsent() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);
        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("12. Antivirus disabled fails")
    void testAntivirusDisabled() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("13. Liveness false doesn't block")
    void testLivenessFalse() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);
        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("14. Actuator beans endpoint exposed fails")
    void testActuatorBeansEndpoint() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info,beans");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("15. Actuator configprops endpoint exposed fails")
    void testActuatorConfigpropsEndpoint() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info,configprops");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("16. Actuator threaddump endpoint exposed fails")
    void testActuatorThreaddumpEndpoint() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info,threaddump");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("17. Actuator flyway endpoint exposed fails")
    void testActuatorFlywayEndpoint() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info,flyway");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("18. Actuator logfile endpoint exposed fails")
    void testActuatorLogfileEndpoint() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info,logfile");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("19. Actuator loggers endpoint exposed fails")
    void testActuatorLoggersEndpoint() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info,loggers");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "swaggerEnabled", false);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("20. CORS wildcard fails")
    void testCorsWildcard() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "corsAllowedOrigins", "*");
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("21. AWS static credentials provided passes")
    void testAwsStaticCredentialsProvided() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        when(environment.getProperty("aws.access-key-id")).thenReturn("AKIA123456789");
        when(environment.getProperty("aws.secret-access-key")).thenReturn("wJalrXUtnFEMI/K7MDENG+41bIqoH8R5N7xwS9TX");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("22. AWS region missing fails")
    void testAwsRegionMissing() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("aws.region")).thenReturn(null);

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("23. AWS credentials incomplete (only access key) fails")
    void testAwsCredentialsIncomplete() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("aws.access-key-id")).thenReturn("AKIA123456789");
        when(environment.getProperty("aws.secret-access-key")).thenReturn(null);

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("24. AWS IAM Role mode (no credentials) passes")
    void testAwsIamRoleMode() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        when(environment.getProperty("aws.access-key-id")).thenReturn(null);
        when(environment.getProperty("aws.secret-access-key")).thenReturn(null);

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }
    // ── Tests that properly pass CORS to reach downstream validators ──────────

    private ProductionSecurityPropertiesValidator buildProdValidatorWithValidCors() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ProductionSecurityPropertiesValidator v = new ProductionSecurityPropertiesValidator(environment);
        setField(v, "cookieSecure", true);
        setField(v, "corsAllowedOrigins", "https://kronostechsolutions.com");
        return v;
    }

    @Test
    @DisplayName("25. Swagger enabled in prod fails (after CORS passes)")
    void testSwaggerEnabledAfterCorsValidation() throws Exception {
        ProductionSecurityPropertiesValidator validator = buildProdValidatorWithValidCors();
        setField(validator, "swaggerEnabled", true);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("26. JWT secret null in prod fails (after CORS passes)")
    void testJwtSecretNullAfterCorsValidation() throws Exception {
        ProductionSecurityPropertiesValidator validator = buildProdValidatorWithValidCors();
        setField(validator, "swaggerEnabled", false);
        when(environment.getProperty("jwt.secret")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("27. JWT secret too short in prod fails (after CORS passes)")
    void testJwtSecretShortAfterCorsValidation() throws Exception {
        ProductionSecurityPropertiesValidator validator = buildProdValidatorWithValidCors();
        setField(validator, "swaggerEnabled", false);
        when(environment.getProperty("jwt.secret")).thenReturn("tooshort");
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("28. AWS region null in prod fails (after CORS+JWT pass)")
    void testAwsRegionNullAfterCorsAndJwtValidation() throws Exception {
        ProductionSecurityPropertiesValidator validator = buildProdValidatorWithValidCors();
        setField(validator, "swaggerEnabled", false);
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-32chars");
        when(environment.getProperty("aws.region")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("29. AWS region empty in prod fails (after CORS+JWT pass)")
    void testAwsRegionEmptyAfterCorsAndJwtValidation() throws Exception {
        ProductionSecurityPropertiesValidator validator = buildProdValidatorWithValidCors();
        setField(validator, "swaggerEnabled", false);
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-32chars");
        when(environment.getProperty("aws.region")).thenReturn("");
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("30. AWS only secret key fails (after CORS+JWT+region pass)")
    void testAwsOnlySecretKeyAfterCorsJwtRegionValidation() throws Exception {
        ProductionSecurityPropertiesValidator validator = buildProdValidatorWithValidCors();
        setField(validator, "swaggerEnabled", false);
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-32chars");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("aws.access-key-id")).thenReturn(null);
        when(environment.getProperty("aws.secret-access-key")).thenReturn("some-secret-key");
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("31. Actuator env endpoint exposed fails (after CORS+JWT+AWS pass)")
    void testActuatorEnvExposedAfterFullChain() throws Exception {
        ProductionSecurityPropertiesValidator validator = buildProdValidatorWithValidCors();
        setField(validator, "swaggerEnabled", false);
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-32chars");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("aws.access-key-id")).thenReturn(null);
        when(environment.getProperty("aws.secret-access-key")).thenReturn(null);
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,env");
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("32. Antivirus disabled fails (after all prior validators pass)")
    void testAntivirusDisabledAfterFullChain() throws Exception {
        ProductionSecurityPropertiesValidator validator = buildProdValidatorWithValidCors();
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", false);
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-32chars");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("aws.access-key-id")).thenReturn(null);
        when(environment.getProperty("aws.secret-access-key")).thenReturn(null);
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("33. CORS origin with trailing slash passes validateOriginFormat path='/' branch")
    void testOriginWithTrailingSlashPasses() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-32chars");
        when(environment.getProperty("aws.region")).thenReturn("us-east-1");
        when(environment.getProperty("aws.access-key-id")).thenReturn(null);
        when(environment.getProperty("aws.secret-access-key")).thenReturn(null);
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com/");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);
        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }
}
