package com.kts.kronos.adapter.in.web.dto.employee;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegisterFaceRequestTest {

    @Test
    void shouldExposeRecordComponents() {
        UUID employeeId = UUID.randomUUID();

        var request = new RegisterFaceRequest("base64-image", employeeId);

        assertEquals("base64-image", request.faceImageBase64());
        assertEquals(employeeId, request.employeeId());
    }
}
