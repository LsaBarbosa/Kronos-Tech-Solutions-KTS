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

        // 2. Extrai o token do cookie HttpOnly usado pelo JwtAuthenticationFilter.
        var cookieToken = authCookieService.extractToken(request);
        if (cookieToken.isPresent()) {
            String token = cookieToken.get();

            // 3. Verifica a claim de aceite
            if (jwtUtils.validateToken(token)) {
                boolean accepted = jwtUtils.getTermsAcceptedFromToken(token);

                if (!accepted) {
                    handlerExceptionResolver.resolveException(
                            request,
                            response,
                            null,
                            new TermsNotAcceptedException("Aceite os termos para continuar.", TERMS_SYSTEM_URL)
                    );
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }
}
