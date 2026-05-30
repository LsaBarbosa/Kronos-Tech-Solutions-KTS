package com.kts.kronos.application.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {
    private static final Pattern CPF_PATTERN = Pattern.compile("(?<!\\d)(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2})(?!\\d)");
    private static final Pattern PIS_PATTERN = Pattern.compile("(?<!\\d)(\\d{3}\\.?\\d{5}\\.?\\d{2}-?\\d{1})(?!\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern RESET_TOKEN_PATTERN = Pattern.compile("(?:token|resetToken|reset_token)=[^\\s&]+");
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("-?\\d{1,2}\\.\\d{6,8}");
    private static final Pattern BASE64_IMAGE_PATTERN = Pattern.compile("[A-Za-z0-9+/]{200,}={0,2}");

    private SensitiveDataMasker() {}

    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return cpf;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return "***";
        }
        return digits.substring(0, 3) + "." + "***" + "." + digits.substring(8) ;
    }

    public static String maskPis(String pis) {
        if (pis == null || pis.isBlank()) {
            return pis;
        }
        String digits = pis.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return "***";
        }
        return digits.substring(0, 3) + "." + "***" + "." + digits.substring(8);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 10) {
            return "***";
        }
        return "(" + digits.substring(0, 2) + ") 9****-" + digits.substring(digits.length() - 4);
    }

    public static String maskCoordinates(String coords) {
        if (coords == null || coords.isBlank()) {
            return coords;
        }
        return "[COORDINATES_REDACTED]";
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***@***";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        if (token.length() <= 7) {
            return "***";
        }
        return token.substring(0, 7) + "***";
    }

    public static String maskStoragePath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        return "[MASKED_PATH]";
    }

    public static String maskStorageReference(String raw) {
        if (raw == null || raw.isBlank()) {
            return "storage_ref_empty";
        }
        return "storage_ref_sha256=" + sha256Hex(raw).substring(0, 12) + ",length=" + raw.length();
    }

    public static String maskFaceBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            return base64;
        }
        return "[FACE_IMAGE_REDACTED:" + base64.length() + "chars]";
    }

    public static String sanitizeDetails(String details) {
        if (details == null || details.isBlank()) {
            return details;
        }
        String result = details;

        // Mask CPF (with or without punctuation)
        Matcher cpfMatcher = CPF_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (cpfMatcher.find()) {
            cpfMatcher.appendReplacement(sb, Matcher.quoteReplacement(maskCpf(cpfMatcher.group(1))));
        }
        cpfMatcher.appendTail(sb);
        result = sb.toString();

        // Mask PIS (with or without punctuation)
        result = PIS_PATTERN.matcher(result).replaceAll(m -> maskPis(m.group(1)));

        // Mask Email
        result = EMAIL_PATTERN.matcher(result).replaceAll(m -> maskEmail(m.group()));

        // Mask Phone
        result = PHONE_PATTERN.matcher(result).replaceAll(m -> maskPhone(m.group()));

        // Mask JWT and reset tokens
        result = TOKEN_PATTERN.matcher(result).replaceAll(m -> maskToken(m.group()));
        result = RESET_TOKEN_PATTERN.matcher(result).replaceAll("[TOKEN_REDACTED]");

        // Mask Base64 images
        result = BASE64_IMAGE_PATTERN.matcher(result).replaceAll("[BASE64_REDACTED]");

        // Mask coordinates (latitude/longitude patterns)
        result = COORDINATES_PATTERN.matcher(result).replaceAll(m -> maskCoordinates(m.group()));

        // Mask storage paths
        if (result.contains("s3://")
                || result.contains("bucket/")
                || result.contains("/uploads/")
                || result.contains("storage/")) {
            result = result.replaceAll("s3://[^\\s]+", "[MASKED_PATH]");
            result = result.replaceAll("bucket/[^\\s]+", "[MASKED_PATH]");
            result = result.replaceAll("/uploads/[^\\s]+", "[MASKED_PATH]");
            result = result.replaceAll("storage/[^\\s]+", "[MASKED_PATH]");
        }
        return result;
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
