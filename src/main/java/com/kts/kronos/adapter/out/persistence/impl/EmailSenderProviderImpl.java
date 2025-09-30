package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderProviderImpl implements EmailSenderProvider {
    private final JavaMailSender mailSender;

    @Override
    public void sendResetEmail(String toEmail, String token, String username, String frontendUrl) {
        log.info("Iniciando envio de e-mail de recuperação via SMTP para: {}", toEmail);

        var message = new SimpleMailMessage();

        // Use o e-mail configurado em application.yml
        message.setFrom("kronos.time.tech.solutions@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Kronos - Redefinição de Senha");

        var resetLink = frontendUrl + "/reset-password?token=" + token;
        var text = String.format(
                "Olá %s,\n\n" +
                        "Recebemos uma solicitação para redefinir sua senha.\n" +
                        "Use o link abaixo para criar uma nova senha. O link expira em 30 minutos.\n\n" +
                        "Link: %s\n\n" +
                        "Se você não solicitou a redefinição, ignore este e-mail.",
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
