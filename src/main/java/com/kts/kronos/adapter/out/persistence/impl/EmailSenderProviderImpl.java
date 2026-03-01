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

import static com.kts.kronos.constants.ExceptionMessages.EMAIL_CONFIGURATION_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.EMAIL_REQUIRED_DATA_MISSING;
import static com.kts.kronos.constants.ExceptionMessages.EMAIL_SEND_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.EMAIL_SENDER_CONFIG_MISSING;
import static com.kts.kronos.constants.Logs.LOG_EMAIL_CONFIG_ERROR;
import static com.kts.kronos.constants.Logs.LOG_EMAIL_RESET_START;
import static com.kts.kronos.constants.Logs.LOG_EMAIL_RESET_SUCCESS;
import static com.kts.kronos.constants.Logs.LOG_EMAIL_SEND_ERROR;
import static com.kts.kronos.constants.Messages.RESET_PASSWORD_HTML_TEMPLATE;
import static com.kts.kronos.constants.Messages.RESET_PASSWORD_SUBJECT;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderProviderImpl implements EmailSenderProvider {
    private final JavaMailSender mailSender;
    @Value("${mail.username}")
    private String emailRemetente;

    @Override
    public void sendResetEmail(String toEmail, String token, String username, String frontendUrl) {

        validateRequest(toEmail, token, username, frontendUrl);
        log.info(LOG_EMAIL_RESET_START, toEmail);

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
            log.info(LOG_EMAIL_RESET_SUCCESS, toEmail);
        } catch (MessagingException e) {
            log.error(LOG_EMAIL_CONFIG_ERROR, toEmail, e.getMessage(), e);
            throw new RuntimeException(EMAIL_CONFIGURATION_ERROR, e);
        } catch (MailException e) {
            log.error(LOG_EMAIL_SEND_ERROR, toEmail, e.getMessage(), e);
            throw new RuntimeException(EMAIL_SEND_ERROR, e);
        }
    }

    private void validateRequest(String toEmail, String token, String username, String frontendUrl) {
        if (isBlank(toEmail) || isBlank(token) || isBlank(username) || isBlank(frontendUrl)) {
            throw new IllegalArgumentException(EMAIL_REQUIRED_DATA_MISSING);
        }

        if (Objects.isNull(emailRemetente) || emailRemetente.isBlank()) {
            throw new IllegalStateException(EMAIL_SENDER_CONFIG_MISSING);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}