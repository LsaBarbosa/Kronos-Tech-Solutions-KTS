package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import testsupport.ContextSmokeTestApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ContextSmokeTestApplication.class)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        // Database
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=kronos_test",
        "DB_USERNAME=kronos_test",
        "DB_PASSWORD=kronos_test",
        // JWT
        "JWT_SECRET=test-jwt-secret-with-at-least-64-characters-1234567890123456",
        // Biometric
        "SECRET_TERM=test-secret-term-with-at-least-32-characters-1234567890",
        // Frontend
        "frontend.base-url-record=https://record.kronos.example",
        "frontend.base-url-plataform=https://app.kronos.example",
        "frontend.base-url-local=https://local.kronos.example",
        "frontend.base-url-local-2=https://local2.kronos.example",
        "FRONTEND_ALLOWED_ORIGINS=https://kronos.example",
        // Mail
        "MAIL_HOST=localhost",
        "MAIL_PORT=2525",
        "MAIL_USERNAME=test@kronos.local",
        "MAIL_PASSWORD=test",
        "management.health.mail.enabled=false",
        // AWS
        "AWS_REGION=us-east-1",
        "AWS_ACCESS_KEY_ID=test-key",
        "AWS_SECRET_ACCESS_KEY=test-secret",
        "AWS_S3_BUCKET_NAME=test-bucket",
        "AWS_REKOGNITION_COLLECTION_ID=test-collection",
        // Certificate
        "DIGITAL_CERTIFICATE_PATH=/tmp/certificado.pfx",
        "DIGITAL_CERTIFICATE_PASSWORD=change-me"
})
class ProdProfileContextSmokeTest {

    @Autowired
    private Environment environment;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;

    @MockitoBean
    private FaceRecognitionProvider faceRecognitionProvider;

    @MockitoBean
    private TokenBlacklistProvider tokenBlacklistProvider;

    @Test
    void shouldStartWithProdProfileAndSecureProductionProperties() {
        assertNotNull(securityFilterChain);
        assertEquals("framework", environment.getProperty("server.forward-headers-strategy"));
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", environment.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("false", environment.getProperty("springdoc.api-docs.enabled"));
        assertEquals("false", environment.getProperty("biometric.liveness-required"));
        assertEquals("INFO", environment.getProperty("logging.level.org.springframework.security"));
    }
}
