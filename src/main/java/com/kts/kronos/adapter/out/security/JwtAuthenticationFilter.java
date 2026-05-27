package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final UserProvider userProvider;
    private final TokenBlacklistProvider tokenBlacklistProvider;
    private final AuthCookieService authCookieService;

    public JwtAuthenticationFilter(
            JwtUtils jwtUtils,
            UserDetailsService userDetailsService,
            UserProvider userProvider,
            TokenBlacklistProvider tokenBlacklistProvider,
            AuthCookieService authCookieService
    ) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.userProvider = userProvider;
        this.tokenBlacklistProvider = tokenBlacklistProvider;
        this.authCookieService = authCookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var token = authCookieService.extractToken(request).orElse(null);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        if (!jwtUtils.validateToken(token)) {
            chain.doFilter(request, response);
            return;
        }

        if (tokenBlacklistProvider.isBlacklisted(token)) {
            chain.doFilter(request, response);
            return;
        }

        String username = jwtUtils.getUsernameFromToken(token);
        var userId = jwtUtils.getUserIdFromToken(token);
        long tokenSessionVersion = jwtUtils.getSessionVersionFromToken(token);

        if (userId == null) {
            chain.doFilter(request, response);
            return;
        }

        var currentUser = userProvider.findById(userId).orElse(null);
        if (currentUser == null) {
            chain.doFilter(request, response);
            return;
        }

        if (currentUser.sessionVersion() != tokenSessionVersion) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setHeader("X-Session-Revoked", "true");
            response.setHeader("X-Session-Revoked-Reason", "SESSION_VERSION_MISMATCH");
            response.getWriter().write("""
                {"code": "SESSION_REVOKED", "message": "Sessão invalidada. Faça login novamente."}
                """);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails user = userDetailsService.loadUserByUsername(username);


            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                    user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));


            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }
}
