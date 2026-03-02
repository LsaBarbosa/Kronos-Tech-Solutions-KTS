package com.kts.kronos.adapter.out.security;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.*;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String requestPath = request.getServletPath();

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug(LOG_AUTH_HEADER_MISSING_OR_INVALID, requestPath);
            chain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(BEARER_PREFIX.length());

        var claimsOpt = jwtUtils.getValidClaims(token);
        if (claimsOpt.isEmpty()) {
            log.warn(LOG_INVALID_JWT_BLOCKED, requestPath);
            writeUnauthorized(response, JWT_INVALID_OR_EXPIRED);
            return;
        }

        String username = claimsOpt.get().getSubject();

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails user = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                        user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug(LOG_AUTHENTICATION_SUCCESS, username, requestPath);
            } catch (UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                log.warn(LOG_TOKEN_USER_NOT_FOUND, username, requestPath);
                writeUnauthorized(response, JWT_USER_NOT_FOUND);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(RESPONSE_UNAUTHORIZED_TEMPLATE, detail));
    }
}
