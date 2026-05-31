package com.kts.kronos.application.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Component
public class PrivacyLogReferenceService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int REF_HEX_LENGTH = 16;
    private static final HexFormat HEX = HexFormat.of();

    private final String hashSecret;

    public PrivacyLogReferenceService(
            @Value("${kronos.lgpd.log.hash-secret:${LGPD_LOG_HASH_SECRET:local-dev-lgpd-log-secret}}") String hashSecret
    ) {
        this.hashSecret = hashSecret == null || hashSecret.isBlank()
                ? "local-dev-lgpd-log-secret"
                : hashSecret;
    }

    public String employeeRef(UUID employeeId) {
        return typedRef("employee", employeeId);
    }

    public String userRef(UUID userId) {
        return typedRef("user", userId);
    }

    public String companyRef(UUID companyId) {
        return typedRef("company", companyId);
    }

    public String storageRef(String storagePath) {
        return typedRef("storage", storagePath);
    }

    public String objectRef(String objectKey) {
        return typedRef("object", objectKey);
    }

    public String faceRef(String faceId) {
        return typedRef("face", faceId);
    }

    public String externalImageRef(UUID externalImageId) {
        return typedRef("external_image", externalImageId);
    }

    public String emailRef(String email) {
        if (email == null) {
            return typedRef("email", null);
        }
        return typedRef("email", email.trim().toLowerCase(Locale.ROOT));
    }

    public String genericRef(String type, Object rawValue) {
        return typedRef(type, rawValue);
    }

    private String typedRef(String type, Object rawValue) {
        String normalizedType = type == null || type.isBlank() ? "value" : type.trim().toLowerCase(Locale.ROOT);
        if (rawValue == null) {
            return normalizedType + "_ref_none";
        }

        String raw = String.valueOf(rawValue).trim();
        if (raw.isEmpty()) {
            return normalizedType + "_ref_empty";
        }

        return normalizedType + "_ref_" + hmac(raw, normalizedType).substring(0, REF_HEX_LENGTH);
    }

    private String hmac(String raw, String type) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HEX.formatHex(mac.doFinal((type + ":" + raw).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create privacy-safe log reference", ex);
        }
    }
}
