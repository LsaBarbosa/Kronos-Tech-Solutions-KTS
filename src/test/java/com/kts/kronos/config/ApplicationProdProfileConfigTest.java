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
        assertEquals("true", properties.getProperty("spring.flyway.enabled"));
        assertEquals("${MAIL_SMTP_DEBUG:false}", properties.getProperty("spring.mail.properties.mail.smtp.debug"));
        assertEquals("${SPRING_SECURITY_LOG_LEVEL:INFO}", properties.getProperty("logging.level.org.springframework.security"));
        assertEquals("WARN", properties.getProperty("logging.level.org.hibernate.SQL"));
        assertEquals("false", properties.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("false", properties.getProperty("springdoc.api-docs.enabled"));
        assertEquals("health", properties.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("never", properties.getProperty("management.endpoint.health.show-details"));
        assertEquals("${BIOMETRIC_LIVENESS_REQUIRED:true}", properties.getProperty("biometric.liveness-required"));
        assertEquals("${DIGITAL_CERTIFICATE_PATH}", properties.getProperty("kronos.security.certificate.path"));
        assertEquals("${DIGITAL_CERTIFICATE_PASSWORD}", properties.getProperty("kronos.security.certificate.password"));
    }
}
