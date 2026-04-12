package com.kts.kronos;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.out.persistence.impl.EmailSenderProviderImpl;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = KronosApplicationTests.TestApp.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.mail.username=test@kronos.local",
        "management.health.mail.enabled=false"
})
class KronosApplicationTests {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailSenderProvider emailSenderProvider;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender javaMailSenderMock;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;

    @MockitoBean
    private FaceRecognitionProvider faceRecognitionProvider;

    @Test
    void contextLoads() {
        assertNotNull(javaMailSender);
        assertNotNull(securityFilterChain);
    }

    @Test
    void shouldWireMailComponents() {
        assertNotNull(javaMailSender);
        assertNotNull(emailSenderProvider);
    }

    @Test
    void shouldWireSecurityComponents() {
        assertNotNull(securityFilterChain);
        assertNotNull(authenticationManager);
        assertNotNull(passwordEncoder);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, EmailSenderProviderImpl.class, RekognitionSetup.class})
    static class TestApp {
    }
}
