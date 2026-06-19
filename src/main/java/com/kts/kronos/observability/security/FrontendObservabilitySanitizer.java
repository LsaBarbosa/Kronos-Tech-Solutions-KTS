package com.kts.kronos.observability.security;

import com.kts.kronos.adapter.in.web.dto.observability.FrontendObservabilityEventRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class FrontendObservabilitySanitizer {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(bearer\\s+[a-z0-9._\\-]+|eyJ[a-zA-Z0-9_\\-.]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\+?\\d{10,13}\\b");
    private static final Pattern GEO_PATTERN = Pattern.compile("-?\\d{1,3}\\.\\d{4,}");
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("[^a-z0-9_\\-./]");

    private final int maxPayloadCharacters;

    public FrontendObservabilitySanitizer(
            @Value("${kronos.observability.frontend.events.max-payload-characters:4096}") int maxPayloadCharacters
    ) {
        this.maxPayloadCharacters = maxPayloadCharacters;
    }

    public SanitizedFrontendObservabilityEvent sanitize(FrontendObservabilityEventRequest request) {
        return new SanitizedFrontendObservabilityEvent(
                sanitizeIdentifier(request.eventType()),
                sanitizeIdentifier(request.level()),
                sanitizeIdentifier(request.category()),
                sanitizeRoute(request.route()),
                sanitizeIdentifier(request.source()),
                sanitizeIdentifier(request.result()),
                sanitizeIdentifier(request.reason()),
                sanitizeFreeText(request.message()),
                sanitizeCorrelationId(request.correlationId()),
                sanitizeDuration(request.durationMs())
        );
    }

    private String sanitizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        String normalized = SAFE_TEXT_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("_");
        normalized = normalized.replaceAll("_+", "_");
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private String sanitizeRoute(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        String normalized = value.trim();
        normalized = CPF_PATTERN.matcher(normalized).replaceAll("[redacted]");
        normalized = EMAIL_PATTERN.matcher(normalized).replaceAll("[redacted]");
        normalized = TOKEN_PATTERN.matcher(normalized).replaceAll("[redacted]");
        normalized = PHONE_PATTERN.matcher(normalized).replaceAll("[redacted]");
        normalized = GEO_PATTERN.matcher(normalized).replaceAll("[redacted]");

        if (normalized.length() > 160) {
            normalized = normalized.substring(0, 160);
        }

        return normalized;
    }

    private String sanitizeFreeText(String value) {
        if (value == null || value.isBlank()) {
            return "not_provided";
        }

        String normalized = value.trim();
        normalized = CPF_PATTERN.matcher(normalized).replaceAll("[redacted]");
        normalized = EMAIL_PATTERN.matcher(normalized).replaceAll("[redacted]");
        normalized = TOKEN_PATTERN.matcher(normalized).replaceAll("[redacted]");
        normalized = PHONE_PATTERN.matcher(normalized).replaceAll("[redacted]");
        normalized = GEO_PATTERN.matcher(normalized).replaceAll("[redacted]");

        if (normalized.length() > Math.min(maxPayloadCharacters, 240)) {
            normalized = normalized.substring(0, Math.min(maxPayloadCharacters, 240));
        }

        return normalized;
    }

    private String sanitizeCorrelationId(String value) {
        if (value == null || value.isBlank()) {
            return "absent";
        }

        if (value.length() > 128) {
            return value.substring(0, 128);
        }

        return value;
    }

    private Long sanitizeDuration(Long value) {
        if (value == null || value < 0) {
            return null;
        }

        return Math.min(value, 120000L);
    }

    public record SanitizedFrontendObservabilityEvent(
            String eventType,
            String level,
            String category,
            String route,
            String source,
            String result,
            String reason,
            String message,
            String correlationId,
            Long durationMs
    ) {
    }
}
