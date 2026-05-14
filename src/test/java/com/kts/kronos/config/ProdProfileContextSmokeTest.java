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
        "spring.mail.username=test@kronos.local",
        "management.health.mail.enabled=false",
        "frontend.base-url-record=https://record.kronos.example",
        "frontend.base-url-plataform=https://app.kronos.example",
        "frontend.base-url-local=https://local.kronos.example",
        "frontend.base-url-local-2=https://local2.kronos.example",
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
