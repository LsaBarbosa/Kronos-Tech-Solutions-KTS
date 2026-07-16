package com.kts.kronos.application.security;

import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationRateLimitServiceCoverageTest {

    private MockHttpServletRequest request;
    private AuthenticationRateLimitService service;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");
        var properties = new ClientIpResolverProperties();
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
        ReflectionTestUtils.setField(service, "adminCheckLimit", 30);
        ReflectionTestUtils.setField(service, "adminCheckWindowSeconds", 60);
    }

    // ── normalize(null) → "unknown" via checkLoginAllowed(null) ──────────────

    @Test
    @DisplayName("normalize(null): checkLoginAllowed(null) → usernameKey(null) → normalize retorna 'unknown'")
    void checkLoginAllowed_nullUsername_normalizeCoveredWithNull() {
        // null username → usernameKey(null) → normalize(null) returns "unknown" → no exception
        assertDoesNotThrow(() -> service.checkLoginAllowed(null));
    }

    // ── checkLoginAllowed — state exists but blockedUntil null → L104-105 ────

    @Test
    @DisplayName("checkLoginAllowed: estado existe mas não está bloqueado → L104-105 alcançado")
    void checkLoginAllowed_stateExistsButNotBlocked_reaches104() {
        // 4 failures (below limit=5) → state exists, blockedUntil=null
        service.onLoginFailure("coverage@kts.com");
        service.onLoginFailure("coverage@kts.com");
        service.onLoginFailure("coverage@kts.com");
        service.onLoginFailure("coverage@kts.com");

        // 5th check: state found, blockedUntil == null → condition FALSE → L104-105 reached
        assertDoesNotThrow(() -> service.checkLoginAllowed("coverage@kts.com"));
    }

    // ── cooldowns() — catch(RuntimeException) → L238-239 ────────────────────

    @Test
    @DisplayName("cooldowns(): loginCooldownMinutes inválido → NumberFormatException capturada → default {5,15,30}")
    void cooldowns_invalidFormat_catchesRuntimeException() {
        // loginUsernameLimit=1 → first failure reaches cooldowns()
        ReflectionTestUtils.setField(service, "loginUsernameLimit", 1);
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", "5,not_a_number,30");

        // cooldowns() throws NumberFormatException → caught → return {5,15,30}
        // onLoginFailure still blocks the user (with default cooldown)
        assertThrows(TooManyRequestsException.class,
                () -> service.onLoginFailure("blockedcatch@kts.com"));
    }

    // ── cooldowns() — parsed.length == 0 → L237 TRUE branch ─────────────────

    @Test
    @DisplayName("cooldowns(): todos os valores filtrados por > 0 → parsed vazio → usa default")
    void cooldowns_allFilteredOut_emptyParsed_usesDefault() {
        ReflectionTestUtils.setField(service, "loginUsernameLimit", 1);
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", "0,0,0");

        // All 0s filtered by value > 0 → parsed.length == 0 → return default {5,15,30}
        assertThrows(TooManyRequestsException.class,
                () -> service.onLoginFailure("filteredout@kts.com"));
    }

    // ── cooldowns() — filter !isBlank() FALSE branch (L233) ──────────────────

    @Test
    @DisplayName("cooldowns(): loginCooldownMinutes com entrada em branco → blank filtrado (L233 FALSE)")
    void cooldowns_blankEntries_filteredBeforeParsing() {
        ReflectionTestUtils.setField(service, "loginUsernameLimit", 1);
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", " , 15, 30");

        // blank entry " " → !isBlank() is FALSE → filtered
        assertThrows(TooManyRequestsException.class,
                () -> service.onLoginFailure("blankcooldown@kts.com"));
    }

    // ── cooldowns() — filter value > 0 FALSE branch (L235) ──────────────────

    @Test
    @DisplayName("cooldowns(): entrada zero → filtro value>0 FALSE branch (L235)")
    void cooldowns_zeroValue_filteredByPositiveFilter() {
        ReflectionTestUtils.setField(service, "loginUsernameLimit", 1);
        ReflectionTestUtils.setField(service, "loginCooldownMinutes", "0,15,30");

        // 0 → value > 0 is FALSE → filtered out → [15, 30] remain
        assertThrows(TooManyRequestsException.class,
                () -> service.onLoginFailure("zerocooldown@kts.com"));
    }

    // ── safeCpfRef — privacyLogReferenceService não-nulo (L274) ──────────────

    @Test
    @DisplayName("safeCpfRef: privacyLogRef não-nulo → genericRef chamado para CPF (L272 TRUE, L274)")
    void checkPasswordRecoveryAllowed_rateLimitStore_safeCpfRef_withPrivacyLogRef() {
        var rateLimitStore = mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        var privacyLogRef = mock(PrivacyLogReferenceService.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        ReflectionTestUtils.setField(service, "privacyLogReferenceService", privacyLogRef);

        when(privacyLogRef.genericRef(anyString(), anyString())).thenReturn("cpf-ref-123");

        // IP within limit
        when(rateLimitStore.increment(
                eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_IP),
                anyString(), any())).thenReturn(1L);
        // CPF over limit
        when(rateLimitStore.increment(
                eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_CPF),
                anyString(), any())).thenReturn(4L); // > recoveryCpfLimit=3

        assertThrows(TooManyRequestsException.class,
                () -> service.checkPasswordRecoveryAllowed("12345678901", "test@kts.com"));

        verify(privacyLogRef, atLeastOnce()).genericRef(eq("cpf"), anyString());
    }

    // ── safeEmailRef — privacyLogReferenceService não-nulo (L280) ────────────

    @Test
    @DisplayName("safeEmailRef: privacyLogRef não-nulo → emailRef chamado para email (L278 TRUE, L280)")
    void checkPasswordRecoveryAllowed_rateLimitStore_safeEmailRef_withPrivacyLogRef() {
        var rateLimitStore = mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        var privacyLogRef = mock(PrivacyLogReferenceService.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);
        ReflectionTestUtils.setField(service, "privacyLogReferenceService", privacyLogRef);

        when(privacyLogRef.emailRef(anyString())).thenReturn("email-ref-456");

        // IP within limit, CPF within limit
        when(rateLimitStore.increment(
                eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_IP),
                anyString(), any())).thenReturn(1L);
        when(rateLimitStore.increment(
                eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_CPF),
                anyString(), any())).thenReturn(1L);
        // Email over limit
        when(rateLimitStore.increment(
                eq(com.kts.kronos.infrastructure.redis.RedisRateLimitNames.AUTH_RECOVER_EMAIL),
                anyString(), any())).thenReturn(4L); // > recoveryEmailLimit=3

        assertThrows(TooManyRequestsException.class,
                () -> service.checkPasswordRecoveryAllowed("12345678901", "test@kts.com"));

        verify(privacyLogRef, atLeastOnce()).emailRef("test@kts.com");
    }

    // ── checkPasswordRecoveryAllowed rateLimitStore — null/blank CPF (L163) ──

    @Test
    @DisplayName("checkPasswordRecoveryAllowed: rateLimitStore + cpf=null → L163 FALSE branch")
    void checkPasswordRecoveryAllowed_rateLimitStore_nullCpf_L163False() {
        var rateLimitStore = mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);

        when(rateLimitStore.increment(any(), anyString(), any())).thenReturn(1L);

        // null cpf → L163 cpf != null FALSE → CPF block skipped entirely
        assertDoesNotThrow(() -> service.checkPasswordRecoveryAllowed(null, "test@kts.com"));
    }

    // ── checkPasswordRecoveryAllowed rateLimitStore — blank CPF (L163) ───────

    @Test
    @DisplayName("checkPasswordRecoveryAllowed: rateLimitStore + cpf=blank → L163 !isBlank() FALSE branch")
    void checkPasswordRecoveryAllowed_rateLimitStore_blankCpf_L163False() {
        var rateLimitStore = mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);

        when(rateLimitStore.increment(any(), anyString(), any())).thenReturn(1L);

        // blank cpf → L163 !cpf.isBlank() FALSE → CPF block skipped
        assertDoesNotThrow(() -> service.checkPasswordRecoveryAllowed("   ", "test@kts.com"));
    }

    // ── checkPasswordRecoveryAllowed rateLimitStore — null email (L168) ──────

    @Test
    @DisplayName("checkPasswordRecoveryAllowed: rateLimitStore + email=null → L168 FALSE branch")
    void checkPasswordRecoveryAllowed_rateLimitStore_nullEmail_L168False() {
        var rateLimitStore = mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);

        when(rateLimitStore.increment(any(), anyString(), any())).thenReturn(1L);

        // null email → L168 email != null FALSE → email block skipped
        assertDoesNotThrow(() -> service.checkPasswordRecoveryAllowed("12345678901", null));
    }

    // ── checkLoginAllowed — estado bloqueado (blockedUntil no futuro) → L100 TRUE ─

    @Test
    @DisplayName("checkLoginAllowed: blockedUntil no futuro → L100 TRUE branch → lança TooManyRequestsException")
    void checkLoginAllowed_blockedUntilInFuture_L100TrueBranch() {
        ReflectionTestUtils.setField(service, "loginUsernameLimit", 1);
        // 1 falha → imediatamente bloqueado (blockedUntil = now + 5min)
        assertThrows(TooManyRequestsException.class, () -> service.onLoginFailure("blockeduser@kts.com"));
        // checkLoginAllowed: state encontrado, blockedUntil != null && isAfter(now) → L100 TRUE → throw
        assertThrows(TooManyRequestsException.class, () -> service.checkLoginAllowed("blockeduser@kts.com"));
    }

    // ── checkPasswordRecoveryAllowed rateLimitStore — blank email → L168 FALSE branch ─

    @Test
    @DisplayName("checkPasswordRecoveryAllowed: rateLimitStore + email em branco → L168 !isBlank() FALSE branch")
    void checkPasswordRecoveryAllowed_rateLimitStore_blankEmail_L168False() {
        var rateLimitStore = mock(com.kts.kronos.application.port.out.provider.RateLimitStore.class);
        ReflectionTestUtils.setField(service, "rateLimitStore", rateLimitStore);

        when(rateLimitStore.increment(any(), anyString(), any())).thenReturn(1L);

        // blank email → L168 !email.isBlank() FALSE → email block skipped (second && short-circuits)
        assertDoesNotThrow(() -> service.checkPasswordRecoveryAllowed("12345678901", "   "));
    }

    // ── checkPasswordRecoveryAllowed in-memory — blank cpf → L178 FALSE branch ─

    @Test
    @DisplayName("checkPasswordRecoveryAllowed: sem rateLimitStore + cpf em branco → L178 !isBlank() FALSE (in-memory path)")
    void checkPasswordRecoveryAllowed_inMemory_blankCpf_L178False() {
        // No rateLimitStore → in-memory path
        // blank cpf → L178: cpf != null TRUE, !cpf.isBlank() FALSE → overall FALSE → skip cpf block
        assertDoesNotThrow(() -> service.checkPasswordRecoveryAllowed("   ", "test@kts.com"));
    }

    // ── checkPasswordRecoveryAllowed in-memory — blank email → L181 FALSE branch ─

    @Test
    @DisplayName("checkPasswordRecoveryAllowed: sem rateLimitStore + email em branco → L181 !isBlank() FALSE (in-memory path)")
    void checkPasswordRecoveryAllowed_inMemory_blankEmail_L181False() {
        // No rateLimitStore → in-memory path
        // blank email → L181: email != null TRUE, !email.isBlank() FALSE → overall FALSE → skip email block
        assertDoesNotThrow(() -> service.checkPasswordRecoveryAllowed("12345678901", "   "));
    }

    // ── checkLoginAllowed: blockedUntil != null but EXPIRED → isAfter=FALSE ────

    @Test
    @DisplayName("checkLoginAllowed: blockedUntil não-null mas expirado → isAfter(now)=FALSE → não lança")
    void checkLoginAllowed_expiredBlock_doesNotThrow() throws Exception {
        // 1) Create a blocked state (limit=1 → first failure blocks)
        ReflectionTestUtils.setField(service, "loginUsernameLimit", 1);
        assertThrows(TooManyRequestsException.class, () -> service.onLoginFailure("expiredblock@kts.com"));

        // 2) Use reflection to set blockedUntil to 1 second in the PAST (expired)
        @SuppressWarnings("unchecked")
        var usernameFailures = (Map<String, Object>) ReflectionTestUtils.getField(service, "usernameFailures");
        String key = (String) ReflectionTestUtils.invokeMethod(service, "usernameKey", "expiredblock@kts.com");
        Object state = usernameFailures.get(key);
        // Set blockedUntil to the past → isAfter(now)=FALSE → block NOT active
        ReflectionTestUtils.setField(state, "blockedUntil", Instant.now().minusSeconds(1));

        // 3) checkLoginAllowed: state found, blockedUntil != null (not null) but !isAfter(now)=TRUE → no throw
        assertDoesNotThrow(() -> service.checkLoginAllowed("expiredblock@kts.com"));
    }

    // ── digits(null): value==null=TRUE → returns "unknown" (dead code via reflection) ─

    @Test
    @DisplayName("digits(null): valor nulo → retorna 'unknown' (via reflexão)")
    void digits_withNullValue_returnsUnknown() {
        // Private method digits(String) → value==null=TRUE → "unknown"
        String result = ReflectionTestUtils.invokeMethod(service, "digits", (Object) null);
        assertEquals("unknown", result);
    }
}
