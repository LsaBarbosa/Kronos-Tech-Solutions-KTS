package com.kts.kronos.observability.domain;

import lombok.extern.slf4j.Slf4j;

/**
 * LGPD-OBS-001: Sanitizador de dados sensíveis em observabilidade
 * Defesa em profundidade: sanitiza dados mesmo que o frontend falhe
 * Nunca retorna dados brutos em caso de erro
 */
@Slf4j
public class SensitiveDataSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final String CPF_REDACTED = "[CPF_REDACTED]";
    private static final String CNPJ_REDACTED = "[CNPJ_REDACTED]";
    private static final String EMAIL_REDACTED = "[EMAIL_REDACTED]";
    private static final String JWT_REDACTED = "[JWT_REDACTED]";
    private static final String STORAGE_PATH_REDACTED = "[STORAGE_PATH_REDACTED]";
    private static final String BASE64_REDACTED = "[REDACTED]";
    private static final String SANITIZATION_FAILED = "[SANITIZATION_FAILED]";

    private static final int MAX_TEXT_LENGTH = 5000;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_NAME_LENGTH = 200;

    /**
     * Sanitiza texto removendo padrões sensíveis
     * Se falhar, retorna marcador seguro
     */
    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        try {
            String sanitized = value;

            // CPF: 123.456.789-01 ou 12345678901
            sanitized = sanitized.replaceAll("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b", CPF_REDACTED);

            // CNPJ: 12.345.678/0001-99 ou 12345678000199
            sanitized = sanitized.replaceAll("\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b", CNPJ_REDACTED);

            // Email
            sanitized = sanitized.replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", EMAIL_REDACTED);

            // Bearer Token
            sanitized = sanitized.replaceAll("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer " + REDACTED);

            // JWT (eyJ...eyJ...xxx)
            sanitized = sanitized.replaceAll("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b", JWT_REDACTED);

            // Set-Cookie e Cookie headers
            sanitized = sanitized.replaceAll("(?i)(Set-Cookie|Cookie)\\s*:\\s*[^;\\n]+(?:;[^;\\n]+)*", "$1: " + REDACTED);

            // data: URL com base64
            sanitized = sanitized.replaceAll("(?i)(data:[^;]+;base64,)[A-Za-z0-9+/=]+", "$1" + BASE64_REDACTED);

            // base64 direto
            sanitized = sanitized.replaceAll("(?i)(base64,)[A-Za-z0-9+/=]+", "$1" + BASE64_REDACTED);

            // Query params sensíveis
            sanitized = sanitized.replaceAll(
                "(?i)\\b(token|access_token|refresh_token|signature|X-Amz-Signature|X-Amz-Credential|X-Amz-Security-Token|AWSAccessKeyId|password|cpf|cnpj|email|username)=([^&\\s]+)",
                "$1=" + REDACTED
            );

            // S3 paths
            sanitized = sanitized.replaceAll("(?i)\\bs3://[^\\s]+", STORAGE_PATH_REDACTED);

            // Storage paths (employees/123/faces, documents/456, etc)
            sanitized = sanitized.replaceAll("(?i)\\b(?:employees?|documents?|faces?)/[A-Za-z0-9._\\-/%]+", STORAGE_PATH_REDACTED);

            return truncate(sanitized, MAX_TEXT_LENGTH);
        } catch (Exception e) {
            log.debug("Error sanitizing text, returning safe marker", e);
            return SANITIZATION_FAILED;
        }
    }

    /**
     * Sanitiza um objeto ou valor genérico
     * Retorna versão sanitizada ou marcador seguro se falhar
     */
    public String sanitizeObject(Object value) {
        if (value == null) {
            return "";
        }

        try {
            String stringValue = String.valueOf(value);
            return sanitizeText(stringValue);
        } catch (Exception e) {
            log.debug("Error sanitizing object, returning safe marker", e);
            return SANITIZATION_FAILED;
        }
    }

    /**
     * Sanitiza uma mensagem de erro
     * Limita tamanho e remove dados sensíveis
     */
    public String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        try {
            String sanitized = sanitizeText(message);
            return truncate(sanitized, MAX_MESSAGE_LENGTH);
        } catch (Exception e) {
            log.debug("Error sanitizing message, returning safe marker", e);
            return SANITIZATION_FAILED;
        }
    }

    /**
     * Sanitiza um nome de erro (exception class name, etc)
     * Limita tamanho e remove dados sensíveis
     */
    public String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        try {
            String sanitized = sanitizeText(name);
            return truncate(sanitized, MAX_NAME_LENGTH);
        } catch (Exception e) {
            log.debug("Error sanitizing name, returning safe marker", e);
            return SANITIZATION_FAILED;
        }
    }

    /**
     * Sanitiza um stack trace
     * Remove dados sensíveis mantendo estrutura útil
     */
    public String sanitizeStackTrace(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            return "";
        }

        try {
            String sanitized = sanitizeText(stackTrace);
            return truncate(sanitized, MAX_TEXT_LENGTH);
        } catch (Exception e) {
            log.debug("Error sanitizing stack trace, returning safe marker", e);
            return SANITIZATION_FAILED;
        }
    }

    /**
     * Verifica se um texto contém dados sensíveis detectáveis
     * Útil para logging seguro
     */
    public boolean containsSensitiveData(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        // Verifica padrões sensíveis (sem modificar, só detecta)
        return value.matches(".*\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b.*") || // CPF
               value.matches(".*\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b.*") || // CNPJ
               value.matches("(?i).*[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}.*") || // Email
               value.matches("(?i).*\\bBearer\\s+[A-Za-z0-9._~+/=-]+.*") || // Bearer
               value.matches(".*\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b.*") || // JWT
               value.matches("(?i).*\\bs3://[^\\s]+.*") || // S3
               value.matches("(?i).*\\b(token|password|authorization|cookie|cpf|cnpj|email|username)=.*"); // Params
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...[TRUNCATED]";
    }
}
