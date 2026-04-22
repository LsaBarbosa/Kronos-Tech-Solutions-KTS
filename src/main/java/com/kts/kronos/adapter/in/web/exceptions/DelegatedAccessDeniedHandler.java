package com.kts.kronos.adapter.in.web.exceptions;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@Slf4j
public class DelegatedAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        log.warn("Acesso negado. method={}, path={}, exceptionType={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException.getClass().getSimpleName(),
                accessDeniedException.getMessage());

        AuthenticationProblemResponses.write(
                response,
                HttpStatus.FORBIDDEN,
                AuthenticationProblemResponses.AUTHORIZATION_DETAIL
        );
    }
}
