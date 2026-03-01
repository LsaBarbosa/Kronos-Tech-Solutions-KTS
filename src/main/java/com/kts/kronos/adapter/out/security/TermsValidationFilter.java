package com.kts.kronos.adapter.out.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class TermsValidationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private static final String TERMS_SYSTEM_URL = "https://termo.kronossolutions.tech/";
    private static final List<String> EXACT_PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/login-face",
            "/auth/recover-password",
            "/auth/reset-password",
            "/terms/accept-biometric",
            "/terms/status",
            "/actuator/health",
            "/actuator/info"
    );

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/v3/api-docs",
            "/swagger-ui"
    );
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // 1. Se for rota pública (Whitelist) ou OPTIONS, deixa passar
        boolean isPublicExact = EXACT_PUBLIC_PATHS.contains(path);
        boolean isPublicPrefix = PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);

        if (isPublicExact || isPublicPrefix || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 3. Verifica a claim de aceite
            var claimsOpt = jwtUtils.getValidClaims(token);
            if (claimsOpt.isPresent()) {
                var acceptedObj = claimsOpt.get().get("terms_accepted");
                boolean accepted = acceptedObj instanceof Boolean b && b;

                if (!accepted) {
                    sendRedirectInstruction(response);
                    return;
                }
            }
        }



        chain.doFilter(request, response);
    }

    private void sendRedirectInstruction(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String jsonResponse = String.format(
                "{\"type\": \"TERMS_NOT_ACCEPTED\", \"redirect_url\": \"%s\", \"detail\": \"Aceite os termos para continuar.\"}",
                TERMS_SYSTEM_URL
        );

        response.getWriter().write(jsonResponse);
    }
}