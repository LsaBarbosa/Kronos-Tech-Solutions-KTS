package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import static com.kts.kronos.constants.Messages.EMAIL_CONTENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderProviderImpl implements EmailSenderProvider {

    private final JavaMailSender mailSender;
    @Value("${mail.username}")
    private String email;
    @Override
    public void sendResetEmail(String toEmail, String token, String username, String frontendUrl) {
        log.info("Iniciando envio de e-mail de recuperação via SMTP para: {}", toEmail);

        var message = new SimpleMailMessage();

        // Use o e-mail configurado em application.yml
        message.setFrom(email);
        message.setTo(toEmail);
        message.setSubject("Kronos - Redefinição de Senha");

        var resetLink = frontendUrl + "/resetar-senha?token=" + token;
        var text = String.format(
                        EMAIL_CONTENT,
                username, resetLink
        );
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("E-mail de redefinição enviado com sucesso para: {}", toEmail);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail via SMTP para {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Falha no envio do e-mail de recuperação.", e);
        }
    }
}
