package com.kts.kronos.adapter.in.web.filters;

import com.kts.kronos.infrastructure.security.SensitiveDataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SensitiveDataLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        String maskedUri = SensitiveDataMasker.maskSensitiveData(uri);
        String maskedQuery = queryString != null ? SensitiveDataMasker.maskSensitiveData(queryString) : "";

        log.debug("event=http_request method={} uri={} query={}", method, maskedUri, maskedQuery);

        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            log.debug("event=http_response method={} uri={} status={}", method, maskedUri, status);
        }
    }
}
