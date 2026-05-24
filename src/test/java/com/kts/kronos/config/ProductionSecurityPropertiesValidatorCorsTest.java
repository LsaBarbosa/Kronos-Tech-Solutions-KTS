package com.kts.kronos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.core.env.Environment;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductionSecurityPropertiesValidator CORS Tests")
public class ProductionSecurityPropertiesValidatorCorsTest {

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
    @DisplayName("1. Single HTTPS origin passes in prod")
    void testSingleHttpsOriginPasses() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("2. Multiple HTTPS origins pass in prod")
    void testMultipleHttpsOriginsPasses() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com,https://www.kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("3. Subdomain HTTPS origin passes in prod")
    void testSubdomainHttpsOriginPasses() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://app.kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("4. HTTPS origin with port passes in prod")
    void testHttpsOriginWithPortPasses() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com:8443");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("5. Empty origin fails in prod")
    void testEmptyOriginFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("6. Wildcard * fails in prod")
    void testWildcardFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "*");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("7. Partial wildcard *.domain fails in prod")
    void testPartialWildcardFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "*.kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("8. HTTP origin fails in prod")
    void testHttpOriginFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "http://kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("9. Origin without scheme fails in prod")
    void testOriginWithoutSchemeFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("10. Origin with path fails in prod")
    void testOriginWithPathFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com/path");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("11. Origin with query string fails in prod")
    void testOriginWithQueryFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com?x=1");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("12. Origin with fragment fails in prod")
    void testOriginWithFragmentFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com#frag");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("13. Origin with space fails in prod")
    void testOriginWithSpaceFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://krono tech.com");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("14. Multiple origins with one invalid fails in prod")
    void testMultipleOriginsWithOneInvalidFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com,http://localhost:3000");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("15. Origin with empty value in list fails in prod")
    void testOriginWithEmptyValueInListFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://kronostechsolutions.com,,https://www.kronostechsolutions.com");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("16. Malformed URL fails in prod")
    void testMalformedUrlFailsInProd() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", "https://[invalid");
        setField(validator, "swaggerEnabled", false);

        assertThrows(IllegalStateException.class, () -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("17. HTTP localhost passes in dev profile")
    void testHttpLocalhostPassesInDev() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(environment.getProperty("jwt.secret")).thenReturn("short");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", false);
        setField(validator, "cookieHttpOnly", false);
        setField(validator, "corsAllowedOrigins", "http://localhost:5173");
        setField(validator, "swaggerEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("18. HTTP localhost:3000 passes in dev profile")
    void testHttpLocalhost3000PassesInDev() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "corsAllowedOrigins", "http://localhost:3000");

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("19. HTTP 127.0.0.1:5173 passes in dev profile")
    void testHttp127PassesInDev() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "corsAllowedOrigins", "http://127.0.0.1:5173");

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }

    @Test
    @DisplayName("20. Multiple origins with whitespace padding passes in prod")
    void testMultipleOriginsWithWhitespacePasses() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("jwt.secret")).thenReturn("this-is-a-very-secure-secret-with-32-characters");
        when(environment.getProperty("management.endpoints.web.exposure.include", "")).thenReturn("health,info");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);
        setField(validator, "cookieSecure", true);
        setField(validator, "cookieHttpOnly", true);
        setField(validator, "corsAllowedOrigins", " https://kronostechsolutions.com , https://www.kronostechsolutions.com ");
        setField(validator, "swaggerEnabled", false);
        setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(() -> validator.validateProductionConfiguration());
    }
}
