package com.kts.kronos.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

final class CertificateCryptoPropertiesValidator implements InitializingBean {

    private static final Set<String> LOCAL_PROFILES = Set.of("dev", "local", "test");
    private static final Set<String> INSECURE_PATH_VALUES = Set.of(
            "certificado.pfx",
            "certificate.pfx",
            "test-cert.pfx",
            "/tmp/test-cert.pfx"
    );
    private static final Set<String> INSECURE_PASSWORD_VALUES = Set.of(
            "password",
            "secret",
            "changeit",
            "test-password",
            "123456",
            "admin"
    );

    private final CertificateCryptoProperties properties;
    private final Environment environment;

    CertificateCryptoPropertiesValidator(CertificateCryptoProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        validateConfigured("KRONOS_CERTIFICATE_PATH", properties.path());
        validateConfigured("KRONOS_CERTIFICATE_PASSWORD", properties.password());

        if (isLocalProfile()) {
            return;
        }

        if (isInsecurePath(properties.path())) {
            throw new IllegalStateException(
                    "KRONOS_CERTIFICATE_PATH não pode usar valor padrão, exemplo ou caminho de teste fora de ambiente local"
            );
        }

        if (isInsecurePassword(properties.password())) {
            throw new IllegalStateException(
                    "KRONOS_CERTIFICATE_PASSWORD não pode usar senha padrão, exemplo ou valor de teste fora de ambiente local"
            );
        }

        if (!isRegularCertificateFile()) {
            throw new IllegalStateException(
                    "KRONOS_CERTIFICATE_PATH deve apontar para um certificado PKCS#12 existente fora de ambiente local"
            );
        }
    }

    private void validateConfigured(String variableName, String value) {
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalStateException(variableName + " deve ser configurado explicitamente");
        }
    }

    private boolean isLocalProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(LOCAL_PROFILES::contains);
    }

    private boolean isInsecurePath(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return INSECURE_PATH_VALUES.contains(normalized)
                || normalized.startsWith("caminho/")
                || normalized.startsWith("/tmp/")
                || normalized.endsWith("/test-cert.pfx");
    }

    private boolean isInsecurePassword(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return INSECURE_PASSWORD_VALUES.contains(normalized)
                || normalized.contains("secret")
                || normalized.contains("senha");
    }

    private boolean isRegularCertificateFile() {
        try {
            return Files.isRegularFile(properties.pathAsPath());
        } catch (InvalidPathException ex) {
            return false;
        }
    }
}
