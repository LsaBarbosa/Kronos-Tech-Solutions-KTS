package com.kts.kronos.infrastructure.security;

import java.util.regex.Pattern;

/**
 * Utilitário para mascarar dados sensíveis em logs e mensagens de erro.
 * Garante conformidade com LGPD ao prevenir vazamento de informações confidenciais.
 */
public class SensitiveDataMasker {

    // Padrões regex para detecção de dados sensíveis
    private static final Pattern CPF_PATTERN = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
    private static final Pattern PIS_PATTERN = Pattern.compile("\\d{3}\\.\\d{5}\\.\\d{2}-\\d{1}");
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|passwd|pwd)[\\s:=]+[^\\s,}]+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\(\\d{2}\\)\\s?9?\\d{4}-\\d{4}");
    private static final Pattern BASE64_FACE_PATTERN = Pattern.compile("faceImageBase64[\\s:=]+([A-Za-z0-9+/=]{100,})");
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("[\\-]?\\d{1,2}\\.\\d{6,}");
    private static final Pattern RESET_TOKEN_PATTERN = Pattern.compile("resetToken[\\s:=]+[a-zA-Z0-9-]+");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api[_-]?key|apikey|secret)[\\s:=]+[^\\s,}]+");

    /**
     * Mascara todos os dados sensíveis em uma mensagem.
     */
    public static String maskSensitiveData(String message) {
        if (message == null) {
            return null;
        }

        String masked = message;

        // Mascarar CPF
        masked = CPF_PATTERN.matcher(masked).replaceAll("***.***.***-**");

        // Mascarar PIS
        masked = PIS_PATTERN.matcher(masked).replaceAll("***.***.***-*");

        // Mascarar JWT
        masked = JWT_PATTERN.matcher(masked).replaceAll("eyJ[MASKED]");

        // Mascarar senha
        masked = PASSWORD_PATTERN.matcher(masked).replaceAll("$1=[MASKED]");

        // Mascarar email (parcial)
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("****@***");

        // Mascarar telefone
        masked = PHONE_PATTERN.matcher(masked).replaceAll("(##)#####-****");

        // Mascarar faceImageBase64
        masked = BASE64_FACE_PATTERN.matcher(masked).replaceAll("faceImageBase64=[MASKED]");

        // Mascarar coordenadas precisas (deixar apenas aproximação de 2 casas)
        masked = COORDINATES_PATTERN.matcher(masked).replaceAll("[MASKED_COORDINATES]");

        // Mascarar reset token
        masked = RESET_TOKEN_PATTERN.matcher(masked).replaceAll("resetToken=[MASKED]");

        // Mascarar API keys
        masked = API_KEY_PATTERN.matcher(masked).replaceAll("$1=[MASKED]");

        return masked;
    }

    /**
     * Mascara CPF especificamente (formato XXX.XXX.XXX-XX → ***.***.***-**)
     */
    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return "[INVALID_CPF]";
        }
        return "***.***.***-" + cpf.substring(cpf.length() - 2);
    }

    /**
     * Mascara email (exemplo@email.com → ****@***).
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "[INVALID_EMAIL]";
        }
        return "****@***";
    }

    /**
     * Mascara telefone (11) 99999-9999 → (##)#####-****
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 10) {
            return "[INVALID_PHONE]";
        }
        return "(##)#####-****";
    }

    /**
     * Mascara JWT token (eyJ... → eyJ[MASKED])
     */
    public static String maskJwt(String jwt) {
        if (jwt == null || jwt.length() < 20) {
            return "[INVALID_JWT]";
        }
        return "eyJ[MASKED]";
    }

    /**
     * Verifica se a mensagem contém dados sensíveis.
     */
    public static boolean containsSensitiveData(String message) {
        if (message == null) {
            return false;
        }

        return CPF_PATTERN.matcher(message).find() ||
               PIS_PATTERN.matcher(message).find() ||
               JWT_PATTERN.matcher(message).find() ||
               PASSWORD_PATTERN.matcher(message).find() ||
               BASE64_FACE_PATTERN.matcher(message).find() ||
               RESET_TOKEN_PATTERN.matcher(message).find() ||
               API_KEY_PATTERN.matcher(message).find();
    }
}
