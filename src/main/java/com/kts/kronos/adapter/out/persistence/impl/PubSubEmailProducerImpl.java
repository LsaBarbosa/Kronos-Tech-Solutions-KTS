package com.kts.kronos.adapter.out.persistence.impl;


import com.kts.kronos.adapter.in.messaging.dto.PasswordResetMessage;
import com.kts.kronos.application.port.out.provider.EmailProducer;
import com.google.cloud.spring.pubsub.core.PubSubTemplate; // Import CORRETO para GCP Pub/Sub
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PubSubEmailProducerImpl implements EmailProducer {
    // Injeta o PubSubTemplate, que será usado para publicar a mensagem.
    private final PubSubTemplate pubSubTemplate;

    // Injeta o nome do Tópico Pub/Sub (definido em Messages.java e application.yml)
    @Value("${PASSWORD_RESET_TOPIC:password-reset-topic}")
    private String passwordResetTopic;

    @Override
    public void sendPasswordResetEmail(PasswordResetMessage message) {
        log.info("Publicando solicitação de recuperação de senha no tópico Pub/Sub: {}", passwordResetTopic);
        try {
            // Publica o objeto DTO. O JacksonPubSubMessageConverter (de PubSubConfig)
            // serializará o DTO para JSON e o enviará para o tópico.
            pubSubTemplate.publish(passwordResetTopic, message);
            log.info("Mensagem publicada com sucesso para o email: {}", message.toEmail());
        } catch (Exception e) {
            log.error("Falha ao publicar mensagem no Pub/Sub Topic {}: {}", passwordResetTopic, e.getMessage(), e);
        }
    }
}
