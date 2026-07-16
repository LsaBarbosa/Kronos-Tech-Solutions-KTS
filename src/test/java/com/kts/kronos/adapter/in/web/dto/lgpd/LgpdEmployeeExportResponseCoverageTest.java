package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LgpdEmployeeExportResponseCoverageTest {

    private Employee buildEmployee(String faceS3ObjectKey, Address address) {
        return new Employee(
                UUID.randomUUID(), "Lucas", "12345678901", "98765432100",
                "Dev", "lucas@kts.com", 5000.0, "11999999999", true,
                address, UUID.randomUUID(), LocalDateTime.now(), false,
                faceS3ObjectKey,
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2, LocalDate.now(),
                DayOfWeek.MONDAY, null, Set.of()
        );
    }

    private Company buildCompany() {
        return new Company(
                UUID.randomUUID(), "KTS", "12345678000199", "kts@kts.com", true,
                new Address("Rua A", "1", "01001000", "SP", "SP"),
                new com.kts.kronos.adapter.in.web.dto.company.Location(-23.0, -46.0), 5, 1
        );
    }

    private LegalConsent buildConsent(UUID employeeId, ConsentType type, boolean active) {
        Instant revokedAt = active ? null : Instant.now().minusSeconds(3600);
        return new LegalConsent(
                UUID.randomUUID(), employeeId, UUID.randomUUID(),
                type, LegalBasis.CONSENT, "purpose", "1.0", "hash",
                Instant.now().minusSeconds(7200), revokedAt,
                "127.0.0.1", "JUnit", null, null,
                Instant.now().minusSeconds(7200), null
        );
    }

    private Document buildDocument(UUID employeeId, DocumentType type) {
        return new Document(
                UUID.randomUUID(), employeeId, type,
                "file.pdf", "application/pdf", "path/to/file.pdf",
                LocalDateTime.now(), null, false, false, "checksum"
        );
    }

    // ── Test 1: hasFaceImage=FALSE → `hasFaceImage && hasActiveBiometricConsent` ─
    //   short-circuits FALSE (from: B=1 covered).
    //   Uses a non-BIOMETRIC_AUTH consent → lambda$from$0 consentType!=BIOMETRIC_AUTH FALSE (B=1).
    //   Uses a non-BIOMETRIC_CONSENT_TERM document → lambda$from$2 FALSE branch (B=1).

    @Test
    void from_withNoFaceImage_andNonBiometricAuthConsent_andOtherDocType() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = buildEmployee(null, new Address("Rua A", "1", "01001000", "SP", "SP"));
        Company company = buildCompany();

        LegalConsent nonBiometricConsent = buildConsent(employeeId, ConsentType.PRIVACY_POLICY, true);
        Document otherDoc = buildDocument(employeeId, DocumentType.PAYSLIP);

        LgpdEmployeeExportResponse result = LgpdEmployeeExportResponse.from(
                employee, null, company,
                List.of(otherDoc), List.of(), List.of(), List.of(),
                List.of(nonBiometricConsent),
                false, UUID.randomUUID()
        );

        assertFalse(result.biometricStatus().faceImageRegistered());
        assertFalse(result.biometricStatus().activeBiometricConsent());
        assertFalse(result.biometricStatus().biometricLoginEnabled());
        // PAYSLIP → filter(type==BIOMETRIC_CONSENT_TERM) returns 0
        assertEquals(0L, result.biometricStatus().biometricEvidenceDocumentCount());
        assertNull(result.user()); // null user → ExportedUser.from(null) = null
    }

    // ── Test 2: BIOMETRIC_AUTH consent + isActive=FALSE (revoked) ────────────────
    //   Covers lambda$from$0: consentType=TRUE, isActive=FALSE branch (B=1).

    @Test
    void from_withRevokedBiometricConsent_notActiveBiometric() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = buildEmployee("face/key.jpg", new Address("Rua B", "2", "01001001", "SP", "SP"));
        Company company = buildCompany();
        User user = new User(UUID.randomUUID(), "lucas", "hash", Role.PARTNER, true, employeeId);

        LegalConsent revokedBiometricConsent = buildConsent(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION, false);
        Document biometricDoc = buildDocument(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM);

        LgpdEmployeeExportResponse result = LgpdEmployeeExportResponse.from(
                employee, user, company,
                List.of(biometricDoc), List.of(), List.of(), List.of(),
                List.of(revokedBiometricConsent),
                false, UUID.randomUUID()
        );

        assertTrue(result.biometricStatus().faceImageRegistered());
        assertFalse(result.biometricStatus().activeBiometricConsent()); // revoked → false
        assertFalse(result.biometricStatus().biometricLoginEnabled()); // face=TRUE but consent=FALSE
        // BIOMETRIC_CONSENT_TERM document → filter counts 1
        assertEquals(1L, result.biometricStatus().biometricEvidenceDocumentCount());
    }

    // ── Test 3: ExportedAddress.from(null) → returns null ────────────────────────

    @Test
    void exportedAddress_fromNull_returnsNull() {
        assertNull(LgpdEmployeeExportResponse.ExportedAddress.from(null));
    }
}
