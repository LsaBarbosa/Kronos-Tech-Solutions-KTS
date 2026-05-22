package com.kts.kronos.application.service.anonymization.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class AnonymizationUtil {
    private static final String ANON_PREFIX = "ANON";

    public static String anonymizeCpf(UUID employeeId) {
        String hash = hashSha256(employeeId.toString()).substring(0, 7).toUpperCase();
        return ANON_PREFIX + hash;
    }

    public static String anonymizePis(UUID employeeId) {
        String hash = hashSha256(employeeId.toString() + "_pis").substring(0, 7).toUpperCase();
        return ANON_PREFIX + hash;
    }

    public static String anonymizeEmail(UUID employeeId) {
        return "anon_" + hashSha256(employeeId.toString()).substring(0, 8) + "@employee.local";
    }

    private static String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
