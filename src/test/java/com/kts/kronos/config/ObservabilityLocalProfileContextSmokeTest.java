package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.observability.web.CorrelationIdFilter;
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
@ActiveProfiles({"local", "observability"})
@TestPropertySource(properties = {
        "management.health.mail.enabled=false",
        "FLYWAY_ENABLED=false"
})
class ObservabilityLocalProfileContextSmokeTest {

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
    void shouldStartWithLocalObservabilityProfile() {
        assertNotNull(healthEndpoint);
        assertNotNull(observabilityStatusUseCase);
        assertNotNull(correlationIdFilter);
        assertEquals("8081", environment.getProperty("management.server.port"));
        assertEquals("127.0.0.1", environment.getProperty("management.server.address"));
        assertEquals("never", environment.getProperty("management.endpoint.health.show-details"));
        assertEquals("health,info,metrics,prometheus", environment.getProperty("management.endpoints.web.exposure.include"));
    }
}
