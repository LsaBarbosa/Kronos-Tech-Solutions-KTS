package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.in.messaging.dto.PasswordResetMessage;
import com.kts.kronos.application.port.out.provider.EmailProducer;
import io.awspring.cloud.sns.core.SnsTemplate; // Novo Import AWS
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnsEmailProducerImpl implements EmailProducer {
    private final SnsTemplate snsTemplate;

    @Value("${password-reset.topic-arn}")
    private String passwordResetTopicArn;

    @Override
    public void sendPasswordResetEmail(PasswordResetMessage message) {
        log.info("Publicando solicitação de recuperação de senha no tópico SNS: {}", passwordResetTopicArn);
        try {
            snsTemplate.convertAndSend(passwordResetTopicArn, message);
            log.info("Mensagem de recuperação publicada com sucesso para o email: {}", message.toEmail());
        } catch (Exception e) {
            log.error("Falha ao publicar mensagem no SNS Topic {}: {}", passwordResetTopicArn, e.getMessage(), e);
            throw new RuntimeException("Falha ao publicar mensagem de recuperação de senha no AWS SNS.", e);
        }
    }
}