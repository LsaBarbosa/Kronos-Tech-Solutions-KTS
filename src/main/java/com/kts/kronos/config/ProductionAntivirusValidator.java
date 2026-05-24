package com.kts.kronos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class ProductionAntivirusValidator {
    private final Environment environment;
    private final boolean isProduction;

    @Value("${upload.antivirus.enabled:false}")
    private boolean antivirusEnabled;

    public ProductionAntivirusValidator(Environment environment) {
        this.environment = environment;
        this.isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateAntivirusConfiguration() {
        if (!isProduction) {
            return;
        }

        if (!antivirusEnabled) {
            var warning = "\n" +
                    "╔════════════════════════════════════════════════════════════════╗\n" +
                    "║  ⚠️  ANTIVIRUS DISABLED IN PRODUCTION                          ║\n" +
                    "╠════════════════════════════════════════════════════════════════╣\n" +
                    "║  Document upload antivirus scanning is DISABLED.              ║\n" +
                    "║                                                               ║\n" +
                    "║  Recommendation for Production:                               ║\n" +
                    "║  Set: UPLOAD_ANTIVIRUS_ENABLED=true                           ║\n" +
                    "║                                                               ║\n" +
                    "║  Impact:                                                      ║\n" +
                    "║  - Malicious files may be uploaded                            ║\n" +
                    "║  - No protection against viruses/malware                      ║\n" +
                    "║  - LGPD data in documents may be at risk                      ║\n" +
                    "║                                                               ║\n" +
                    "║  This is a STRONG RECOMMENDATION, not a requirement.          ║\n" +
                    "║  If intentionally disabled, ensure compensating controls      ║\n" +
                    "║  are in place and documented.                                 ║\n" +
                    "╚════════════════════════════════════════════════════════════════╝\n";

            log.warn(warning);
        } else {
            log.info("✓ Antivirus enabled for document uploads");
        }
    }
}
