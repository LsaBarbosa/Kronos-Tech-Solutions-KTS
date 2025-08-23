package com.kts.kronos.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.kts.kronos.constants.Messages.*;

@Configuration
public class RabbitMQConfig {


    @Bean
    public Queue queue() {
        return new Queue(TIME_RECORD_CHANGE_QUEUE, true); // durable queue
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(TIME_RECORD_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}
