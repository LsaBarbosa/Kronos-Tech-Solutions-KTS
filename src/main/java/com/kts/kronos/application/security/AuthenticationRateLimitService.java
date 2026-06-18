package com.kts.kronos.application.security;

import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.port.out.provider.RateLimitStore;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.infrastructure.redis.RedisRateLimitNames;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationRateLimitService {
    private static final String LOGIN_RATE_LIMIT_MESSAGE = "Muitas tentativas de login. Tente novamente mais tarde.";
    private static final String RECOVERY_RATE_LIMIT_MESSAGE = "Muitas solicitações de recuperação de senha. Tente novamente mais tarde.";

    private final HttpServletRequest request;
    private final ClientIpResolver clientIpResolver;
    private final Map<String, Deque<Instant>> requestBuckets = new ConcurrentHashMap<>();
    private final Map<String, UsernameFailureState> usernameFailures = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RateLimitStore rateLimitStore;

    @Autowired(required = false)
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Value("${kronos.security.rate-limit.login.ip.limit:10}")
    private int loginIpLimit;

    @Value("${kronos.security.rate-limit.login.ip.window-seconds:60}")
    private int loginIpWindowSeconds;

    @Value("${kronos.security.rate-limit.login.username.limit:5}")
    private int loginUsernameLimit;

    @Value("${kronos.security.rate-limit.login.username.window-seconds:300}")
    private int loginUsernameWindowSeconds;

    @Value("${kronos.security.rate-limit.login.cooldown-minutes:5,15,30}")
    private String loginCooldownMinutes;

    @Value("${kronos.security.rate-limit.recovery.cpf.limit:3}")
    private int recoveryCpfLimit;

    @Value("${kronos.security.rate-limit.recovery.email.limit:3}")
    private int recoveryEmailLimit;

    @Value("${kronos.security.rate-limit.recovery.ip.limit:10}")
    private int recoveryIpLimit;

    @Value("${kronos.security.rate-limit.recovery.window-seconds:3600}")
    private int recoveryWindowSeconds;

    @Value("${kronos.security.rate-limit.admin-check.limit:30}")
    private int adminCheckLimit;

    @Value("${kronos.security.rate-limit.admin-check.window-seconds:60}")
    private int adminCheckWindowSeconds;

    public void checkLoginAllowed(String username) {
        var now = Instant.now();
        if (rateLimitStore != null) {
            var ipCount = rateLimitStore.increment(
                    RedisRateLimitNames.AUTH_LOGIN_IP,
                    clientIp(),
                    Duration.ofSeconds(loginIpWindowSeconds)
            );
            if (ipCount > loginIpLimit) {
                log.warn("event=rate_limit_blocked operation=auth_login target={} reason=ip_limit", safeClientIpRef());
                throw new TooManyRequestsException(LOGIN_RATE_LIMIT_MESSAGE);
            }

            if (rateLimitStore.isCoolingDown(RedisRateLimitNames.AUTH_LOGIN_USERNAME, username)) {
                log.warn("event=rate_limit_blocked operation=auth_login target={} reason=username_cooldown", safeUsernameRef(username));
                throw new TooManyRequestsException(LOGIN_RATE_LIMIT_MESSAGE);
            }
            return;
        }

        consumeOrThrow("auth:login:ip:" + clientIp(), loginIpLimit, Duration.ofSeconds(loginIpWindowSeconds), LOGIN_RATE_LIMIT_MESSAGE, now);
        var key = usernameKey(username);
        var state = usernameFailures.get(key);
        if (state == null) {
            return;
        }
        synchronized (state) {
            if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
                log.warn("Login bloqueado por rate limit para usernameHash={} ate={}", Integer.toHexString(key.hashCode()), state.blockedUntil);
                throw new TooManyRequestsException(LOGIN_RATE_LIMIT_MESSAGE);
            }
        }
    }

    public void onLoginFailure(String username) {
        var now = Instant.now();
        if (rateLimitStore != null) {
            var attempts = rateLimitStore.increment(
                    RedisRateLimitNames.AUTH_LOGIN_USERNAME,
                    username,
                    Duration.ofSeconds(loginUsernameWindowSeconds)
            );
            if (attempts >= loginUsernameLimit) {
                var penalty = rateLimitStore.incrementPenalty(
                        RedisRateLimitNames.AUTH_LOGIN_USERNAME,
                        username,
                        Duration.ofDays(1)
                );
                rateLimitStore.setCooldown(
                        RedisRateLimitNames.AUTH_LOGIN_USERNAME,
                        username,
                        cooldownForPenalty((int) penalty)
                );
                log.warn("event=rate_limit_blocked operation=auth_login target={} reason=username_threshold", safeUsernameRef(username));
                throw new TooManyRequestsException(LOGIN_RATE_LIMIT_MESSAGE);
            }
            return;
        }

        var key = usernameKey(username);
        var state = usernameFailures.computeIfAbsent(key, ignored -> new UsernameFailureState());
        synchronized (state) {
            purgeBefore(state.attempts, now.minusSeconds(loginUsernameWindowSeconds));
            state.attempts.addLast(now);
            if (state.attempts.size() >= loginUsernameLimit) {
                state.penaltyLevel = Math.min(state.penaltyLevel + 1, cooldowns().length);
                state.blockedUntil = now.plus(cooldownForPenalty(state.penaltyLevel));
                state.attempts.clear();
                log.warn("event=rate_limit_blocked operation=auth_login target={} reason=username_threshold until={}",
                        safeUsernameRef(username), state.blockedUntil);
                throw new TooManyRequestsException(LOGIN_RATE_LIMIT_MESSAGE);
            }
        }
    }

    public void onLoginSuccess(String username) {
        if (rateLimitStore != null) {
            rateLimitStore.reset(RedisRateLimitNames.AUTH_LOGIN_USERNAME, username);
        }
        usernameFailures.remove(usernameKey(username));
    }

    public void checkPasswordRecoveryAllowed(String cpf, String email) {
        var now = Instant.now();
        var window = Duration.ofSeconds(recoveryWindowSeconds);
        if (rateLimitStore != null) {
            if (rateLimitStore.increment(RedisRateLimitNames.AUTH_RECOVER_IP, clientIp(), window) > recoveryIpLimit) {
                log.warn("event=rate_limit_blocked operation=password_recovery target={} reason=ip_limit", safeClientIpRef());
                throw new TooManyRequestsException(RECOVERY_RATE_LIMIT_MESSAGE);
            }
            if (cpf != null && !cpf.isBlank()
                    && rateLimitStore.increment(RedisRateLimitNames.AUTH_RECOVER_CPF, cpf, window) > recoveryCpfLimit) {
                log.warn("event=rate_limit_blocked operation=password_recovery target={} reason=cpf_limit", safeCpfRef(cpf));
                throw new TooManyRequestsException(RECOVERY_RATE_LIMIT_MESSAGE);
            }
            if (email != null && !email.isBlank()
                    && rateLimitStore.increment(RedisRateLimitNames.AUTH_RECOVER_EMAIL, email, window) > recoveryEmailLimit) {
                log.warn("event=rate_limit_blocked operation=password_recovery target={} reason=email_limit", safeEmailRef(email));
                throw new TooManyRequestsException(RECOVERY_RATE_LIMIT_MESSAGE);
            }
            return;
        }

        try {
            consumeOrThrow("auth:recover:ip:" + clientIp(), recoveryIpLimit, window, RECOVERY_RATE_LIMIT_MESSAGE, now);
            if (cpf != null && !cpf.isBlank()) {
                consumeOrThrow("auth:recover:cpf:" + digits(cpf), recoveryCpfLimit, window, RECOVERY_RATE_LIMIT_MESSAGE, now);
            }
            if (email != null && !email.isBlank()) {
                consumeOrThrow("auth:recover:email:" + normalize(email), recoveryEmailLimit, window, RECOVERY_RATE_LIMIT_MESSAGE, now);
            }
        } catch (TooManyRequestsException ex) {
            log.warn("event=rate_limit_blocked operation=password_recovery target={} reason=limit", safeClientIpRef());
            throw ex;
        }
    }

    public void checkAdminSearchRateLimit() {
        var now = Instant.now();
        if (rateLimitStore != null) {
            if (rateLimitStore.increment(RedisRateLimitNames.ADMIN_CHECK, clientIp(), Duration.ofSeconds(adminCheckWindowSeconds)) > adminCheckLimit) {
                log.warn("event=rate_limit_blocked operation=admin_check target={} reason=limit", safeClientIpRef());
                throw new TooManyRequestsException("Muitas consultas de verificação. Tente novamente mais tarde.");
            }
            return;
        }
        consumeOrThrow("admin:check:" + clientIp(), adminCheckLimit, Duration.ofSeconds(adminCheckWindowSeconds),
                "Muitas consultas de verificação. Tente novamente mais tarde.", now);
    }

    private void consumeOrThrow(String key, int limit, Duration window, String message, Instant now) {
        if (limit <= 0) {
            throw new TooManyRequestsException(message);
        }
        var bucket = requestBuckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            purgeBefore(bucket, now.minus(window));
            if (bucket.size() >= limit) {
                throw new TooManyRequestsException(message);
            }
            bucket.addLast(now);
        }
    }

    private void purgeBefore(Deque<Instant> bucket, Instant threshold) {
        while (!bucket.isEmpty() && bucket.peekFirst().isBefore(threshold)) {
            bucket.pollFirst();
        }
    }

    private Duration cooldownForPenalty(int penaltyLevel) {
        var cooldowns = cooldowns();
        int index = Math.max(0, Math.min(penaltyLevel - 1, cooldowns.length - 1));
        return Duration.ofMinutes(cooldowns[index]);
    }

    private long[] cooldowns() {
        try {
            long[] parsed = java.util.Arrays.stream(loginCooldownMinutes.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .mapToLong(Long::parseLong)
                    .filter(value -> value > 0)
                    .toArray();
            return parsed.length == 0 ? new long[]{5, 15, 30} : parsed;
        } catch (RuntimeException ex) {
            return new long[]{5, 15, 30};
        }
    }

    private String usernameKey(String username) {
        return "auth:login:user:" + normalize(username);
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String digits(String value) {
        return value == null ? "unknown" : value.replaceAll("\\D", "");
    }

    private String clientIp() {
        return clientIpResolver.resolve(request);
    }

    private String safeClientIpRef() {
        return privacyLogReferenceService == null
                ? "ip_ref_unknown"
                : privacyLogReferenceService.genericRef("ip", clientIp());
    }

    private String safeUsernameRef(String username) {
        return privacyLogReferenceService == null
                ? "username_ref_unknown"
                : privacyLogReferenceService.genericRef("username", username);
    }

    private String safeCpfRef(String cpf) {
        return privacyLogReferenceService == null
                ? "cpf_ref_unknown"
                : privacyLogReferenceService.genericRef("cpf", digits(cpf));
    }

    private String safeEmailRef(String email) {
        return privacyLogReferenceService == null
                ? "email_ref_unknown"
                : privacyLogReferenceService.emailRef(email);
    }

    private static final class UsernameFailureState {
        private final Deque<Instant> attempts = new ArrayDeque<>();
        private Instant blockedUntil;
        private int penaltyLevel;
    }
}
