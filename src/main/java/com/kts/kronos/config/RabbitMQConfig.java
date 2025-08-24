package com.kts.kronos.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
    @Bean
    public MessageConverter jsonMessageConverter() {
        // A classe TimeRecordChangeRequestMessage não é uma entidade que deve ser desserializada diretamente, mas
        // sim um DTO para mensageria. A implementação atual usa o SerializedMessageConverter, o que causa o erro.
        // A abordagem mais robusta e segura é usar um conversor que trabalha com JSON,
        // que é um formato mais universal e menos propenso a falhas de segurança de desserialização.
        // O Jackson2JsonMessageConverter do Spring AMQP é uma ótima escolha.
        return new Jackson2JsonMessageConverter();
    }
}
