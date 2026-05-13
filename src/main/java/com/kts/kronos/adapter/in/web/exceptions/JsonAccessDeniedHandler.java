package com.kts.kronos.adapter.in.web.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String detail = accessDeniedException instanceof CsrfException
                ? "Token CSRF ausente ou inválido."
                : "Acesso negado.";

        var problem = ProblemDetail.builder()
                .code(accessDeniedException instanceof CsrfException ? "CSRF_TOKEN_INVALID" : "ACCESS_DENIED")
                .message(detail)
                .status(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .title(HttpStatus.FORBIDDEN.getReasonPhrase())
                .detail(detail)
                .build();

        objectMapper.findAndRegisterModules().writeValue(response.getWriter(), problem);
    }
}
