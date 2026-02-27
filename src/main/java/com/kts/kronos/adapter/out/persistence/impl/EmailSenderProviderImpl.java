package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

import static com.kts.kronos.constants.Messages.RESET_PASSWORD_HTML_TEMPLATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderProviderImpl implements EmailSenderProvider {
    private static final String RESET_PASSWORD_SUBJECT = "🔒 Kronos Suporte - Redefinição de Senha";
    private final JavaMailSender mailSender;
    @Value("${mail.username}")
    private String emailRemetente;

    @Override
    public void sendResetEmail(String toEmail, String token, String username, String frontendUrl) {

        validateRequest(toEmail, token, username, frontendUrl);
        log.info("Iniciando envio de e-mail de recuperação via SMTP para: {}", toEmail);

        var message = mailSender.createMimeMessage();

        try {
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(emailRemetente);
            helper.setTo(toEmail);
            helper.setSubject(RESET_PASSWORD_SUBJECT);

            var resetLink = UriComponentsBuilder.fromUriString(frontendUrl)
                    .queryParam("token", token).build().toUriString();

            // CORREÇÃO: Passando os 4 argumentos que o HTML espera!
            var htmlText = String.format(RESET_PASSWORD_HTML_TEMPLATE, username,    // 1. Olá, %s! (Nome do usuário)
                    resetLink,   // 2. Link do botão (%s)
                    resetLink,   // 3. Link do fallback no href (%s)
                    resetLink    // 4. Texto do link de fallback (%s)
            );

            // Seta o conteúdo HTML
            helper.setText(htmlText, true);

            mailSender.send(message);
            log.info("E-mail de redefinição enviado com sucesso para: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Falha ao configurar MimeMessage para {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Falha na configuração do e-mail de recuperação.", e);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail via SMTP para {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Falha no envio do e-mail de recuperação.", e);
        }
    }

    private void validateRequest(String toEmail, String token, String username, String frontendUrl) {
        if (isBlank(toEmail) || isBlank(token) || isBlank(username) || isBlank(frontendUrl)) {
            throw new IllegalArgumentException("Dados obrigatórios para envio de e-mail não informados.");
        }

        if (Objects.isNull(emailRemetente) || emailRemetente.isBlank()) {
            throw new IllegalStateException("Configuração de remetente de e-mail (mail.username) está ausente.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}