package com.kts.kronos.infrastructure.redis;

public final class RedisRateLimitNames {
    private RedisRateLimitNames() {
    }

    public static final String AUTH_LOGIN_IP = "auth-login-ip";
    public static final String AUTH_LOGIN_USERNAME = "auth-login-username";
    public static final String AUTH_RECOVER_CPF = "auth-recover-cpf";
    public static final String AUTH_RECOVER_EMAIL = "auth-recover-email";
    public static final String AUTH_RECOVER_IP = "auth-recover-ip";
    public static final String ADMIN_CHECK = "admin-check";
    public static final String BIOMETRIC_LOGIN_FACE = "biometric-login-face";
    public static final String BIOMETRIC_CHECKIN = "biometric-checkin";
    public static final String BIOMETRIC_ENROLLMENT = "biometric-enrollment";
}
