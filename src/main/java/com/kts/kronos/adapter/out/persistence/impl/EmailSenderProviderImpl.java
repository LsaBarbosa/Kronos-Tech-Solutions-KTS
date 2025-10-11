package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

        MimeMessage message = mailSender.createMimeMessage();

        try {
            // 2. Usa MimeMessageHelper (o 'true' no construtor habilita o modo multipart/HTML)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Use o e-mail configurado em application.yml
            helper.setFrom(email);
            helper.setTo(toEmail);
            helper.setSubject("Kronos Suporte - Redefinição de Senha");

            var resetLink = frontendUrl + "/resetar-senha?token=" + token;

            // Mantendo a string HTML, mas agora ela será interpretada corretamente
            var htmlText = String.format(
                    "<!DOCTYPE html>" +
                            "<html lang='pt-BR'>" +
                            "<head><meta charset='UTF-8'></head>" +
                            "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; text-align: center;'>" +

                            // CONTAINER PRINCIPAL
                            "<div>" +

                            // APRESENTAÇÃO
                            "<h1 style='color: #1a73e8; font-size: 26px; border-bottom: 2px solid #eee; padding-bottom: 10px;'>👋 Olá, %s! Sua Segurança é Nossa Prioridade!</h1>" +

                            // CORPO
                            "<p style='font-size: 16px; color: #333;'>Para prosseguir e criar uma nova senha, é só clicar no botão azul logo abaixo. É rápido e fácil!</p>" +

                            // DESTAQUE E BOTÃO
                            "<p style='margin: 30px 0; text-align: center;'>" +
                            // Botão com fundo azul, texto branco, fonte maior e negrito
                            "<a href='%s' target='_blank' style='" +
                            "display: inline-block; " +
                            "padding: 15px 30px; " +
                            "background-color: #1a73e8; " + // Cor Azul Profundo
                            "color: #ffffff; " +
                            "text-decoration: none; " +
                            "border-radius: 50px; " + // Borda arredondada para um visual moderno
                            "font-size: 18px; " +
                            "font-weight: bold; " +
                            "border: 1px solid #1a73e8; " +
                            "box-shadow: 0 2px 4px rgba(0,0,0,0.2);" +
                            "'>🔒 CLIQUE AQUI PARA NOVA SENHA</a>" +
                            "</p>" +

                            // INFORMAÇÃO DE EXPIRAÇÃO (ALERTA EM LARANJA)
                            "<p style='font-size: 14px; color: #666; text-align: center;'>Se o botão não funcionar, copie e cole o link abaixo em seu navegador:<br>" +
                            "<a href='%s' style='color: #1a73e8; word-break: break-all;'>%s</a></p>" +

                            // CLÁUSULA DE SEGURANÇA

                            "<p style='font-size: 15px; color: #ff9900; margin: 0;'>" +
                            "&#x26A0;&#xFE0F; Atenção:Este link de redefinição e expira em **30 minutos por motivos de segurança." +
                            "</p>" +

                            "<p style='font-size: 14px; color: #1e8449; margin-top: 30px; border-top: 1px solid #eee; padding-top: 15px;'>" +
                            "***Não solicitou esta redefinição?*** *Relaxe!* Se você não fez esta solicitação, **pode simplesmente ignorar este e-mail**. Sua senha antiga permanecerá segura e nenhuma alteração será feita na sua conta." +
                            "</p>" +

                            // FECHAMENTO
                            "<p style='font-size: 16px; color: #333; margin-top: 40px;'>Conte sempre conosco para manter sua conta segura!</p>" +
                            "<p style='font-size: 14px; color: #555;'>Atenciosamente,<br>O Time de Suporte *[Nome da Sua Empresa]*</p>" +
                            "</div>"+
                            "</body>" +
                            "</html>",
                    username, resetLink
            );

            // 3. Seta o conteúdo HTML, o 'true' no segundo parâmetro é CRÍTICO.
            helper.setText(htmlText, true);

            mailSender.send(message);
            log.info("E-mail de redefinição enviado com sucesso para: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Falha ao configurar MimeMessage para {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Falha na configuração do e-mail de recuperação.", e);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail via SMTP para {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Falha no envio do e-mail de recuperação.", e);
        }
    }
}
