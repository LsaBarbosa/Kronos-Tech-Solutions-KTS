package com.kts.kronos.adapter.in.messaging.listener;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.kts.kronos.adapter.in.messaging.dto.PasswordResetMessage;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetListener {
    public static final String PASSWORD_RESET_SUBSCRIPTION = "password-reset-subscription";

    private final EmailSenderProvider emailSenderService;

    @ServiceActivator(inputChannel = PASSWORD_RESET_SUBSCRIPTION + ".input")
    public void handlePasswordResetRequest(
            PasswordResetMessage message,
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage originalMessage) {

        log.info("Recebida solicitação de recuperação para o usuário: {}", message.userName());

        try {
            // 1. Chama o serviço que usa o JavaMailSender para enviar o e-mail
            emailSenderService.sendResetEmail(
                    message.toEmail(),
                    message.resetToken(),
                    message.userName()
            );

            // 2. Confirmação (ACK) - informa ao Pub/Sub que a mensagem foi processada
            originalMessage.ack();
            log.info("E-mail de recuperação enviado com sucesso para: {}", message.toEmail());

        } catch (Exception e) {
            log.error("Erro ao processar a mensagem de recuperação de senha para {}: {}",
                    message.userName(), e.getMessage(), e);
            // 3. Rejeição (NACK) para re-entrega, caso seja um erro transitório
            originalMessage.nack();
        }
    }
}
