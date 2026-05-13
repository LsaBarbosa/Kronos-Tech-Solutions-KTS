package com.kts.kronos.application.security;

import com.kts.kronos.application.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationRateLimitServiceTest {

    private MockHttpServletRequest request;
    private AuthenticationRateLimitService service;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.10");
        service = new AuthenticationRateLimitService(request, new ClientIpResolver());
        ReflectionTestUtils.setField(service, "loginIpLimit", 100);
        ReflectionTestUtils.setField(service, "loginIpWindowSeconds", 60);
        ReflectionTestUtils.setField(service, "loginUsernameLimit", 5);
        ReflectionTestUtils.setField(service, "loginUsernameWindowSeconds", 300);
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", "5,15,30");
        ReflectionTestUtils.setField(service, "recoveryCpfLimit", 3);
        ReflectionTestUtils.setField(service, "recoveryEmailLimit", 3);
        ReflectionTestUtils.setField(service, "recoveryIpLimit", 10);
        ReflectionTestUtils.setField(service, "recoveryWindowSeconds", 3600);
    }

    @Test
    void shouldBlockUsernameAfterFiveFailedLogins() {
        service.onLoginFailure("Alice@KTS.com");
        service.onLoginFailure("alice@kts.com");
        service.onLoginFailure("ALICE@KTS.COM");
        service.onLoginFailure(" alice@kts.com ");

        assertThrows(TooManyRequestsException.class, () -> service.onLoginFailure("alice@kts.com"));
        assertThrows(TooManyRequestsException.class, () -> service.checkLoginAllowed("alice@kts.com"));
    }

    @Test
    void shouldLimitLoginAttemptsByIp() {
        ReflectionTestUtils.setField(service, "loginIpLimit", 2);

        service.checkLoginAllowed("alice");
        service.checkLoginAllowed("bob");

        assertThrows(TooManyRequestsException.class, () -> service.checkLoginAllowed("carol"));
    }

    @Test
    void shouldClearUsernameFailuresAfterSuccessfulLogin() {
        service.onLoginFailure("alice");
        service.onLoginFailure("alice");
        service.onLoginFailure("alice");
        service.onLoginFailure("alice");

        service.onLoginSuccess("alice");

        assertDoesNotThrow(() -> service.checkLoginAllowed("alice"));
    }

    @Test
    void shouldLimitPasswordRecoveryByCpfAndKeepIpIndependent() {
        service.checkPasswordRecoveryAllowed("123.456.789-01", "user@kts.com");
        service.checkPasswordRecoveryAllowed("12345678901", "other@kts.com");
        service.checkPasswordRecoveryAllowed("12345678901", "third@kts.com");

        assertThrows(TooManyRequestsException.class,
                () -> service.checkPasswordRecoveryAllowed("12345678901", "fourth@kts.com"));
    }

    @Test
    void shouldLimitPasswordRecoveryByIp() {
        ReflectionTestUtils.setField(service, "recoveryCpfLimit", 100);
        ReflectionTestUtils.setField(service, "recoveryEmailLimit", 100);
        ReflectionTestUtils.setField(service, "recoveryIpLimit", 2);

        service.checkPasswordRecoveryAllowed("11111111111", "one@kts.com");
        service.checkPasswordRecoveryAllowed("22222222222", "two@kts.com");

        assertThrows(TooManyRequestsException.class,
                () -> service.checkPasswordRecoveryAllowed("33333333333", "three@kts.com"));
    }
}
