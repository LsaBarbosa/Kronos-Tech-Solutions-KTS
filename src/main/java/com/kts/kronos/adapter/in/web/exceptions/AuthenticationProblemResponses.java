package com.kts.kronos.adapter.in.web.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

final class AuthenticationProblemResponses {

    static final String AUTHENTICATION_DETAIL = "Autenticação inválida ou ausente.";
    static final String AUTHORIZATION_DETAIL = "Acesso negado.";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private AuthenticationProblemResponses() {
    }

    static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.builder()
                .status(status.value())
                .title(status.getReasonPhrase())
                .detail(detail)
                .build();
    }

    static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), problem(status, detail));
    }
}
