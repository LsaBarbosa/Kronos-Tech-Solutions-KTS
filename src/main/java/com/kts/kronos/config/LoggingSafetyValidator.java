package com.kts.kronos.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

final class LoggingSafetyValidator implements InitializingBean {

    private static final Set<String> LOCAL_PROFILES = Set.of("dev", "local", "test");
    private static final Set<String> UNSAFE_LOG_LEVELS = Set.of("debug", "trace");
    private static final String SECURITY_LOG_LEVEL = "logging.level.org.springframework.security";
    private static final String SECURITY_WEB_LOG_LEVEL = "logging.level.org.springframework.security.web";
    private static final String SMTP_DEBUG = "spring.mail.properties.mail.smtp.debug";

    private final Environment environment;

    LoggingSafetyValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (isLocalProfile()) {
            return;
        }

        rejectUnsafeSecurityLogging(SECURITY_LOG_LEVEL);
        rejectUnsafeSecurityLogging(SECURITY_WEB_LOG_LEVEL);
        rejectSmtpDebug();
    }

    private void rejectUnsafeSecurityLogging(String propertyName) {
        String configuredLevel = environment.getProperty(propertyName);
        if (configuredLevel != null && UNSAFE_LOG_LEVELS.contains(configuredLevel.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(propertyName + " não pode usar DEBUG/TRACE fora de ambiente local");
        }
    }

    private void rejectSmtpDebug() {
        Boolean smtpDebug = environment.getProperty(SMTP_DEBUG, Boolean.class);
        if (Boolean.TRUE.equals(smtpDebug)) {
            throw new IllegalStateException(SMTP_DEBUG + " não pode ser true fora de ambiente local");
        }
    }

    private boolean isLocalProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(LOCAL_PROFILES::contains);
    }
}
