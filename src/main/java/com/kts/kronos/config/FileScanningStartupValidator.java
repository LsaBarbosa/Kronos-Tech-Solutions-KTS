package com.kts.kronos.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class FileScanningStartupValidator {

    private final Environment environment;

    @Value("${kronos.security.upload.antivirus.enabled:false}")
    private boolean fileScanningEnabled;

    public FileScanningStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        boolean isProduction = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod")
                        || profile.equalsIgnoreCase("production"));

        if (isProduction && !fileScanningEnabled) {
            log.error("event=startup_validation result=failure reason=file_scanning_disabled_in_production");
            throw new IllegalStateException("File scanning must be enabled in production.");
        }

        if (isProduction) {
            log.info("event=startup_validation result=success component=file_scanning status=enabled");
        }
    }
}
