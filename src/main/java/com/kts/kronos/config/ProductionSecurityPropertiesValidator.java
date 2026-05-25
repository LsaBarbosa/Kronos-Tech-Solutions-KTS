package com.kts.kronos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@Component
@Slf4j
public class ProductionSecurityPropertiesValidator {
    private final Environment environment;
    private final boolean isProduction;

    @Value("${kronos.security.auth-cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${frontend.allowed-origins:*}")
    private String corsAllowedOrigins;

    // Note: HttpOnly is hardcoded in AuthCookieService.baseCookie() - no configuration needed

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerEnabled;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:0}")
    private int hibernateBatchSize;

    @Value("${kronos.security.upload.antivirus.enabled:false}")
    private boolean antivirusEnabled;

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
        validateAntivirus();

        log.info("Production security validation completed successfully");
    }

    private void validateCookieSecurity() {
        if (!cookieSecure) {
            var error = "SECURITY ERROR: Authentication cookies are not secure in production. " +
                    "Set kronos.security.auth-cookie.secure=true";
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.info("✓ Cookie security validated (HttpOnly is guaranteed by AuthCookieService)");
    }

    private void validateCORS() {
        if (!isProduction) {
            log.info("✓ CORS validation skipped: not running in prod profile");
            return;
        }

        if (corsAllowedOrigins == null || corsAllowedOrigins.trim().isEmpty()) {
            var error = "SECURITY ERROR: frontend.allowed-origins is empty in production. " +
                    "Set it to specific HTTPS domains";
            log.error(error);
            throw new IllegalStateException(error);
        }

        String[] origins = corsAllowedOrigins.split(",");
        for (String origin : origins) {
            origin = origin.trim();

            if (origin.isEmpty()) {
                var error = "SECURITY ERROR: empty origin found in frontend.allowed-origins. " +
                        "Remove empty values from the list";
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (origin.contains("*")) {
                var error = "SECURITY ERROR: wildcard (*) found in CORS origin: " + origin + ". " +
                        "Use specific HTTPS domains only";
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (origin.contains(" ")) {
                var error = "SECURITY ERROR: origin contains spaces: " + origin + ". " +
                        "Remove all spaces from CORS origins";
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (!origin.startsWith("https://")) {
                var error = "SECURITY ERROR: origin does not use HTTPS: " + origin + ". " +
                        "All production CORS origins must use HTTPS";
                log.error(error);
                throw new IllegalStateException(error);
            }

            validateOriginFormat(origin);
        }

        log.info("✓ CORS configuration validated");
    }

    private void validateOriginFormat(String origin) {
        try {
            URL url = new URL(origin);

            if (url.getPath() != null && !url.getPath().isEmpty() && !url.getPath().equals("/")) {
                var error = "SECURITY ERROR: CORS origin contains path: " + origin + ". " +
                        "Origins must be scheme://host[:port] only, without path";
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (url.getQuery() != null && !url.getQuery().isEmpty()) {
                var error = "SECURITY ERROR: CORS origin contains query string: " + origin + ". " +
                        "Origins must not contain query parameters";
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (url.getRef() != null && !url.getRef().isEmpty()) {
                var error = "SECURITY ERROR: CORS origin contains fragment: " + origin + ". " +
                        "Origins must not contain fragments";
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (url.getHost() == null || url.getHost().isEmpty()) {
                var error = "SECURITY ERROR: CORS origin has no valid host: " + origin;
                log.error(error);
                throw new IllegalStateException(error);
            }
        } catch (MalformedURLException e) {
            var error = "SECURITY ERROR: malformed CORS origin: " + origin + ". " +
                    "Must be a valid URL in format https://host[:port]";
            log.error(error);
            throw new IllegalStateException(error, e);
        }
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
        String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.length() < 32) {
            var error = "SECURITY ERROR: JWT secret is not configured or too short in production. " +
                    "Set jwt.secret to a string with at least 32 characters";
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.info("✓ JWT secret configured");
    }

    private void validateAwsCredentials() {
        String awsAccessKey = environment.getProperty("aws.access-key-id");
        String awsSecretKey = environment.getProperty("aws.secret-access-key");
        String awsRegion = environment.getProperty("aws.region");

        if (awsRegion == null || awsRegion.isEmpty()) {
            var error = "SECURITY ERROR: AWS region is not configured in production. " +
                    "Set aws.region (required for S3 and Rekognition)";
            log.error(error);
            throw new IllegalStateException(error);
        }

        boolean hasAccessKey = awsAccessKey != null && !awsAccessKey.isEmpty();
        boolean hasSecretKey = awsSecretKey != null && !awsSecretKey.isEmpty();

        if (hasAccessKey && hasSecretKey) {
            log.info("✓ AWS credentials mode: Static credentials (access-key-id + secret-access-key)");
        } else if (!hasAccessKey && !hasSecretKey) {
            log.info("✓ AWS credentials mode: IAM Role (will use instance profile, ECS task role, or web identity)");
        } else {
            var error = "SECURITY ERROR: AWS credentials are incomplete. " +
                    "Either provide both aws.access-key-id and aws.secret-access-key, or use IAM Role (provide neither)";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void validateActuatorEndpoints() {
        String actuatorExposure = environment.getProperty("management.endpoints.web.exposure.include", "");

        String[] sensitiveEndpoints = {"*", "env", "heapdump", "beans", "configprops", "threaddump", "flyway", "logfile", "loggers"};

        for (String endpoint : sensitiveEndpoints) {
            if (actuatorExposure.contains(endpoint)) {
                var error = "SECURITY ERROR: Sensitive Actuator endpoint '" + endpoint + "' is exposed in production. " +
                        "Remove from management.endpoints.web.exposure.include";
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        log.info("✓ Actuator endpoints validation completed");
    }

    private void validateAntivirus() {
        if (!antivirusEnabled) {
            var error = "SECURITY ERROR: Antivirus protection is disabled in production. " +
                    "Set kronos.security.upload.antivirus.enabled=true";
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.info("✓ Antivirus protection enabled");
    }
}
