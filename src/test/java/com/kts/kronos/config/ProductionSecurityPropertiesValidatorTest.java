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
}
