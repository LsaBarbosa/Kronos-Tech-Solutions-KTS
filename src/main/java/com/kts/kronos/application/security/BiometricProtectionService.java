package com.kts.kronos.application.security;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.port.out.provider.LivenessVerificationProvider;
import com.kts.kronos.application.port.out.provider.RateLimitStore;
import com.kts.kronos.domain.model.enuns.LivenessOperation;
import com.kts.kronos.infrastructure.redis.RedisRateLimitNames;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class BiometricProtectionService {

    private static final String PAYLOAD_TOO_LARGE = "Imagem biométrica excede o tamanho máximo permitido.";
    private static final String LIVENESS_REQUIRED = "Validação de liveness obrigatória para esta operação.";
    private static final String LOGIN_FACE_RATE_LIMIT = "Muitas tentativas de login facial. Tente novamente em instantes.";
    private static final String CHECKIN_RATE_LIMIT = "Muitas tentativas biométricas de registro de ponto. Tente novamente em instantes.";
    private static final String ENROLLMENT_RATE_LIMIT = "Muitas tentativas de cadastro/atualização biométrica. Tente novamente em instantes.";
    private static final String CONTRACT_SIGN_RATE_LIMIT = "Muitas tentativas de assinatura biométrica. Tente novamente em instantes.";
    private static final String TIMESHEET_SIGN_RATE_LIMIT = "Muitas tentativas de assinatura de espelho de ponto. Tente novamente em instantes.";

    private final HttpServletRequest request;
    private final ClientIpResolver clientIpResolver;
    private final ObjectProvider<LivenessVerificationProvider> livenessVerificationProvider;
    private final PrivacyLogReferenceService privacyLogReferenceService;
    @Autowired(required = false)
    private RateLimitStore rateLimitStore;

    public BiometricProtectionService(
            HttpServletRequest request,
            ClientIpResolver clientIpResolver,
            ObjectProvider<LivenessVerificationProvider> livenessVerificationProvider,
            PrivacyLogReferenceService privacyLogReferenceService
    ) {
        this.request = request;
        this.clientIpResolver = clientIpResolver;
        this.livenessVerificationProvider = livenessVerificationProvider;
        this.privacyLogReferenceService = privacyLogReferenceService;
    }

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

    @Value("${app.biometric.contract-sign.limit:${biometric.contract-sign.limit:5}}")
    private int contractSignLimit;

    @Value("${app.biometric.contract-sign.window-seconds:${biometric.contract-sign.window-seconds:300}}")
    private int contractSignWindowSeconds;

    @Value("${app.biometric.timesheet-sign.limit:${biometric.timesheet-sign.limit:5}}")
    private int timesheetSignLimit;

    @Value("${app.biometric.timesheet-sign.window-seconds:${biometric.timesheet-sign.window-seconds:300}}")
    private int timesheetSignWindowSeconds;

    public void protectPublicLogin(String faceImageBase64, Boolean livenessPassed) {
        ensurePayloadSize(faceImageBase64);
        ensureServerSideLiveness(faceImageBase64, LivenessOperation.FACE_LOGIN, null);
        consume(
                RedisRateLimitNames.BIOMETRIC_LOGIN_FACE,
                clientIp(),
                loginFaceLimit,
                Duration.ofSeconds(loginFaceWindowSeconds),
                LOGIN_FACE_RATE_LIMIT
        );
    }

    public void protectCheckIn(UUID employeeId, String faceImageBase64, Boolean livenessPassed) {
        ensurePayloadSize(faceImageBase64);
        ensureServerSideLiveness(faceImageBase64, LivenessOperation.CHECKIN, employeeId);
        consume(
                RedisRateLimitNames.BIOMETRIC_CHECKIN,
                employeeId + "|" + clientIp(),
                checkinLimit,
                Duration.ofSeconds(checkinWindowSeconds),
                CHECKIN_RATE_LIMIT
        );
    }

    public void protectTimesheetSigning(UUID employeeId, String faceImageBase64) {
        ensurePayloadSize(faceImageBase64);
        ensureServerSideLiveness(faceImageBase64, LivenessOperation.TIMESHEET_SIGNING, employeeId);
        // Hard limit: por colaborador — impede abuso independentemente de rotação de IP
        consume(
                RedisRateLimitNames.BIOMETRIC_TIMESHEET_SIGN_EMPLOYEE,
                employeeId.toString(),
                timesheetSignLimit,
                Duration.ofSeconds(timesheetSignWindowSeconds),
                TIMESHEET_SIGN_RATE_LIMIT
        );
        // Soft limit: por colaborador+IP — complementar para mitigar botnets
        consume(
                RedisRateLimitNames.BIOMETRIC_TIMESHEET_SIGN,
                employeeId + "|" + clientIp(),
                timesheetSignLimit,
                Duration.ofSeconds(timesheetSignWindowSeconds),
                TIMESHEET_SIGN_RATE_LIMIT
        );
    }

    public void protectContractSigning(UUID employeeId, String faceImageBase64) {
        ensurePayloadSize(faceImageBase64);
        ensureServerSideLiveness(faceImageBase64, LivenessOperation.CONTRACT_SIGNING, employeeId);
        // Hard limit: por colaborador — impede abuso independentemente de rotação de IP
        consume(
                RedisRateLimitNames.BIOMETRIC_CONTRACT_SIGN_EMPLOYEE,
                employeeId.toString(),
                contractSignLimit,
                Duration.ofSeconds(contractSignWindowSeconds),
                CONTRACT_SIGN_RATE_LIMIT
        );
        // Soft limit: por colaborador+IP — complementar para mitigar botnets
        consume(
                RedisRateLimitNames.BIOMETRIC_CONTRACT_SIGN,
                employeeId + "|" + clientIp(),
                contractSignLimit,
                Duration.ofSeconds(contractSignWindowSeconds),
                CONTRACT_SIGN_RATE_LIMIT
        );
    }

    public void protectEnrollment(UUID employeeId, String faceImageBase64, Boolean livenessPassed) {
        ensurePayloadSize(faceImageBase64);
        ensureServerSideLiveness(faceImageBase64, LivenessOperation.ENROLLMENT, employeeId);
        consume(
                RedisRateLimitNames.BIOMETRIC_ENROLLMENT,
                employeeId + "|" + clientIp(),
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

        LivenessVerificationProvider provider = livenessVerificationProvider.getIfAvailable();

        if (provider == null) {
            log.error("event=biometric_liveness_provider_missing operation={} employeeRef={}",
                    operation, privacyLogReferenceService.employeeRef(employeeId));
            throw new IllegalStateException(
                    "BIOMETRIC_LIVENESS_REQUIRED=true requires a configured LivenessVerificationProvider"
            );
        }

        var result = provider.verify(faceImageBase64, operation, employeeId);

        if (!result.passed()) {
            log.warn("event=biometric_liveness_failed operation={} employeeRef={} reason={}",
                    operation, privacyLogReferenceService.employeeRef(employeeId), result.reasonCode());
            throw new ForbiddenException(LIVENESS_REQUIRED);
        }

        log.debug("event=biometric_liveness_passed operation={} employeeRef={} provider={}",
                operation, privacyLogReferenceService.employeeRef(employeeId), result.provider());
    }

    private void consume(String bucketName, String rawScope, int limit, Duration window, String message) {
        if (rateLimitStore == null) {
            log.error("event=biometric_rate_limit_store_missing bucket={} — rate limiting disabled", bucketName);
            return;
        }
        var count = rateLimitStore.increment(bucketName, rawScope, window);
        if (count > limit) {
            log.warn("event=biometric_rate_limit_exceeded rateLimitRef={}",
                    privacyLogReferenceService.genericRef("biometric_rate_limit", bucketName + ":" + rawScope));
            throw new TooManyRequestsException(message);
        }
    }

    private String clientIp() {
        return clientIpResolver.resolve(request);
    }
}
