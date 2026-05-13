package com.kts.kronos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class DigitalCertificateProductionValidator implements ApplicationRunner {

    private static final Set<String> INVALID_PLACEHOLDERS = Set.of(
            "caminho/para/certificado.pfx",
            "sua_senha_secreta"
    );

    private final Environment environment;
    private final String certificatePath;
    private final String certificatePassword;

    public DigitalCertificateProductionValidator(
            Environment environment,
            @Value("${kronos.security.certificate.path:}") String certificatePath,
            @Value("${kronos.security.certificate.password:}") String certificatePassword
    ) {
        this.environment = environment;
        this.certificatePath = certificatePath;
        this.certificatePassword = certificatePassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }

        requireValid("DIGITAL_CERTIFICATE_PATH", certificatePath);
        requireValid("DIGITAL_CERTIFICATE_PASSWORD", certificatePassword);
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }

    private void requireValid(String propertyName, String value) {
        if (value == null || value.isBlank() || INVALID_PLACEHOLDERS.contains(value.trim())) {
            throw new IllegalStateException(propertyName + " deve ser configurado para o profile prod.");
        }
    }
}
