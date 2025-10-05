package com.kts.kronos.adapter.in.messaging.listener;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import org.springframework.integration.annotation.ServiceActivator;
import com.kts.kronos.adapter.in.messaging.dto.PasswordResetMessage;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetListener {
    private final EmailSenderProvider emailSenderService;

    @ServiceActivator(inputChannel = "passwordResetInputChannel")
    public void handlePasswordResetRequest(PasswordResetMessage payload,
                                           @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {

        log.info("Recebida solicitação de recuperação para o usuário: {}", payload.userName());
        try {
            emailSenderService.sendResetEmail(
                    payload.toEmail(),
                    payload.resetToken(),
                    payload.userName(),
                    payload.frontendBaseUrl()
            );
            log.info("E-mail de recuperação enviado com sucesso para: {}", payload.toEmail());
            message.ack();
        } catch (Exception e) {
            log.error("Erro ao processar a mensagem de recuperação de senha para {}: {}",
                    payload.userName(), e.getMessage(), e);
            message.nack(); // Rejeita a mensagem para nova tentativa
            throw e;
        }
    }
}