package com.kts.kronos.application.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
public class LgpdProductionReadinessValidator {

    @Bean
    ApplicationRunner validateLgpdProduction(Environment environment) {
        return args -> {
            if (!isProductionProfile(environment)) {
                return;
            }

            validateRetentionConfiguration(environment);
            validateBiometricConfiguration(environment);
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

    private void validateRetentionConfiguration(Environment environment) {
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

        log.info("event=lgpd_retention_production_validated status=OK " +
                "enabled={} mode={} applyConfirmed={} allowApply={}",
                enabled, mode, applyConfirmed, allowApply);
    }

    private void validateBiometricConfiguration(Environment environment) {
        boolean livenessRequired = environment.getProperty("biometric.liveness-required", Boolean.class, false);

        if (!livenessRequired) {
            log.warn("event=lgpd_biometric_liveness_disabled status=ACCEPTED_BY_PRODUCT_DECISION " +
                    "action=liveness_check_skipped note=BasicImageLivenessProvider active as fallback");
            return;
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
