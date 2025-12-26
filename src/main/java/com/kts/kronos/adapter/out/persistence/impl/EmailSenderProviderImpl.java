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
    @Value("${spring.mail.username}")
    private String emailRemetente; // Renomeado para maior clareza

    // Utilizando o Bloco de Texto (Text Block) do Java 15+ para código HTML limpo.
    // Ele contém 4 placeholders: %s (Nome), %s (Link Botão), %s (Link Fallback), %s (Texto Fallback)
    private static final String RESET_PASSWORD_HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html lang='pt-BR'>
        <head>
        <meta charset='UTF-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
        </head>
        <body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; text-align: center;'>

        <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); text-align: left;'>

            <h1 style='color: #1a73e8; font-size: 26px; border-bottom: 2px solid #eee; padding-bottom: 10px;'>👋 Olá, %s! Sua Segurança é Nossa Prioridade!</h1>

            <p style='font-size: 18px; color: #333;'>Esperamos que esteja tudo bem.</p>
        
            <p style='font-size: 16px; color: #333;'> Recebemos uma solicitação para redefinir a senha da sua conta.</p>
            <p style='font-size: 16px; color: #333;'>Para prosseguir e criar uma nova senha, é só clicar no botão azul logo abaixo. Rápido e fácil!</p>

            <p style='margin: 30px 0; text-align: center;'>
            <a href='%s' target='_blank' style='
            display: inline-block;
            padding: 15px 30px;
            background-color: #1a73e8;
            color: #ffffff;
            text-decoration: none;
            border-radius: 50px;
            font-size: 18px;
            font-weight: bold;
            border: 1px solid #1a73e8;
            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
            '>🔒 CLIQUE AQUI PARA NOVA SENHA</a>
            </p>

            <div style='background-color: #fff3e0; border-left: 5px solid #ff9900; padding: 15px; margin-top: 25px; border-radius: 4px;'>
                <p style='font-size: 15px; color: #ff9900; margin: 0;'>
                &#x26A0;&#xFE0F; Atenção: Este link de redefinição é sensível ao tempo e expira em 30 minutos por motivos de segurança.
                </p>
            </div>
            
            <p style='font-size: 14px; color: #666; text-align: center; margin-top: 20px;'>Se o botão não funcionar, copie e cole o link abaixo em seu navegador:<br>
            <a href='%s' style='color: #1a73e8; word-break: break-all;'>%s</a></p>

            <p style='font-size: 14px; color: #1e8449; margin-top: 30px; border-top: 1px solid #eee; padding-top: 15px; text-align: center;'>
            Não solicitou esta redefinição? Relaxe! 
            <p style='font-size: 14px; color: #1e8449; margin-top: 30px; border-top: 1px solid #eee;'>
            Se você não fez esta solicitação, pode simplesmente ignorar este e-mail. Sua senha antiga permanecerá segura e nenhuma alteração será feita na sua conta.
            </p>

            <p style='font-size: 16px; color: #333; margin-top: 40px;'>Conte sempre conosco para manter sua conta segura!</p>
            <p style='font-size: 14px; color: #555;'>Atenciosamente,<br>Time de Suporte Kronos Solutions</p>
        </div>
        </body>
        </html>
        """;


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