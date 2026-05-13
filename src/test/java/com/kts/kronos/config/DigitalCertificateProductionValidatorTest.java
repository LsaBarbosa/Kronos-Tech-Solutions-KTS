package com.kts.kronos.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalCertificateProductionValidatorTest {

    @Mock
    private Environment environment;

    @Mock
    private ApplicationArguments arguments;

    @Test
    void shouldFailStartupWhenProdCertificatePathIsMissing() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        var validator = new DigitalCertificateProductionValidator(environment, "", "secret");

        assertThrows(IllegalStateException.class, () -> validator.run(arguments));
    }

    @Test
    void shouldFailStartupWhenProdCertificatePasswordIsPlaceholder() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        var validator = new DigitalCertificateProductionValidator(
                environment,
                "/etc/kronos/certificate.pfx",
                "sua_senha_secreta"
        );

        assertThrows(IllegalStateException.class, () -> validator.run(arguments));
    }

    @Test
    void shouldAllowStartupWhenProdCertificateConfigIsValid() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        var validator = new DigitalCertificateProductionValidator(
                environment,
                "/etc/kronos/certificate.pfx",
                "strong-secret"
        );

        assertDoesNotThrow(() -> validator.run(arguments));
    }

    @Test
    void shouldNotRequireCertificateConfigOutsideProdProfile() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        var validator = new DigitalCertificateProductionValidator(environment, "", "");

        assertDoesNotThrow(() -> validator.run(arguments));
    }
}
