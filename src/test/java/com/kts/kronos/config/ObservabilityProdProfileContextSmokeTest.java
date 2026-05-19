package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.observability.adapter.in.web.CorrelationIdFilter;
import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import testsupport.ContextSmokeTestApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ContextSmokeTestApplication.class)
@ActiveProfiles({"prod", "observability"})
@TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=kronos_test",
        "DB_USERNAME=kronos_test",
        "DB_PASSWORD=kronos_test",
        "JWT_SECRET=test-jwt-secret-with-at-least-64-characters-1234567890123456",
        "SECRET_TERM=test-secret-term-with-at-least-32-characters-1234567890",
        "FRONTEND_BASE_URL_RECORD=https://record.kronos.example",
        "FRONTEND_BASE_URL_PLATAFORM=https://app.kronos.example",
        "FRONTEND_BASE_URL_LOCAL=https://local.kronos.example",
        "FRONTEND_BASE_URL_LOCAL_2=https://local2.kronos.example",
        "FRONTEND_ALLOWED_ORIGINS=https://kronos.example",
        "MAIL_HOST=localhost",
        "MAIL_PORT=2525",
        "MAIL_USERNAME=test@kronos.local",
        "MAIL_PASSWORD=test",
        "management.health.mail.enabled=false",
        "AWS_REGION=us-east-1",
        "AWS_ACCESS_KEY_ID=test-key",
        "AWS_SECRET_ACCESS_KEY=test-secret",
        "AWS_S3_BUCKET_NAME=test-bucket",
        "AWS_S3_BUCKET_NAME_DOCS=test-doc-bucket",
        "AWS_REKOGNITION_COLLECTION_ID=test-collection",
        "DIGITAL_CERTIFICATE_PATH=/tmp/certificado.pfx",
        "DIGITAL_CERTIFICATE_PASSWORD=change-me"
})
class ObservabilityProdProfileContextSmokeTest {

    @Autowired
    private Environment environment;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private ObservabilityStatusUseCase observabilityStatusUseCase;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

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

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void shouldStartWithProdObservabilityProfile() {
        assertNotNull(healthEndpoint);
        assertNotNull(observabilityStatusUseCase);
        assertNotNull(correlationIdFilter);
        assertEquals("8081", environment.getProperty("management.server.port"));
        assertEquals("127.0.0.1", environment.getProperty("management.server.address"));
        assertEquals("never", environment.getProperty("management.endpoint.health.show-details"));
        assertEquals("0.1", environment.getProperty("management.tracing.sampling.probability"));
    }
}
