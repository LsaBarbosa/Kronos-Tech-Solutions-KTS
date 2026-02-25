package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import static com.kts.kronos.constants.Messages.RESET_PASSWORD_HTML_TEMPLATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderProviderImpl implements EmailSenderProvider {
    private final JavaMailSender mailSender;
    @Value("${mail.username}")
    private String emailRemetente;

    @Override
    public void sendResetEmail(String toEmail, String token, String username, String frontendUrl) {
        log.info("Iniciando envio de e-mail de recuperação via SMTP para: {}", toEmail);

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailRemetente);
            helper.setTo(toEmail);
            helper.setSubject("🔒 Kronos Suporte - Redefinição de Senha");

            var resetLink = frontendUrl + "/?token=" + token;

            // CORREÇÃO: Passando os 4 argumentos que o HTML espera!
            var htmlText = String.format(
                    RESET_PASSWORD_HTML_TEMPLATE,
                    username,    // 1. Olá, %s! (Nome do usuário)
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
        } catch (Exception e) {
            // Este log captura o erro de envio (como o MissingFormatArgumentException original)
            log.error("Falha ao enviar e-mail via SMTP para {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Falha no envio do e-mail de recuperação.", e);
        }
    }
}