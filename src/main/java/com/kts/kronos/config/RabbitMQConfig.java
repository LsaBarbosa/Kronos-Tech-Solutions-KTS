package com.kts.kronos.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.kts.kronos.constants.Messages.*;


@Configuration
public class RabbitMQConfig {
    @Bean
    public Exchange approvalExchange() {
        // Cria um Topic Exchange (Flexível para roteamento futuro)
        return ExchangeBuilder
                .topicExchange(TIME_RECORD_APPROVAL_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue approvalQueue() {
        return QueueBuilder
                .durable(TIME_RECORD_APPROVAL_QUEUE)
                .build();
    }

    @Bean
    public Binding approvalBinding(Queue approvalQueue, Exchange approvalExchange) {
        // Liga a fila ao exchange com uma Routing Key
        return BindingBuilder
                .bind(approvalQueue)
                .to(approvalExchange)
                .with(TIME_RECORD_APPROVAL_ROUTING_KEY)
                .noargs();
    }
}
