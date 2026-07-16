package com.kts.kronos.application.security;

import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationRateLimitServiceTest {

    private MockHttpServletRequest request;
    private AuthenticationRateLimitService service;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.10");
        ClientIpResolverProperties properties = new ClientIpResolverProperties();
        service = new AuthenticationRateLimitService(request, new ClientIpResolver(properties));
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
    void shouldExpireLoginIpWindowAfterTheConfiguredTTL() throws Exception {
        ReflectionTestUtils.setField(service, "loginIpLimit", 1);
        ReflectionTestUtils.setField(service, "loginIpWindowSeconds", 1);

        service.checkLoginAllowed("alice");

        assertThrows(TooManyRequestsException.class, () -> service.checkLoginAllowed("bob"));

        TimeUnit.MILLISECONDS.sleep(1100);

        assertDoesNotThrow(() -> service.checkLoginAllowed("carol"));
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

    @Test
    void shouldLimitAdminSearchRateLimitByIp() {
        ReflectionTestUtils.setField(service, "adminCheckLimit", 2);
        ReflectionTestUtils.setField(service, "adminCheckWindowSeconds", 60);

        service.checkAdminSearchRateLimit();
        service.checkAdminSearchRateLimit();

        assertThrows(TooManyRequestsException.class, () -> service.checkAdminSearchRateLimit());
    }

    @Test
    void shouldAllowAdminSearchWithinLimit() {
        ReflectionTestUtils.setField(service, "adminCheckLimit", 5);
        ReflectionTestUtils.setField(service, "adminCheckWindowSeconds", 60);

        assertDoesNotThrow(() -> service.checkAdminSearchRateLimit());
    }

    @Test
    void shouldUseRateLimitStoreForLoginCheckWhenPresent() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_LOGIN_IP),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(1L);
        org.mockito.Mockito.when(rateLimitStore.isCoolingDown(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_LOGIN_USERNAME),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(false);

        assertDoesNotThrow(() -> service.checkLoginAllowed("alice@kts.com"));
    }

    @Test
    void shouldBlockLoginByIpViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_LOGIN_IP),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn((long)(loginIpLimit() + 1));

        assertThrows(TooManyRequestsException.class, () -> service.checkLoginAllowed("alice@kts.com"));
    }

    @Test
    void shouldBlockLoginByUsernameCooldownViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(1L);
        org.mockito.Mockito.when(rateLimitStore.isCoolingDown(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_LOGIN_USERNAME),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(true);

        assertThrows(TooManyRequestsException.class, () -> service.checkLoginAllowed("blocked@kts.com"));
    }

    @Test
    void shouldRecordLoginFailureAndBlockViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_LOGIN_USERNAME),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(5L);
        org.mockito.Mockito.when(rateLimitStore.incrementPenalty(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(1L);

        assertThrows(TooManyRequestsException.class, () -> service.onLoginFailure("alice@kts.com"));
    }

    @Test
    void shouldRecordLoginFailureWithinLimitViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(2L);

        assertDoesNotThrow(() -> service.onLoginFailure("alice@kts.com"));
    }

    @Test
    void shouldResetUsernameCooldownOnLoginSuccessViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);

        assertDoesNotThrow(() -> service.onLoginSuccess("alice@kts.com"));
        org.mockito.Mockito.verify(rateLimitStore).reset(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_LOGIN_USERNAME),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldBlockAdminSearchViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        ReflectionTestUtils.setField(service, "adminCheckLimit", 5);
        ReflectionTestUtils.setField(service, "adminCheckWindowSeconds", 60);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(6L);

        assertThrows(TooManyRequestsException.class, () -> service.checkAdminSearchRateLimit());
    }

    @Test
    void shouldAllowAdminSearchViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        ReflectionTestUtils.setField(service, "adminCheckLimit", 5);
        ReflectionTestUtils.setField(service, "adminCheckWindowSeconds", 60);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(3L);

        assertDoesNotThrow(() -> service.checkAdminSearchRateLimit());
    }

    @Test
    void shouldBlockPasswordRecoveryByCpfViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        // IP returns within limit, CPF returns over limit
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_IP),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(1L);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_CPF),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(4L);

        assertThrows(TooManyRequestsException.class,
                () -> service.checkPasswordRecoveryAllowed("12345678901", "test@kts.com"));
    }

    @Test
    void shouldBlockPasswordRecoveryByEmailViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_IP),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(1L);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_CPF),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(1L);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_EMAIL),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(4L);

        assertThrows(TooManyRequestsException.class,
                () -> service.checkPasswordRecoveryAllowed("12345678901", "test@kts.com"));
    }

    @Test
    void shouldAllowPasswordRecoveryViaRateLimitStore() {
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(1L);

        assertDoesNotThrow(() -> service.checkPasswordRecoveryAllowed("12345678901", "test@kts.com"));
    }

    @Test
    void shouldHandleCooldownsParsingFailure() {
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", "not_a_number");
        // Should not throw - falls back to default cooldowns
        assertDoesNotThrow(() -> service.onLoginFailure("newuser@kts.com"));
    }

    @Test
    void shouldHandleEmptyCooldownsConfiguration() {
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", "");
        assertDoesNotThrow(() -> service.onLoginFailure("newuser@kts.com"));
    }

    @Test
    void shouldUsePrivacyLogReferenceServiceWhenPresent() {
        // safeClientIpRef() is called only in the rateLimitStore path
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        var privacyLogRef = org.mockito.Mockito.mock(com.kts.kronos.application.security.PrivacyLogReferenceService.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        ReflectionTestUtils.setField(service, "privacyLogReferenceService", privacyLogRef);
        ReflectionTestUtils.setField(service, "loginIpLimit", 5);
        org.mockito.Mockito.when(privacyLogRef.genericRef(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn("ref-1234");
        // IP count exceeds limit → calls safeClientIpRef()
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_LOGIN_IP),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(6L);

        assertThrows(TooManyRequestsException.class, () -> service.checkLoginAllowed("alice"));
        org.mockito.Mockito.verify(privacyLogRef, org.mockito.Mockito.atLeastOnce())
                .genericRef(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldCheckPasswordRecoveryWithNullCpfAndEmail() {
        // null cpf and null email → only IP check runs
        assertDoesNotThrow(() -> service.checkPasswordRecoveryAllowed(null, null));
    }

    @Test
    void shouldBlockImmediatelyWhenIpLimitIsZero() {
        ReflectionTestUtils.setField(service, "loginIpLimit", 0);
        assertThrows(TooManyRequestsException.class, () -> service.checkLoginAllowed("alice"));
    }

    @Test
    void shouldHandleCooldownsWithZeroValues() {
        // "0,15,30" → filter removes 0 → [15, 30] → still valid
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", "0,15,30");
        assertDoesNotThrow(() -> service.onLoginFailure("newuser@kts.com"));
    }

    @Test
    void shouldUseDefaultCooldownsWhenAllFiltered() {
        // "0,0,0" → all filtered out → uses default {5, 15, 30}
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", "0,0,0");
        assertDoesNotThrow(() -> service.onLoginFailure("newuser2@kts.com"));
    }

    @Test
    void shouldHandlePasswordRecoveryWithPrivacyLogRefPresent() {
        // safeClientIpRef() is called only in the rateLimitStore path
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        var privacyLogRef = org.mockito.Mockito.mock(com.kts.kronos.application.security.PrivacyLogReferenceService.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        ReflectionTestUtils.setField(service, "privacyLogReferenceService", privacyLogRef);
        org.mockito.Mockito.when(privacyLogRef.genericRef(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn("ip-ref");
        // IP count exceeds → log.warn calls safeClientIpRef()
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_IP),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(99L);
        ReflectionTestUtils.setField(service, "recoveryIpLimit", 10);

        assertThrows(TooManyRequestsException.class,
                () -> service.checkPasswordRecoveryAllowed("12345678901", "test@example.com"));
        org.mockito.Mockito.verify(privacyLogRef, org.mockito.Mockito.atLeastOnce())
                .genericRef(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldUsePrivacyLogRefForUsernameOnLoginFailure() {
        // safeUsernameRef() is called only in the rateLimitStore path
        var rateLimitStore = org.mockito.Mockito.mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        var privacyLogRef = org.mockito.Mockito.mock(com.kts.kronos.application.security.PrivacyLogReferenceService.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        ReflectionTestUtils.setField(service, "privacyLogReferenceService", privacyLogRef);
        org.mockito.Mockito.when(privacyLogRef.genericRef(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn("user-ref");
        // attempts >= limit → calls safeUsernameRef()
        org.mockito.Mockito.when(rateLimitStore.increment(
                org.mockito.ArgumentMatchers.eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_LOGIN_USERNAME),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(5L);
        org.mockito.Mockito.when(rateLimitStore.incrementPenalty(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(1L);
        ReflectionTestUtils.setField(service, "loginUsernameLimit", 5);

        assertThrows(TooManyRequestsException.class, () -> service.onLoginFailure("blocked@kts.com"));
        org.mockito.Mockito.verify(privacyLogRef, org.mockito.Mockito.atLeastOnce())
                .genericRef(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private int loginIpLimit() {
        return (Integer) ReflectionTestUtils.getField(service, "loginIpLimit");
    }
}
