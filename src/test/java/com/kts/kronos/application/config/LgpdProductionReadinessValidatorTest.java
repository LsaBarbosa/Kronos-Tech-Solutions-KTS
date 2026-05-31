package com.kts.kronos.application.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LgpdProductionReadinessValidatorTest {

    private LgpdProductionReadinessValidator validator;
    private Environment environment;
    private ApplicationArguments applicationArguments;

    @BeforeEach
    void setUp() {
        validator = new LgpdProductionReadinessValidator();
        environment = mock(Environment.class);
        applicationArguments = mock(ApplicationArguments.class);
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
    @DisplayName("shouldAcceptBiometricLivenessEnabledInProd")
    void shouldAcceptBiometricLivenessEnabledInProd() throws Exception {
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
        when(environment.getProperty("biometric.liveness-required", Boolean.class, false))
                .thenReturn(true); // Liveness enabled - should also be accepted

        ApplicationRunner runner = validator.validateLgpdProduction(environment);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> runner.run(applicationArguments));
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
}
