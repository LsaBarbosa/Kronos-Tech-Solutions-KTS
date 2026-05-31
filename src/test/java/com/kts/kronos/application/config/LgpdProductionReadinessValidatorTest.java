package com.kts.kronos.application.config;

import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.application.port.out.provider.LivenessVerificationProvider;
import com.kts.kronos.application.service.retention.RetentionDomainProcessor;
import com.kts.kronos.domain.model.LivenessVerificationResult;
import com.kts.kronos.domain.model.RetentionPolicyCatalogEntry;
import com.kts.kronos.domain.model.enuns.RetentionAction;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LgpdProductionReadinessValidatorTest {

    private LgpdProductionReadinessValidator validator;
    private Environment environment;
    private ApplicationArguments applicationArguments;
    private LivenessVerificationProvider realLivenessProvider;

    @BeforeEach
    void setUp() {
        validator = new LgpdProductionReadinessValidator();
        environment = mock(Environment.class);
        applicationArguments = mock(ApplicationArguments.class);
        realLivenessProvider = (faceImageBase64, operation, employeeId) ->
                LivenessVerificationResult.passed("REAL_PRODUCTION_PROVIDER", 0.99);
    }

    @Test
    @DisplayName("shouldNotFailWhenLivenessRequiredFalseInProd")
    void shouldNotFailWhenLivenessRequiredFalseInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(false);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldNotRunValidationOutsideProdProfile")
    void shouldNotRunValidationOutsideProdProfile() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert - should not throw and validation should be skipped
        assertDoesNotThrow(() -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldThrowWhenRetentionEnabledButApplyConfirmedFalseInProd")
    void shouldThrowWhenRetentionEnabledButApplyConfirmedFalseInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("APPLY");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(false); // This should trigger failure
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(true);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldThrowWhenRetentionEnabledButAllowApplyFalseInProd")
    void shouldThrowWhenRetentionEnabledButAllowApplyFalseInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("APPLY");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(false); // This should trigger failure
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(true);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldThrowWhenRetentionDisabledInProdWithoutWarningFlag")
    void shouldThrowWhenRetentionDisabledInProdWithoutWarningFlag() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(false); // Disabled - should fail without warning flag
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(true);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldNotThrowWhenConfigurationInvalidButWarningFlagEnabled")
    void shouldNotThrowWhenConfigurationInvalidButWarningFlagEnabled() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(false);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(false);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(false);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("true"); // Allow start with warnings
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(false);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert - should not throw when warning flag is enabled
        assertDoesNotThrow(() -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldAcceptBiometricLivenessDisabledInProd")
    void shouldAcceptBiometricLivenessDisabledInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(false); // Liveness disabled - should be accepted

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldRejectBiometricLivenessEnabledWithoutRealProviderInProd")
    void shouldRejectBiometricLivenessEnabledWithoutRealProviderInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(false);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(false);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(true);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldAcceptBiometricLivenessEnabledWithRealProviderInProd")
    void shouldAcceptBiometricLivenessEnabledWithRealProviderInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(false);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(false);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(true);

        ApplicationRunner runner = validator.buildRunner(environment, null, List.of(), realLivenessProvider);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldRejectApplyWhenActiveProcessorDoesNotSupportApply")
    void shouldRejectApplyWhenActiveProcessorDoesNotSupportApply() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("APPLY");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");

        RetentionDomainProcessor processor = mock(RetentionDomainProcessor.class);
        when(processor.supports()).thenReturn(RetentionResourceType.TIME_RECORD);
        when(processor.supportsApply()).thenReturn(false);

        var catalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getActivePolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_TIME_RECORD,
                        "Retenção de registros de ponto",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.TIME_RECORD,
                        1095,
                        RetentionAction.PRESERVE_LEGAL_EVIDENCE,
                        true,
                        true,
                        true,
                        false,
                        false
                ));
            }
        };

        ApplicationRunner runner = validator.buildRunner(environment, catalog, List.of(processor), realLivenessProvider);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldAcceptDryRunModeInProduction")
    void shouldAcceptDryRunModeInProduction() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN"); // DRY_RUN is valid operational state
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(false);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert - DRY_RUN should not fail
        assertDoesNotThrow(() -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldThrowWhenLgpdLogHashSecretMissingInProd")
    void shouldThrowWhenLgpdLogHashSecretMissingInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.log.hash-secret", "")).thenReturn("");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(false);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldThrowWhenLgpdLogHashSecretIsDefaultValueInProd")
    void shouldThrowWhenLgpdLogHashSecretIsDefaultValueInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.log.hash-secret", ""))
                .thenReturn("local-dev-lgpd-log-secret");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(false);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldAcceptValidLgpdLogHashSecretInProd")
    void shouldAcceptValidLgpdLogHashSecretInProd() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("kronos.lgpd.log.hash-secret", ""))
                .thenReturn("real-production-hash-secret-key-12345");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.enabled", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.scheduler.mode", "DRY_RUN"))
                .thenReturn("DRY_RUN");
        when(environment.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("kronos.lgpd.retention.allow-apply", Boolean.class, false))
                .thenReturn(true);
        when(environment.getProperty("LGPD_PRODUCTION_READINESS_ALLOW_START_WITH_WARNINGS", "false"))
                .thenReturn("false");
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(false);

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> runner.run(applicationArguments));
    }

    @Test
    @DisplayName("shouldAcceptFallbackLgpdLogHashSecretInDev")
    void shouldAcceptFallbackLgpdLogHashSecretInDev() throws Exception {
        // Arrange
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(environment.getProperty("kronos.lgpd.log.hash-secret", ""))
                .thenReturn("local-dev-lgpd-log-secret");

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert - dev should skip validation
        assertDoesNotThrow(() -> runner.run(applicationArguments));
    }
}
