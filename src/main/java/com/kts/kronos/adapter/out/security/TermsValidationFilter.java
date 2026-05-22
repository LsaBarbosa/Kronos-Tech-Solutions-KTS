package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class TermsValidationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AuthCookieService authCookieService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private static final String TERMS_SYSTEM_URL = "https://termo.kronossolutions.tech/";
    // Lista de endpoints permitidos mesmo sem aceite dos termos
    private static final List<String> WHITELIST = Arrays.asList(
            "/auth",             // Login
            "/terms",            // Endpoints de Aceite e Status
            "/v3/api-docs",      // Swagger
            "/swagger-ui",       // Swagger
            "/actuator"          // Health checks
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

        // Note: Biometric consent checks are now handled at the service/controller level,
        // not globally, allowing granular control over which features require consent.
        // LGPD-102: Removed global terms acceptance block to support granular biometric consent.
        chain.doFilter(request, response);
    }
}
