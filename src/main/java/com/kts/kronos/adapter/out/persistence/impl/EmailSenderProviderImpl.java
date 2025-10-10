package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
                        "<!DOCTYPE html>" +
                                "<html lang='pt-BR'>" +
                                "<head><meta charset='UTF-8'></head>" +
                                "<body style='font-family: Arial, sans-serif; color: #333; line-height: 1.6;'>" +

                                // APRESENTAÇÃO
                                "<h1 style='color: #0056b3; font-size: 24px;'>Olá, %s! Sua Segurança é Nossa Prioridade.</h1>" +
                                "<hr style='border: 0; border-top: 1px solid #eee;'>" +

                                // CORPO
                                "<p style='font-size: 16px;'>Recebemos uma solicitação para **redefinir a senha** da sua conta.</p>" +
                                "<p style='font-size: 16px;'>Para criar uma nova senha, clique no botão abaixo. Se você não consegue clicar, copie e cole o link no seu navegador.</p>" +

                                // DESTAQUE E BOTÃO
                                "<p style='margin: 25px 0;'>" +
                                // Botão com fundo azul, texto branco, fonte maior e negrito
                                "<a href='%s' target='_blank' style='" +
                                "display: inline-block; " +
                                "padding: 12px 25px; " +
                                "background-color: #007bff; " +
                                "color: #ffffff; " +
                                "text-decoration: none; " +
                                "border-radius: 5px; " +
                                "font-size: 18px; " +
                                "font-weight: bold;" +
                                "'>Criar Nova Senha</a>" +
                                "</p>" +

                                // INFORMAÇÃO DE EXPIRAÇÃO (ALERTA EM LARANJA)
                                "<p style='font-size: 14px; color: #ff9900; margin-top: 20px;'>" +
                                "**Lembre-se:** Este link de redefinição **expira em 30 minutos** por questões de segurança." +
                                "</p>" +

                                // CLÁUSULA DE SEGURANÇA
                                "<p style='font-size: 14px; color: #666; margin-top: 30px;'>" +
                                "Se você **não solicitou** a redefinição de senha, por favor, **ignore** este e-mail. Nenhuma ação será tomada na sua conta." +
                                "</p>" +

                                // FECHAMENTO
                                "<p style='font-size: 14px; margin-top: 40px;'>Atenciosamente,<br>O Time de Suporte [Nome da Sua Empresa]</p>" +

                                "</body>" +
                                "</html>",
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
