package com.kts.kronos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class LoggingSafetyConfig {

    @Bean
    LoggingSafetyValidator loggingSafetyValidator(Environment environment) {
        return new LoggingSafetyValidator(environment);
    }
}
