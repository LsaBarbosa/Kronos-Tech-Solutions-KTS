package com.kts.kronos.config;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class PubSubConfig {
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
                new PubSubInboundChannelAdapter(pubSubTemplate, "${pubsub.subscriptions.password-reset}");
        adapter.setOutputChannel(inputChannel);
        return adapter;
    }

    @Bean
    public PubSubInboundChannelAdapter timeRecordApprovalMessageChannelAdapter(
            @Qualifier("timeRecordApprovalInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, "${pubsub.subscriptions.time-record-approval}");
        adapter.setOutputChannel(inputChannel);
        return adapter;
    }
}
