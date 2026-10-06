package com.kts.kronos.application.security;

import com.kts.kronos.domain.model.enuns.LivenessOperation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DisabledLivenessVerificationProviderTest {

    @Test
    void verify_returnsError_withMissingProviderReasonCode() {
        var privacyRef = new PrivacyLogReferenceService("test-hmac-secret-for-unit-tests-32b!");
        var provider = new DisabledLivenessVerificationProvider(privacyRef);

        var result = provider.verify(
                "validBase64Image==",
                LivenessOperation.FACE_LOGIN,
                UUID.randomUUID()
        );

        assertFalse(result.passed());
        assertEquals("REAL_LIVENESS_PROVIDER_NOT_CONFIGURED", result.reasonCode());
        assertEquals(DisabledLivenessVerificationProvider.PROVIDER_NAME, result.provider());
    }
}
