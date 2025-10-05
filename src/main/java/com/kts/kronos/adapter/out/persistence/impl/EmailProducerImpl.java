package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.in.messaging.dto.PasswordResetMessage;
import com.kts.kronos.application.port.out.provider.EmailProducer;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailProducerImpl implements EmailProducer {

    private final PubSubTemplate pubSubTemplate;

    @Value("${pubsub.topics.password-reset}")
    private String passwordResetTopic;

    @Override
    public void sendPasswordResetEmail(PasswordResetMessage message) {
        log.info("Publicando solicitação de recuperação de senha no tópico Pub/Sub: {}", passwordResetTopic);
        try {
            pubSubTemplate.publish(passwordResetTopic, message);
            log.info("Mensagem de recuperação publicada com sucesso para o email: {}", message.toEmail());
        } catch (Exception e) {
            log.error("Falha ao publicar mensagem no Pub/Sub Topic {}: {}", passwordResetTopic, e.getMessage(), e);
            throw new RuntimeException("Falha ao publicar mensagem de recuperação de senha no Google Cloud Pub/Sub.", e);
        }
    }
}