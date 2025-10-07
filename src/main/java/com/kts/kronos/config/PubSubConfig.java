package com.kts.kronos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.converter.JacksonPubSubMessageConverter;
import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class PubSubConfig {
    @Value("${pubsub.subscriptions.password-reset}")
    private String passwordResetSubscription;
    @Value("${pubsub.subscriptions.time-record-approval}")
    private String timeRecordApprovalSubscription;

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
    @Bean
    public MessageChannel passwordResetInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel timeRecordApprovalInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter passwordResetMessageChannelAdapter(
            @Qualifier("passwordResetInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, passwordResetSubscription);
        adapter.setOutputChannel(inputChannel);
        return adapter;
    }

    @Bean
    public PubSubInboundChannelAdapter timeRecordApprovalMessageChannelAdapter(
            @Qualifier("timeRecordApprovalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, timeRecordApprovalSubscription);
        adapter.setOutputChannel(inputChannel);
        return adapter;
    }
}
