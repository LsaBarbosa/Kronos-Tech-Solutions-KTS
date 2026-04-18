package com.kts.kronos.application.security;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BiometricProtectionServiceTest {

    private BiometricProtectionService service;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.42");
        service = new BiometricProtectionService(request);
        ReflectionTestUtils.setField(service, "maxBase64Chars", 10);
        ReflectionTestUtils.setField(service, "livenessRequired", false);
        ReflectionTestUtils.setField(service, "loginFaceLimit", 2);
        ReflectionTestUtils.setField(service, "loginFaceWindowSeconds", 60);
        ReflectionTestUtils.setField(service, "checkinLimit", 2);
        ReflectionTestUtils.setField(service, "checkinWindowSeconds", 60);
        ReflectionTestUtils.setField(service, "enrollmentLimit", 2);
        ReflectionTestUtils.setField(service, "enrollmentWindowSeconds", 60);
    }

    @Test
    @DisplayName("protectPublicLogin: deve aplicar rate limit por IP")
    void shouldRateLimitPublicLogin() {
        service.protectPublicLogin("abc", null);
        service.protectPublicLogin("abc", null);

        assertThrows(TooManyRequestsException.class, () -> service.protectPublicLogin("abc", null));
    }

    @Test
    @DisplayName("protectEnrollment: deve rejeitar payload biométrico acima do limite")
    void shouldRejectOversizedBiometricPayload() {
        assertThrows(BadRequestException.class,
                () -> service.protectEnrollment(UUID.randomUUID(), "abcdefghijk"));
    }

    @Test
    @DisplayName("protectCheckIn: deve exigir liveness quando configurado")
    void shouldRequireLivenessWhenEnabled() {
        ReflectionTestUtils.setField(service, "livenessRequired", true);

        assertThrows(ForbiddenException.class,
                () -> service.protectCheckIn(UUID.randomUUID(), "abc", false));
    }
}
