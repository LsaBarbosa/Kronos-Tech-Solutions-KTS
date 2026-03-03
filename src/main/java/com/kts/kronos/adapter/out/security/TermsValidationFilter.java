package com.kts.kronos.adapter.out.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.*;

@RequiredArgsConstructor
@Slf4j
public class TermsValidationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AuthCookieService authCookieService;

    private static final List<String> EXACT_PUBLIC_PATHS = List.of(
            AUTH_LOGIN_PATH,
            AUTH_LOGIN_FACE_PATH,
            AUTH_RECOVER_PASSWORD_PATH,
            AUTH_RESET_PASSWORD_PATH,
            TERMS_ACCEPT_BIOMETRIC_PATH,
            TERMS_STATUS_PATH,
            ACTUATOR_HEALTH_PATH,
            ACTUATOR_INFO_PATH
    );

    private static final List<String> PUBLIC_PREFIXES = List.of(
            API_DOCS_PREFIX,
            SWAGGER_UI_PREFIX
    );
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // 1. Se for rota pública (Whitelist) ou OPTIONS, deixa passar
        boolean isPublicExact = EXACT_PUBLIC_PATHS.contains(path);
        boolean isPublicPrefix = PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);

        if (isPublicExact || isPublicPrefix || HTTP_METHOD_OPTIONS.equalsIgnoreCase(request.getMethod())) {
            log.debug(LOG_TERMS_VALIDATION_SKIPPED, path, request.getMethod());
            chain.doFilter(request, response);
            return;
        }

        var tokenOpt = extractToken(request.getHeader(AUTHORIZATION_HEADER), request);
        if (tokenOpt.isPresent()) {
            String token = tokenOpt.get();

            // 3. Verifica a claim de aceite
            var claimsOpt = jwtUtils.getValidClaims(token);
            if (claimsOpt.isPresent()) {
                var acceptedObj = claimsOpt.get().get("terms_accepted");
                boolean accepted = acceptedObj instanceof Boolean b && b;

                if (!accepted) {
                    log.warn(LOG_TERMS_NOT_ACCEPTED_BLOCKED, path);
                    sendRedirectInstruction(response);
                    return;
                }
                log.debug(LOG_TERMS_ACCEPTED, path);
            } else {
                log.warn(LOG_TERMS_TOKEN_INVALID, path);
            }
        } else {
            log.debug(LOG_TERMS_NO_BEARER_TOKEN, path);
        }

        chain.doFilter(request, response);
    }

    private Optional<String> extractToken(String authHeader, HttpServletRequest request) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.of(authHeader.substring(BEARER_PREFIX.length()));
        }
        return authCookieService.extractTokenFromCookie(request);
    }

    private void sendRedirectInstruction(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String jsonResponse = String.format(
                TERMS_NOT_ACCEPTED_RESPONSE_TEMPLATE,
                TERMS_NOT_ACCEPTED_TYPE,
                TERMS_SYSTEM_URL,
                TERMS_NOT_ACCEPTED_DETAIL
        );

        response.getWriter().write(jsonResponse);
    }
}
