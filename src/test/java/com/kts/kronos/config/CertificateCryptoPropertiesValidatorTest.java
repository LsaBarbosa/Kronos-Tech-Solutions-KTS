package com.kts.kronos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificateCryptoPropertiesValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CryptoConfig.class);

    @Test
    void shouldFailApplicationContextWhenCertificatePasswordIsMissing() {
        contextRunner
                .withPropertyValues("kronos.security.certificate.path=/opt/kronos/certs/signing.p12")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()).getMessage())
                            .contains("KRONOS_CERTIFICATE_PASSWORD deve ser configurado");
                });
    }

    @Test
    void shouldFailWhenCertificatePathIsMissing() {
        var properties = new CertificateCryptoProperties("", "strong-password-value");
        var validator = new CertificateCryptoPropertiesValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KRONOS_CERTIFICATE_PATH deve ser configurado explicitamente");
    }

    @Test
    void shouldFailWhenCertificatePasswordUsesInsecureFallbackOutsideLocalProfile() {
        var properties = new CertificateCryptoProperties("/opt/kronos/certs/signing.p12", "password");
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var validator = new CertificateCryptoPropertiesValidator(properties, environment);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KRONOS_CERTIFICATE_PASSWORD não pode usar senha padrão");
    }

    @Test
    void shouldAllowTestProfileToUseExplicitTestCertificateSettings() {
        var properties = new CertificateCryptoProperties("/tmp/test-cert.pfx", "test-password");
        var environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        var validator = new CertificateCryptoPropertiesValidator(properties, environment);

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    private Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
