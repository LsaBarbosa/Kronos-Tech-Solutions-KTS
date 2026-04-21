package com.kts.kronos.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "kronos.security.certificate")
public record CertificateCryptoProperties(
        @NotBlank(message = "KRONOS_CERTIFICATE_PATH deve ser configurado")
        String path,

        @NotBlank(message = "KRONOS_CERTIFICATE_PASSWORD deve ser configurado")
        String password
) {
    public Path pathAsPath() {
        return Path.of(path);
    }

    public char[] passwordAsChars() {
        return password.toCharArray();
    }
}
