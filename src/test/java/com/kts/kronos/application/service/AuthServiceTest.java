package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.adapter.out.security.PasswordPolicyValidator;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.out.provider.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authManager;
    @Mock JwtUtils jwtUtils;
    @Mock UserProvider userProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock PasswordResetTokenProvider tokenProvider;
    @Mock EmailSenderProvider emailSenderProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock FaceRecognitionProvider faceRecognitionProvider;
    @Mock DocumentProvider documentProvider;
    @Mock PasswordPolicyValidator passwordPolicyValidator;

    @Test
    void loginFaceThrowsBadRequestWhenBase64IsInvalid() {
        AuthService service = new AuthService(
                authManager,
                jwtUtils,
                userProvider,
                employeeProvider,
                tokenProvider,
                emailSenderProvider,
                passwordEncoder,
                faceRecognitionProvider,
                documentProvider,
                new SimpleMeterRegistry(),
                passwordPolicyValidator
        );

        assertThrows(BadRequestException.class, () -> service.loginFace("not-base64$$$"));
    }
}
