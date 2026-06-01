package com.kts.kronos.application.config;

import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.application.port.out.provider.LivenessVerificationProvider;
import com.kts.kronos.application.security.BasicImageLivenessVerificationProvider;
import com.kts.kronos.application.security.DisabledLivenessVerificationProvider;
import com.kts.kronos.application.service.retention.RetentionDomainProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Slf4j
@Configuration
public class LgpdProductionReadinessValidator {

    private static final String LOCAL_DEV_LGPD_LOG_SECRET = "local-dev-lgpd-log-secret";

    @Bean
    ApplicationRunner validateLgpdProduction(
            Environment environment,
            RetentionPolicyCatalog retentionPolicyCatalog,
            List<RetentionDomainProcessor> retentionProcessors,
            LivenessVerificationProvider livenessVerificationProvider
    ) {
        return buildRunner(environment, retentionPolicyCatalog, retentionProcessors, livenessVerificationProvider);
    }

    ApplicationRunner validateLgpdProduction(Environment environment) {
        return buildRunner(environment, null, List.of(), null);
    }

    ApplicationRunner buildRunner(
            Environment environment,
            RetentionPolicyCatalog retentionPolicyCatalog,
            List<RetentionDomainProcessor> retentionProcessors,
            LivenessVerificationProvider livenessVerificationProvider
    ) {
        return args -> {
            if (!isProductionProfile(environment)) {
                return;
            }

            validatePrivacyLogHashSecret(environment);
            validateRetentionConfiguration(environment, retentionPolicyCatalog, retentionProcessors);
            validateBiometricConfiguration(environment, livenessVerificationProvider);
        };
    }

    private boolean isProductionProfile(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private void validateRetentionConfiguration(
            Environment environment,
            RetentionPolicyCatalog retentionPolicyCatalog,
            List<RetentionDomainProcessor> retentionProcessors
    ) {
        boolean enabled = environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false);
        String mode = environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN");
        boolean applyConfirmed = environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false);
        boolean allowApply = environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false);
        String allowWarnings = environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false");

        if (!enabled) {
            String message = "LGPD Retention Scheduler is DISABLED in production. " +
                    "Set LGPD_RETENTION_SCHEDULER_ENABLED=true";
            logCriticalError(message);
            if (!isAllowStartWithWarnings(allowWarnings)) {
                throw new IllegalStateException(message);
            }
        }

        if (!"APPLY".equalsIgnoreCase(mode)) {
            log.warn("event=lgpd_retention_mode_not_apply status=OPERATIONAL_DECISION " +
                    "mode={} note=DRY_RUN is valid operational state for gradual rollout", mode);
            log.info("event=lgpd_retention_production_validated status=DRY_RUN enabled={} mode={}", enabled, mode);
            return;
        }

        if (!applyConfirmed) {
            String message = "LGPD Retention Scheduler apply-confirmed is FALSE in production. " +
                    "Set LGPD_RETENTION_SCHEDULER_APPLY_CONFIRMED=true";
            logCriticalError(message);
            if (!isAllowStartWithWarnings(allowWarnings)) {
                throw new IllegalStateException(message);
            }
        }

        if (!allowApply) {
            String message = "LGPD Retention allow-apply is FALSE in production. " +
                    "Set LGPD_RETENTION_ALLOW_APPLY=true";
            logCriticalError(message);
            if (!isAllowStartWithWarnings(allowWarnings)) {
                throw new IllegalStateException(message);
            }
        }

        validateApplyCapableProcessors(retentionPolicyCatalog, retentionProcessors);

        log.info("event=lgpd_retention_production_validated status=OK " +
                "enabled={} mode={} applyConfirmed={} allowApply={}",
                enabled, mode, applyConfirmed, allowApply);
    }

    private void validateApplyCapableProcessors(
            RetentionPolicyCatalog retentionPolicyCatalog,
            List<RetentionDomainProcessor> retentionProcessors
    ) {
        if (retentionPolicyCatalog == null || retentionProcessors == null || retentionProcessors.isEmpty()) {
            log.warn("event=lgpd_retention_processor_validation_skipped reason=processor_context_unavailable");
            return;
        }

        for (var policy : retentionPolicyCatalog.getSchedulerExecutablePolicies()) {
            var processor = retentionProcessors.stream()
                    .filter(candidate -> candidate.supports() == policy.resourceType())
                    .findFirst();

            if (processor.isEmpty()) {
                throw new IllegalStateException(
                        "LGPD retention APPLY is enabled but no processor exists for " + policy.resourceType()
                );
            }

            if (!processor.get().supportsApply()) {
                throw new IllegalStateException(
                        "LGPD retention APPLY is enabled but processor does not support APPLY for " + policy.resourceType()
                );
            }
        }
    }

    private void validatePrivacyLogHashSecret(Environment environment) {
        String hashSecret = environment.getProperty("kronos.lgpd.log.hash-secret", "");

        if (hashSecret == null
                || hashSecret.isBlank()
                || LOCAL_DEV_LGPD_LOG_SECRET.equals(hashSecret)) {
            String message = "LGPD_LOG_HASH_SECRET must be configured with a non-default value in production. " +
                    "Set kronos.lgpd.log.hash-secret or LGPD_LOG_HASH_SECRET environment variable.";
            logCriticalError(message);
            throw new IllegalStateException(message);
        }

        log.info("event=lgpd_log_hash_secret_validated status=OK");
    }

    private void validateBiometricConfiguration(
            Environment environment,
            LivenessVerificationProvider livenessVerificationProvider
    ) {
        boolean livenessRequired = environment.getProperty("biometric.liveness-required", Boolean.class, false);

        if (!livenessRequired) {
            log.info("event=lgpd_biometric_liveness_context_disabled status=ACCEPTED_PRODUCT_DECISION " +
                    "action=liveness_not_required");
            return;
        }

        if (livenessVerificationProvider == null
                || livenessVerificationProvider instanceof BasicImageLivenessVerificationProvider
                || livenessVerificationProvider instanceof DisabledLivenessVerificationProvider) {
            String message = "BIOMETRIC_LIVENESS_REQUIRED=true requires a real production liveness provider.";
            logCriticalError(message);
            throw new IllegalStateException(message);
        }

        log.info("event=lgpd_biometric_production_validated status=OK livenessRequired=true");
    }

    private void logCriticalError(String message) {
        log.error("event=lgpd_production_readiness_failure severity=CRITICAL message={}", message);
    }

    private boolean isAllowStartWithWarnings(String allowWarnings) {
        return "true".equalsIgnoreCase(allowWarnings);
    }
}
