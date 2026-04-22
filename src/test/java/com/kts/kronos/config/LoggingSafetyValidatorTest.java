package com.kts.kronos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingSafetyValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LoggingSafetyConfig.class);

    @Test
    void shouldAllowSafeDefaultLoggingOutsideLocalProfile() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "logging.level.org.springframework.security=WARN",
                        "spring.mail.properties.mail.smtp.debug=false"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldFailOutsideLocalProfileWhenSpringSecurityDebugIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "logging.level.org.springframework.security=DEBUG"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()).getMessage())
                            .contains("logging.level.org.springframework.security não pode usar DEBUG/TRACE");
                });
    }

    @Test
    void shouldFailOutsideLocalProfileWhenSmtpDebugIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "spring.mail.properties.mail.smtp.debug=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()).getMessage())
                            .contains("spring.mail.properties.mail.smtp.debug não pode ser true");
                });
    }

    @Test
    void shouldAllowDiagnosticLoggingInLocalProfile() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "logging.level.org.springframework.security=DEBUG",
                        "spring.mail.properties.mail.smtp.debug=true"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    private Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
