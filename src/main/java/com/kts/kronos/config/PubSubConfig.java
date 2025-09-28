package com.kts.kronos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.converter.JacksonPubSubMessageConverter;
import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PubSubConfig {
    private final ObjectMapper objectMapper;

    // A ObjectMapper já deve estar disponível (do RedisConfig)
    public PubSubConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Define um conversor de mensagens personalizado para usar JSON (Jackson).
     * Isso resolve o erro 'Unable to convert payload... to byte[]'.
     */
    @Bean
    public PubSubMessageConverter pubSubMessageConverter() {
        return new JacksonPubSubMessageConverter(objectMapper);
    }
}
