package com.kts.kronos.observability.support;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ObservabilityTagSanitizer {

    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-z0-9_\\-.]");
    private static final Pattern UUID_LIKE = Pattern.compile(".*[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}.*");
    private static final Set<String> ALLOWED_TAG_KEYS = Set.of(
            "method",
            "result",
            "reason",
            "operation",
            "scheduler",
            "document_type",
            "legal_document_type",
            "action",
            "event_type",
            "status",
            "mode",
            "provider",
            "severity",
            "channel",
            "trigger"
    );

    public String sanitizeTagKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Metric tag key cannot be blank");
        }

        String normalized = UNSAFE_CHARS.matcher(key.trim().toLowerCase(Locale.ROOT)).replaceAll("_");
        if (!ALLOWED_TAG_KEYS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported metric tag: " + key);
        }

        return normalized;
    }

    public String sanitizeTagValue(String key, String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = UNSAFE_CHARS.matcher(normalized).replaceAll("_");
        normalized = normalized.replaceAll("_+", "_");

        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }

        if (normalized.isBlank() || UUID_LIKE.matcher(normalized).matches()) {
            return "redacted";
        }

        if ("reason".equals(sanitizeTagKey(key)) && normalized.startsWith("java_")) {
            return "exception";
        }

        return normalized;
    }
}
