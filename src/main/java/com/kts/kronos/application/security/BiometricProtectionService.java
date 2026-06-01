package com.kts.kronos.application.security;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.port.out.provider.LivenessVerificationProvider;
import com.kts.kronos.domain.model.enuns.LivenessOperation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiometricProtectionService {

    private static final String PAYLOAD_TOO_LARGE = "Imagem biométrica excede o tamanho máximo permitido.";
    private static final String LIVENESS_REQUIRED = "Validação de liveness obrigatória para esta operação.";
    private static final String LOGIN_FACE_RATE_LIMIT = "Muitas tentativas de login facial. Tente novamente em instantes.";
    private static final String CHECKIN_RATE_LIMIT = "Muitas tentativas biométricas de registro de ponto. Tente novamente em instantes.";
    private static final String ENROLLMENT_RATE_LIMIT = "Muitas tentativas de cadastro/atualização biométrica. Tente novamente em instantes.";

    private final HttpServletRequest request;
    private final ClientIpResolver clientIpResolver;
    private final LivenessVerificationProvider livenessVerificationProvider;
    private final PrivacyLogReferenceService privacyLogReferenceService;
    private final Map<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    @Value("${app.biometric.max-base64-chars:${biometric.max-base64-chars:1500000}}")
    private int maxBase64Chars;

    @Value("${app.biometric.liveness-required:${biometric.liveness-required:false}}")
    private boolean livenessRequired;

    @Value("${app.biometric.login-face.limit:${biometric.login-face.limit:5}}")
    private int loginFaceLimit;

    @Value("${app.biometric.login-face.window-seconds:${biometric.login-face.window-seconds:60}}")
    private int loginFaceWindowSeconds;

    @Value("${app.biometric.checkin.limit:${biometric.checkin.limit:20}}")
    private int checkinLimit;

    @Value("${app.biometric.checkin.window-seconds:${biometric.checkin.window-seconds:60}}")
    private int checkinWindowSeconds;

    @Value("${app.biometric.enrollment.limit:${biometric.enrollment.limit:10}}")
    private int enrollmentLimit;

    @Value("${app.biometric.enrollment.window-seconds:${biometric.enrollment.window-seconds:600}}")
    private int enrollmentWindowSeconds;

    public void protectPublicLogin(String faceImageBase64, Boolean livenessPassed) {
        ensurePayloadSize(faceImageBase64);
        ensureServerSideLiveness(faceImageBase64, LivenessOperation.FACE_LOGIN, null);
        consume(
                "bio:login-face:" + clientIp(),
                loginFaceLimit,
                Duration.ofSeconds(loginFaceWindowSeconds),
                LOGIN_FACE_RATE_LIMIT
        );
    }

    public void protectCheckIn(UUID employeeId, String faceImageBase64, Boolean livenessPassed) {
        ensurePayloadSize(faceImageBase64);
        ensureServerSideLiveness(faceImageBase64, LivenessOperation.CHECKIN, employeeId);
        consume(
                "bio:checkin:" + employeeId + ":" + clientIp(),
                checkinLimit,
                Duration.ofSeconds(checkinWindowSeconds),
                CHECKIN_RATE_LIMIT
        );
    }

    public void protectEnrollment(UUID employeeId, String faceImageBase64, Boolean livenessPassed) {
        ensurePayloadSize(faceImageBase64);
        ensureServerSideLiveness(faceImageBase64, LivenessOperation.ENROLLMENT, employeeId);
        consume(
                "bio:enrollment:" + employeeId + ":" + clientIp(),
                enrollmentLimit,
                Duration.ofSeconds(enrollmentWindowSeconds),
                ENROLLMENT_RATE_LIMIT
        );
    }

    private void ensurePayloadSize(String faceImageBase64) {
        if (faceImageBase64 == null || faceImageBase64.isBlank()) {
            return;
        }

        if (faceImageBase64.length() > maxBase64Chars) {
            throw new BadRequestException(PAYLOAD_TOO_LARGE);
        }
    }

    private void ensureServerSideLiveness(String faceImageBase64, LivenessOperation operation, UUID employeeId) {
        if (!livenessRequired) {
            log.debug("event=biometric_liveness_skipped reason=disabled_by_product_decision operation={}", operation);
            return;
        }

        var result = livenessVerificationProvider.verify(faceImageBase64, operation, employeeId);

        if (!result.passed()) {
            log.warn("event=biometric_liveness_failed operation={} employeeRef={} reason={}",
                    operation, privacyLogReferenceService.employeeRef(employeeId), result.reasonCode());
            throw new ForbiddenException(LIVENESS_REQUIRED);
        }

        log.debug("event=biometric_liveness_passed operation={} employeeRef={} provider={}",
                operation, privacyLogReferenceService.employeeRef(employeeId), result.provider());
    }

    private void consume(String key, int limit, Duration window, String message) {
        Instant now = Instant.now();
        Instant threshold = now.minus(window);

        Deque<Instant> bucket = buckets.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());

        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(threshold)) {
                bucket.pollFirst();
            }

            if (bucket.size() >= limit) {
                log.warn("event=biometric_rate_limit_exceeded rateLimitRef={}",
                        privacyLogReferenceService.genericRef("biometric_rate_limit", key));
                throw new TooManyRequestsException(message);
            }

            bucket.addLast(now);
        }
    }

    private String clientIp() {
        return clientIpResolver.resolve(request);
    }
}
