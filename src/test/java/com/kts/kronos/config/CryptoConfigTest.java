package com.kts.kronos.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CryptoConfigTest {

    @Test
    void shouldCreateCertificateValidatorBean() {
        var config = new CryptoConfig();
        var properties = new CertificateCryptoProperties("/tmp/test-cert.pfx", "test-password");

        assertNotNull(config.certificateCryptoPropertiesValidator(properties, new MockEnvironment()));
    }
}
