package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.application.port.out.provider.CommercialLeadEmailProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CommercialLeadEmailProviderImpl implements CommercialLeadEmailProvider {

    private final JavaMailSender mailSender;
    private final PrivacyLogReferenceService privacyLogReferenceService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Value("${app.mail.from:noreply@kronos-tech.com}")
    private String mailFrom;

    @Value("${kronos.commercial.leads.to}")
    private String leadsTo;

    @Autowired
    public CommercialLeadEmailProviderImpl(
            JavaMailSender mailSender,
            PrivacyLogReferenceService privacyLogReferenceService,
            KronosMetrics kronosMetrics,
            KronosTracing kronosTracing
    ) {
        this.mailSender = mailSender;
        this.privacyLogReferenceService = privacyLogReferenceService;
        this.kronosMetrics = kronosMetrics;
        this.kronosTracing = kronosTracing;
    }

    public CommercialLeadEmailProviderImpl(
            JavaMailSender mailSender,
            PrivacyLogReferenceService privacyLogReferenceService
    ) {
        this(mailSender, privacyLogReferenceService, ObservabilityDefaults.metrics(), ObservabilityDefaults.tracing());
    }

    @Override
    public void sendLeadNotification(String name, String company, String corporateEmail) {
        long startedAt = System.nanoTime();
        try {
            kronosTracing.observe("kronos.external.email", () -> {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                    helper.setFrom(mailFrom);
                    helper.setTo(leadsTo);
                    helper.setReplyTo(corporateEmail);
                    helper.setSubject("Nova solicitação de demonstração — " + company);
                    helper.setText(buildPlainText(name, company, corporateEmail), buildHtml(name, company, corporateEmail));

                    mailSender.send(mimeMessage);
                } catch (MessagingException e) {
                    throw new EmailDispatchException(e);
                }
            }, "provider", "email", "operation", "lead");

            kronosMetrics.recordExternalProviderRequest("email", "lead", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("email", "lead",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
            log.info("event=commercial_lead_email_sent recipientRef={} companyRef={}",
                    privacyLogReferenceService.emailRef(corporateEmail),
                    privacyLogReferenceService.genericRef("company", company));
        } catch (EmailDispatchException e) {
            kronosMetrics.recordExternalProviderRequest("email", "lead", "failure", "messaging_exception");
            kronosMetrics.recordExternalProviderRequestDuration("email", "lead",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=commercial_lead_email_error recipientRef={} exceptionType={}",
                    privacyLogReferenceService.emailRef(corporateEmail),
                    MessagingException.class.getSimpleName());
            throw new RuntimeException("Falha ao enviar notificação de lead comercial.", e.getCause());
        }
    }

    private String buildHtml(String name, String company, String email) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#F8FAFC;font-family:Inter,Segoe UI,Arial,sans-serif">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 16px">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:16px;border:1px solid #E2E8F0;overflow:hidden">
                        <tr>
                          <td style="background:#2563EB;padding:28px 32px">
                            <h1 style="margin:0;color:#fff;font-size:22px;font-weight:700">Nova solicitação de demonstração</h1>
                            <p style="margin:6px 0 0;color:#BFDBFE;font-size:14px">Kronos · Plataforma de Gestão de Pessoas</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="padding:10px 0;border-bottom:1px solid #F1F5F9">
                                  <p style="margin:0;font-size:12px;color:#94A3B8;text-transform:uppercase;letter-spacing:.06em;font-weight:600">Nome</p>
                                  <p style="margin:4px 0 0;font-size:15px;color:#111827;font-weight:500">%s</p>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:10px 0;border-bottom:1px solid #F1F5F9">
                                  <p style="margin:0;font-size:12px;color:#94A3B8;text-transform:uppercase;letter-spacing:.06em;font-weight:600">Empresa</p>
                                  <p style="margin:4px 0 0;font-size:15px;color:#111827;font-weight:500">%s</p>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:10px 0">
                                  <p style="margin:0;font-size:12px;color:#94A3B8;text-transform:uppercase;letter-spacing:.06em;font-weight:600">E-mail corporativo</p>
                                  <p style="margin:4px 0 0;font-size:15px;color:#2563EB;font-weight:500">
                                    <a href="mailto:%s" style="color:#2563EB;text-decoration:none">%s</a>
                                  </p>
                                </td>
                              </tr>
                            </table>
                            <div style="margin-top:24px;padding:16px;background:#EFF6FF;border-radius:10px;border:1px solid #BFDBFE">
                              <p style="margin:0;font-size:13px;color:#1D4ED8">
                                Responda diretamente a este e-mail para entrar em contato com o solicitante.
                              </p>
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 32px 24px;border-top:1px solid #F1F5F9">
                            <p style="margin:0;font-size:11px;color:#94A3B8;text-align:center">Kronos Tech Solutions · Plataforma enviou esta notificação automaticamente</p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(name), escapeHtml(company), escapeHtml(email), escapeHtml(email));
    }

    private String buildPlainText(String name, String company, String email) {
        return """
                Nova solicitação de demonstração — Kronos

                Nome: %s
                Empresa: %s
                E-mail corporativo: %s

                Responda diretamente para o e-mail acima para entrar em contato com o solicitante.
                """.formatted(name, company, email);
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static final class EmailDispatchException extends RuntimeException {
        private EmailDispatchException(MessagingException cause) {
            super(cause);
        }
    }
}
