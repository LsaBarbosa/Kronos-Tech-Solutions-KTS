package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.public_commercial.CommercialLeadRequest;
import com.kts.kronos.application.port.out.provider.CommercialLeadEmailProvider;
import com.kts.kronos.application.port.out.provider.RateLimitStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommercialLeadService {

    private static final String RATE_LIMIT_BUCKET = "commercial-leads";
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final CommercialLeadEmailProvider commercialLeadEmailProvider;
    private final RateLimitStore rateLimitStore;

    @Value("${kronos.commercial.leads.rate-limit:5}")
    private long rateLimit;

    public void submitLead(CommercialLeadRequest request, String clientIp) {
        long count = rateLimitStore.increment(RATE_LIMIT_BUCKET, clientIp, RATE_LIMIT_WINDOW);
        if (count > rateLimit) {
            log.warn("event=commercial_lead_rate_limited ip_ref={}", anonymizeIp(clientIp));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas solicitações. Tente novamente mais tarde.");
        }

        String normalizedEmail = request.corporateEmail().trim().toLowerCase(Locale.ROOT);
        String normalizedName = request.name().trim();
        String normalizedCompany = request.company().trim();

        commercialLeadEmailProvider.sendLeadNotification(normalizedName, normalizedCompany, normalizedEmail);

        log.info("event=commercial_lead_submitted ip_ref={}", anonymizeIp(clientIp));
    }

    private String anonymizeIp(String ip) {
        if (ip == null) return "unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) return ip.substring(0, lastDot) + ".***";
        int lastColon = ip.lastIndexOf(':');
        if (lastColon > 0) return ip.substring(0, lastColon) + ":****";
        return "***";
    }
}
