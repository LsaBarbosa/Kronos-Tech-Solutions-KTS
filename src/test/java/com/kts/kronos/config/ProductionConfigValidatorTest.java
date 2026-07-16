package com.kts.kronos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductionConfigValidator Unit Tests")
class ProductionConfigValidatorTest {

    private Environment env;
    private ProductionConfigValidator validator;

    @BeforeEach
    void setUp() {
        env = mock(Environment.class);
        validator = new ProductionConfigValidator(env);
    }

    private void stubAllRequired() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(env.getProperty("DB_HOST")).thenReturn("db-host");
        when(env.getProperty("DB_PORT")).thenReturn("5432");
        when(env.getProperty("DB_NAME")).thenReturn("kronos");
        when(env.getProperty("DB_USERNAME")).thenReturn("user");
        when(env.getProperty("DB_PASSWORD")).thenReturn("pass");
        when(env.getProperty("JWT_SECRET")).thenReturn("a".repeat(64));
        when(env.getProperty("SECRET_TERM")).thenReturn("a".repeat(32));
        when(env.getProperty("FRONTEND_BASE_URL_PLATAFORM")).thenReturn("https://app.example.com");
        when(env.getProperty("FRONTEND_BASE_URL_RECORD")).thenReturn("https://record.example.com");
        when(env.getProperty("FRONTEND_ALLOWED_ORIGINS")).thenReturn("https://app.example.com");
        when(env.getProperty("AWS_REGION")).thenReturn("us-east-1");
        when(env.getProperty("AWS_ACCESS_KEY_ID")).thenReturn("AKID");
        when(env.getProperty("AWS_SECRET_ACCESS_KEY")).thenReturn("secret");
        when(env.getProperty("AWS_S3_BUCKET_NAME")).thenReturn("bucket");
        when(env.getProperty("AWS_REKOGNITION_COLLECTION_ID")).thenReturn("collection");
        when(env.getProperty("MAIL_HOST")).thenReturn("smtp.example.com");
        when(env.getProperty("MAIL_PORT")).thenReturn("587");
        when(env.getProperty("MAIL_USERNAME")).thenReturn("user@example.com");
        when(env.getProperty("MAIL_PASSWORD")).thenReturn("mailpass");
        when(env.getProperty("JPA_DDL_AUTO", "validate")).thenReturn("validate");
        when(env.getProperty("AUTH_COOKIE_SECURE", "true")).thenReturn("true");
    }

    @Test
    void shouldSkipValidationWhenNotProdProfile() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});
        assertDoesNotThrow(() -> validator.validate());
        verify(env, never()).getProperty(any());
    }

    @Test
    void shouldSkipValidationWhenNoActiveProfiles() {
        when(env.getActiveProfiles()).thenReturn(new String[]{});
        assertDoesNotThrow(() -> validator.validate());
    }

    @Test
    void shouldPassWhenAllVariablesAreSet() {
        stubAllRequired();
        assertDoesNotThrow(() -> validator.validate());
    }

    @Test
    void shouldThrowWhenDbHostMissing() {
        stubAllRequired();
        when(env.getProperty("DB_HOST")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenDbPortBlank() {
        stubAllRequired();
        when(env.getProperty("DB_PORT")).thenReturn("  ");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenDbNameMissing() {
        stubAllRequired();
        when(env.getProperty("DB_NAME")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenDbUsernameMissing() {
        stubAllRequired();
        when(env.getProperty("DB_USERNAME")).thenReturn("");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenDbPasswordMissing() {
        stubAllRequired();
        when(env.getProperty("DB_PASSWORD")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenJwtSecretMissing() {
        stubAllRequired();
        when(env.getProperty("JWT_SECRET")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenJwtSecretTooShort() {
        stubAllRequired();
        when(env.getProperty("JWT_SECRET")).thenReturn("x".repeat(63));
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenSecretTermMissing() {
        stubAllRequired();
        when(env.getProperty("SECRET_TERM")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenSecretTermTooShort() {
        stubAllRequired();
        when(env.getProperty("SECRET_TERM")).thenReturn("short-term");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenSecretTermIsChangeMePlaceholder() {
        stubAllRequired();
        when(env.getProperty("SECRET_TERM")).thenReturn("change-me");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenSecretTermIsLongChangeMePlaceholder() {
        stubAllRequired();
        when(env.getProperty("SECRET_TERM")).thenReturn("change-me-use-a-strong-random-value");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenSecretTermIsBiometricSaltPlaceholder() {
        stubAllRequired();
        when(env.getProperty("SECRET_TERM")).thenReturn("change-me-biometric-term-salt");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenSecretTermIsSecret() {
        stubAllRequired();
        when(env.getProperty("SECRET_TERM")).thenReturn("secret");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenSecretTermIsTeste() {
        stubAllRequired();
        when(env.getProperty("SECRET_TERM")).thenReturn("teste");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenSecretTermIs123456() {
        stubAllRequired();
        when(env.getProperty("SECRET_TERM")).thenReturn("123456");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenFrontendBaseUrlPlataformMissing() {
        stubAllRequired();
        when(env.getProperty("FRONTEND_BASE_URL_PLATAFORM")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenFrontendBaseUrlRecordMissing() {
        stubAllRequired();
        when(env.getProperty("FRONTEND_BASE_URL_RECORD")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenFrontendAllowedOriginsMissing() {
        stubAllRequired();
        when(env.getProperty("FRONTEND_ALLOWED_ORIGINS")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenFrontendAllowedOriginsHasWildcard() {
        stubAllRequired();
        when(env.getProperty("FRONTEND_ALLOWED_ORIGINS")).thenReturn("https://app.example.com,*");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenAwsRegionMissing() {
        stubAllRequired();
        when(env.getProperty("AWS_REGION")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenAwsAccessKeyMissing() {
        stubAllRequired();
        when(env.getProperty("AWS_ACCESS_KEY_ID")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenAwsSecretKeyMissing() {
        stubAllRequired();
        when(env.getProperty("AWS_SECRET_ACCESS_KEY")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenS3BucketMissing() {
        stubAllRequired();
        when(env.getProperty("AWS_S3_BUCKET_NAME")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenRekognitionCollectionMissing() {
        stubAllRequired();
        when(env.getProperty("AWS_REKOGNITION_COLLECTION_ID")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenMailHostMissing() {
        stubAllRequired();
        when(env.getProperty("MAIL_HOST")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenMailPortMissing() {
        stubAllRequired();
        when(env.getProperty("MAIL_PORT")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenMailUsernameMissing() {
        stubAllRequired();
        when(env.getProperty("MAIL_USERNAME")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenMailPasswordMissing() {
        stubAllRequired();
        when(env.getProperty("MAIL_PASSWORD")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenJpaDdlAutoIsCreate() {
        stubAllRequired();
        when(env.getProperty("JPA_DDL_AUTO", "validate")).thenReturn("create");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenJpaDdlAutoIsCreateDrop() {
        stubAllRequired();
        when(env.getProperty("JPA_DDL_AUTO", "validate")).thenReturn("create-drop");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenJpaDdlAutoIsCreateUppercase() {
        stubAllRequired();
        when(env.getProperty("JPA_DDL_AUTO", "validate")).thenReturn("CREATE");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldThrowWhenJpaDdlAutoIsUpdate() {
        stubAllRequired();
        when(env.getProperty("JPA_DDL_AUTO", "validate")).thenReturn("update");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldAllowJpaDdlAutoNone() {
        stubAllRequired();
        when(env.getProperty("JPA_DDL_AUTO", "validate")).thenReturn("none");
        assertDoesNotThrow(() -> validator.validate());
    }

    @Test
    void shouldThrowWhenCookieSecureIsFalse() {
        stubAllRequired();
        when(env.getProperty("AUTH_COOKIE_SECURE", "true")).thenReturn("false");
        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    void shouldAllowCookieSecureTrueUppercase() {
        stubAllRequired();
        when(env.getProperty("AUTH_COOKIE_SECURE", "true")).thenReturn("TRUE");
        assertDoesNotThrow(() -> validator.validate());
    }
}
