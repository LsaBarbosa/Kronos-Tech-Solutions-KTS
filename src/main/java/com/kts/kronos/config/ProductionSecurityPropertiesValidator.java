package com.kts.kronos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class ProductionSecurityPropertiesValidator {
    private final Environment environment;
    private final boolean isProduction;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${spring.security.cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @Value("${server.servlet.session.cookie.http-only:false}")
    private boolean cookieHttpOnly;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerEnabled;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:0}")
    private int hibernateBatchSize;

    public ProductionSecurityPropertiesValidator(Environment environment) {
        this.environment = environment;
        this.isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionConfiguration() {
        if (!isProduction) {
            log.debug("Production validation skipped: not running in prod profile");
            return;
        }

        log.info("Validating production security configuration...");

        validateCookieSecurity();
        validateCORS();
        validateSwagger();
        validateJwtSecret();
        validateAwsCredentials();
        validateActuatorEndpoints();

        log.info("Production security validation completed successfully");
    }

    private void validateCookieSecurity() {
        if (!cookieSecure) {
            var error = "SECURITY ERROR: Authentication cookies are not secure in production. " +
                    "Set server.servlet.session.cookie.secure=true";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (!cookieHttpOnly) {
            var warning = "WARNING: Authentication cookies are not HTTP-only in production. " +
                    "Set server.servlet.session.cookie.http-only=true recommended";
            log.warn(warning);
        }

        log.info("✓ Cookie security validated");
    }

    private void validateCORS() {
        if (corsAllowedOrigins != null && corsAllowedOrigins.contains("*")) {
            var error = "SECURITY ERROR: CORS is configured with wildcard (*) in production. " +
                    "Set spring.security.cors.allowed-origins to specific domains";
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.info("✓ CORS configuration validated");
    }

    private void validateSwagger() {
        if (swaggerEnabled) {
            var error = "SECURITY ERROR: Swagger UI is enabled in production. " +
                    "Set springdoc.swagger-ui.enabled=false";
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.info("✓ Swagger disabled in production");
    }

    private void validateJwtSecret() {
        String jwtSecret = environment.getProperty("app.jwt.secret");
        if (jwtSecret == null || jwtSecret.length() < 32) {
            var error = "SECURITY ERROR: JWT secret is not configured or too short in production. " +
                    "Set app.jwt.secret to a string with at least 32 characters";
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.info("✓ JWT secret configured");
    }

    private void validateAwsCredentials() {
        String awsAccessKey = environment.getProperty("aws.accessKeyId");
        String awsSecretKey = environment.getProperty("aws.secretAccessKey");
        String awsRegion = environment.getProperty("aws.region");

        if (awsAccessKey == null || awsAccessKey.isEmpty()) {
            log.warn("WARNING: AWS access key ID not configured. S3 operations may fail");
        }

        if (awsSecretKey == null || awsSecretKey.isEmpty()) {
            log.warn("WARNING: AWS secret access key not configured. S3 operations may fail");
        }

        if (awsRegion == null || awsRegion.isEmpty()) {
            log.warn("WARNING: AWS region not configured. Using default region");
        }

        if ((awsAccessKey != null && !awsAccessKey.isEmpty()) &&
            (awsSecretKey != null && !awsSecretKey.isEmpty())) {
            log.info("✓ AWS credentials configured");
        }
    }

    private void validateActuatorEndpoints() {
        String actuatorExposure = environment.getProperty("management.endpoints.web.exposure.include", "");

        if (actuatorExposure.contains("env") || actuatorExposure.contains("*")) {
            log.warn("WARNING: Actuator /env endpoint is exposed. Remove from management.endpoints.web.exposure.include");
        }

        if (actuatorExposure.contains("heapdump")) {
            log.warn("WARNING: Actuator /heapdump endpoint is exposed. Remove from management.endpoints.web.exposure.include");
        }

        if (actuatorExposure.contains("configprops")) {
            log.warn("WARNING: Actuator /configprops endpoint is exposed. Remove from management.endpoints.web.exposure.include");
        }

        log.info("✓ Actuator endpoints validation completed");
    }
}
