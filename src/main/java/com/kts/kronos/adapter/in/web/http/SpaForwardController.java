package com.kts.kronos.adapter.in.web.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    private final String frontendBaseUrl;

    public SpaForwardController(@Value("${frontend.base-url-record}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping(
            value = {
                    "/",
                    "/dashboard",
                    "/login",
                    "/senha-primeiro-acesso",
                    "/resetar-senha",
                    "/relatorio-detalhado",
                    "/espelho-ponto",
                    "/assinatura-ponto",
                    "/assinatura-contrato",
                    "/contratos/admin",
                    "/contratos/enviar",
                    "/usuario",
                    "/empresa",
                    "/empresa/criar",
                    "/empresa/buscar",
                    "/empresa/atualizar",
                    "/documentos",
                    "/enviar-documentos",
                    "/enviar-documento-colaborador",
                    "/avisos",
                    "/criar-aviso",
                    "/solicitar-ferias",
                    "/solicitar-abono",
                    "/auditoria",
                    "/criar-colaborador",
                    "/criar-administrador",
                    "/lista-colaboradores",
                    "/apuracao-horas",
                    "/status-do-registro",
                    "/ferias",
                    "/aprovacoes-abono",
                    "/meus-documentos",
                    "/administracao",
                    "/privacidade",
                    "/privacy/processing-catalog",
                    "/privacy/policy",
                    "/privacy/biometric-term",
                    "/lgpd/admin/requests",
                    "/lgpd/admin/requests/{requestId}",
                    "/lgpd/admin/inventory",
                    "/lgpd/admin/inventory/novo",
                    "/lgpd/admin/inventory/{processCode}/editar",
                    "/aviso",
                    "/privacy-policy",
                    "/politica-de-privacidade"
            },
            produces = MediaType.TEXT_HTML_VALUE
    )
    public String redirectSpaRoutes(HttpServletRequest request) {
        var requestUri = request.getRequestURI();
        var queryString = request.getQueryString();
        var target = frontendBaseUrl + requestUri;

        if (queryString != null && !queryString.isBlank()) {
            target += "?" + queryString;
        }

        return "redirect:" + target;
    }
}
