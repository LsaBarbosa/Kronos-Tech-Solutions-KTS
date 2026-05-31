package com.kts.kronos.application.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivacyLogReferenceServiceTest {

    @Test
    void shouldCreateStableEmployeeRefWithoutRawUuid() {
        var service = new PrivacyLogReferenceService("test-secret");
        var employeeId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        var first = service.employeeRef(employeeId);
        var second = service.employeeRef(employeeId);

        assertEquals(first, second);
        assertTrue(first.startsWith("employee_ref_"));
        assertFalse(first.contains(employeeId.toString()));
    }

    @Test
    void shouldUseSecretWhenHashingReferences() {
        var employeeId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        var first = new PrivacyLogReferenceService("secret-one").employeeRef(employeeId);
        var second = new PrivacyLogReferenceService("secret-two").employeeRef(employeeId);

        assertNotEquals(first, second);
    }

    @Test
    void shouldNormalizeEmailBeforeHashing() {
        var service = new PrivacyLogReferenceService("test-secret");

        assertEquals(
                service.emailRef("USER@example.com"),
                service.emailRef(" user@example.com ")
        );
    }

    @Test
    void shouldReturnTypedNoneAndEmptyReferences() {
        var service = new PrivacyLogReferenceService("test-secret");

        assertEquals("storage_ref_none", service.storageRef(null));
        assertEquals("storage_ref_empty", service.storageRef(" "));
    }
}
