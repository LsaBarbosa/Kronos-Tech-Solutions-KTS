package com.kts.kronos.adapter.in.web.dto.lgpd;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnonymizationDomainTest {

    @Test
    void shouldCreateUserDomain() {
        var domain = AnonymizationDomain.user(10, 5, 3);
        assertEquals("USER", domain.resourceType());
        assertEquals("ANONYMIZE_AND_DEACTIVATE", domain.action());
        assertEquals(10, domain.scanned());
    }

    @Test
    void shouldCreateDocumentDomain() {
        var domain = AnonymizationDomain.document(20, 15, 2);
        assertEquals("DOCUMENT", domain.resourceType());
        assertEquals("DELETE_AND_ANONYMIZE", domain.action());
    }

    @Test
    void shouldCreateBiometricArtifactDomain() {
        var domain = AnonymizationDomain.biometricArtifact(5, 5, 0);
        assertEquals("BIOMETRIC_ARTIFACT", domain.resourceType());
    }

    @Test
    void shouldCreateEmployeeDomain() {
        var domain = AnonymizationDomain.employee(1, 1, 0);
        assertEquals("EMPLOYEE", domain.resourceType());
    }

    @Test
    void shouldCreateMessageDomain() {
        var domain = AnonymizationDomain.message(30, 20, 5);
        assertEquals("MESSAGE", domain.resourceType());
    }

    @Test
    void shouldCreateAuditLogDomain() {
        var domain = AnonymizationDomain.auditLog(100, 50, 10);
        assertEquals("AUDIT_LOG", domain.resourceType());
    }
}
