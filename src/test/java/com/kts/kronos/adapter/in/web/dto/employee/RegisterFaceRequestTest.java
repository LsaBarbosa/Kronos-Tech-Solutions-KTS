package com.kts.kronos.adapter.in.web.dto.employee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;;

class RegisterFaceRequestTest {

    @Test
    void shouldExposeRecordComponents() {
        UUID employeeId = UUID.randomUUID();

        var request = new RegisterFaceRequest("base64-image", employeeId, true);

        assertEquals("base64-image", request.faceImageBase64());
        assertEquals(employeeId, request.employeeId());
        assertEquals(true, request.livenessPassed());
    }

    @Test
    @DisplayName("toString does not reveal faceImageBase64 value")
    void toString_doesNotRevealFaceImageBase64() {
        String sensitiveBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        UUID employeeId = UUID.randomUUID();
        RegisterFaceRequest request = new RegisterFaceRequest(sensitiveBase64, employeeId, true);

        String result = request.toString();

        assertFalse(result.contains("iVBORw0KGgoAAAA"));
        assertFalse(result.contains(sensitiveBase64));
        assertTrue(result.contains("***MASKED***"));
    }

    @Test
    @DisplayName("toString contains employeeId")
    void toString_containsEmployeeId() {
        String base64 = "dGVzdGJhc2U2NCBkYXRh";
        UUID employeeId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        RegisterFaceRequest request = new RegisterFaceRequest(base64, employeeId, false);

        String result = request.toString();

        assertTrue(result.contains("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    @DisplayName("toString has expected format with MASKED placeholder")
    void toString_hasExpectedFormat() {
        String base64 = "dGVzdA==";
        UUID employeeId = UUID.randomUUID();
        RegisterFaceRequest request = new RegisterFaceRequest(base64, employeeId, null);

        String result = request.toString();

        assertTrue(result.startsWith("RegisterFaceRequest["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("faceImageBase64=***MASKED***"));
        assertTrue(result.contains("employeeId=" + employeeId));
    }
}
