package com.kts.kronos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CertificateCryptoProperties.class)
public class CryptoConfig {

    @Bean
    CertificateCryptoPropertiesValidator certificateCryptoPropertiesValidator(
            CertificateCryptoProperties properties,
            Environment environment
    ) {
        return new CertificateCryptoPropertiesValidator(properties, environment);
    }
}
