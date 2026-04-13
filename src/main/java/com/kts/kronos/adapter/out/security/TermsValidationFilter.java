package com.kts.kronos.adapter.out.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.exceptions.ProblemDetail;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class TermsValidationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private static final String TERMS_SYSTEM_URL = "https://termo.kronossolutions.tech/";
    // Lista de endpoints permitidos mesmo sem aceite dos termos
    private static final List<String> WHITELIST = Arrays.asList(
            "/auth",
            "/terms/accept-biometric",
            "/terms/status",
            "/v3/api-docs",
            "/swagger-ui",
            "/actuator/health"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // 1. Se for rota pública (Whitelist) ou OPTIONS, deixa passar
        boolean isWhitelisted = WHITELIST.stream().anyMatch(path::startsWith);
        if (isWhitelisted || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Extrai o token (assumindo que o JwtAuthenticationFilter já validou a assinatura antes)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 3. Verifica a claim de aceite
            if (jwtUtils.validateToken(token)) {
                boolean accepted = jwtUtils.getTermsAcceptedFromToken(token);

                if (!accepted) {
                    sendRedirectInstruction(response);
                    return;
                }
            }
        }



        chain.doFilter(request, response);
    }

    private void sendRedirectInstruction(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value()); // 403
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Retornamos um JSON instruindo o redirecionamento
        String jsonResponse = String.format(
                "{\"type\": \"TERMS_NOT_ACCEPTED\", \"redirect_url\": \"%s\", \"detail\": \"Aceite os termos para continuar.\"}",
                TERMS_SYSTEM_URL
        );

        response.getWriter().write(jsonResponse);
    }
}