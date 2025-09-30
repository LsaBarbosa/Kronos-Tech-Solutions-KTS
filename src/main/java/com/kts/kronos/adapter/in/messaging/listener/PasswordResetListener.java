package com.kts.kronos.adapter.in.messaging.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import com.kts.kronos.adapter.in.messaging.dto.PasswordResetMessage;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetListener {
    private final EmailSenderProvider emailSenderService;

    @SqsListener("${password-reset.queue-name}")
    public void handlePasswordResetRequest(PasswordResetMessage message) {

        log.info("Recebida solicitação de recuperação para o usuário: {}", message.userName());

        try {

            emailSenderService.sendResetEmail(
                    message.toEmail(),
                    message.resetToken(),
                    message.userName(),
                    message.frontendBaseUrl()
            );

            log.info("E-mail de recuperação enviado com sucesso para: {}", message.toEmail());

        } catch (Exception e) {
            log.error("Erro ao processar a mensagem de recuperação de senha para {}: {}",
                    message.userName(), e.getMessage(), e);

            throw e;
        }
    }
}