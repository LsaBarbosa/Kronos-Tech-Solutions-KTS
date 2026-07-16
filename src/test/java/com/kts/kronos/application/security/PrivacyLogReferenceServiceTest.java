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
    @Test
    void constructorWithNullHashSecret_usesDefault() {
        var service = new PrivacyLogReferenceService(null);
        // should not throw and should produce stable refs
        var ref = service.employeeRef(java.util.UUID.randomUUID());
        assertTrue(ref.startsWith("employee_ref_"));
    }

    @Test
    void constructorWithBlankHashSecret_usesDefault() {
        var service = new PrivacyLogReferenceService("   ");
        var ref = service.storageRef("/path/to/file");
        assertTrue(ref.startsWith("storage_ref_"));
    }

    @Test
    void emailRefWithNull_returnsNoneRef() {
        var service = new PrivacyLogReferenceService("test-secret");
        assertEquals("email_ref_none", service.emailRef(null));
    }

    @Test
    void genericRefWithNullType_usesValueAsType() {
        var service = new PrivacyLogReferenceService("test-secret");
        var ref = service.genericRef(null, "something");
        assertTrue(ref.startsWith("value_ref_"));
    }

    @Test
    void genericRefWithBlankType_usesValueAsType() {
        var service = new PrivacyLogReferenceService("test-secret");
        var ref = service.genericRef("   ", "something");
        assertTrue(ref.startsWith("value_ref_"));
    }

}