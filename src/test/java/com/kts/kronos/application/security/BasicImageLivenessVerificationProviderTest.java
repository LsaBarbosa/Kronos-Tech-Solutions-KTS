package com.kts.kronos.application.security;

import com.kts.kronos.domain.model.LivenessVerificationResult;
import com.kts.kronos.domain.model.enuns.LivenessOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicImageLivenessVerificationProviderTest {

    private BasicImageLivenessVerificationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new BasicImageLivenessVerificationProvider(
                new PrivacyLogReferenceService("test-secret"));
    }

    @Test
    void shouldFailWhenImageIsNull() {
        var result = provider.verify(null, LivenessOperation.FACE_LOGIN, UUID.randomUUID());
        assertFalse(result.passed());
        assertEquals("EMPTY_IMAGE", result.reasonCode());
    }

    @Test
    void shouldFailWhenImageIsBlank() {
        var result = provider.verify("  ", LivenessOperation.CHECKIN, UUID.randomUUID());
        assertFalse(result.passed());
        assertEquals("EMPTY_IMAGE", result.reasonCode());
    }

    @Test
    void shouldFailWhenImageIsTooSmall() {
        var result = provider.verify("abc", LivenessOperation.ENROLLMENT, UUID.randomUUID());
        assertFalse(result.passed());
        assertEquals("IMAGE_TOO_SMALL", result.reasonCode());
    }

    @Test
    void shouldFailWhenImageIsTooLarge() {
        String bigImage = "A".repeat(5_000_001);
        var result = provider.verify(bigImage, LivenessOperation.FACE_LOGIN, null);
        assertFalse(result.passed());
        assertEquals("IMAGE_TOO_LARGE", result.reasonCode());
    }

    @Test
    void shouldFailWhenBase64IsInvalid() {
        String notBase64 = "!".repeat(200);
        var result = provider.verify(notBase64, LivenessOperation.CHECKIN, null);
        assertFalse(result.passed());
        assertEquals("INVALID_BASE64", result.reasonCode());
    }

    @Test
    void shouldPassWithValidBase64Image() {
        byte[] bytes = new byte[200];
        java.util.Arrays.fill(bytes, (byte) 1);
        String validBase64 = Base64.getEncoder().encodeToString(bytes);
        var result = provider.verify(validBase64, LivenessOperation.TIMESHEET_SIGNING, UUID.randomUUID());
        assertTrue(result.passed());
        assertEquals("BASIC_IMAGE_VALIDATOR", result.provider());
    }
    @Test
    void shouldReturnErrorWhenUnexpectedExceptionOccursDuringVerification() {
        PrivacyLogReferenceService badRef = mock(PrivacyLogReferenceService.class);
        // First call (in log.warn for null/blank image) throws → outer catch fires
        // Second call (in log.error inside catch block) must return to let catch complete
        when(badRef.employeeRef(any()))
                .thenThrow(new RuntimeException("forced-error"))
                .thenReturn("employee_ref_none");
        var badProvider = new BasicImageLivenessVerificationProvider(badRef);

        // null image triggers isBlank() log path which calls employeeRef() → throws
        // outer catch returns LivenessVerificationResult.error()
        var result = badProvider.verify(null, LivenessOperation.FACE_LOGIN, UUID.randomUUID());

        assertFalse(result.passed());
        assertEquals("VERIFICATION_ERROR", result.reasonCode());
    }

}
