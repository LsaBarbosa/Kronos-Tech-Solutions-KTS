package com.kts.kronos.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationProdProfileConfigTest {

    @Test
    void shouldDeclareSafeProductionDefaults() {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-prod.yml"));
        var properties = factory.getObject();

        assertEquals("framework", properties.getProperty("server.forward-headers-strategy"));
        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("${FLYWAY_ENABLED:true}", properties.getProperty("spring.flyway.enabled"));
        assertEquals("${MAIL_SMTP_DEBUG:false}", properties.getProperty("spring.mail.properties.mail.smtp.debug"));
        assertEquals("WARN", properties.getProperty("logging.level.org.hibernate.SQL"));
        assertEquals("false", properties.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("false", properties.getProperty("springdoc.api-docs.enabled"));
        assertEquals("${MANAGEMENT_HEALTH_SHOW_DETAILS:never}", properties.getProperty("management.endpoint.health.show-details"));
        assertEquals("${BIOMETRIC_LIVENESS_REQUIRED:false}", properties.getProperty("biometric.liveness-required"));
        assertEquals("${LGPD_LOG_HASH_SECRET}", properties.getProperty("kronos.lgpd.log.hash-secret"));
        assertEquals("${LGPD_RETENTION_SCHEDULER_MODE:DRY_RUN}", properties.getProperty("kronos.lgpd.retention.scheduler.mode"));
        assertEquals("${LGPD_RETENTION_SCHEDULER_APPLY_CONFIRMED:false}", properties.getProperty("kronos.lgpd.retention.scheduler.apply-confirmed"));
        assertEquals("${LGPD_RETENTION_ALLOW_APPLY:false}", properties.getProperty("kronos.lgpd.retention.allow-apply"));
        assertEquals("${DIGITAL_CERTIFICATE_PATH}", properties.getProperty("kronos.security.certificate.path"));
        assertEquals("${DIGITAL_CERTIFICATE_PASSWORD}", properties.getProperty("kronos.security.certificate.password"));
    }
}
