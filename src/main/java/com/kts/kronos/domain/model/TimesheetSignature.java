package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.TimesheetSignatureMethod;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TimesheetSignature(
        UUID signatureId,
        UUID employeeId,
        UUID companyId,
        UUID signerUserId,
        int referenceYear,
        int referenceMonth,
        LocalDate periodStart,
        LocalDate periodEnd,
        Instant signedAt,
        String signedAtZone,
        TimesheetSignatureType signatureType,
        TimesheetSignatureMethod signatureMethod,
        TimesheetSignatureStatus status,
        UUID pointMirrorDocumentId,
        String pointMirrorHashSha256,
        String recordsSnapshotHashSha256,
        String declarationVersion,
        String declarationHashSha256,
        String declarationText,
        String ipAddress,
        String userAgent,
        String evidenceJson,
        Instant createdAt,
        Instant updatedAt,
        Instant voidedAt,
        UUID voidedByUserId,
        String voidReason
) {
    public TimesheetSignature withId(UUID id) {
        return new TimesheetSignature(
                id, employeeId, companyId, signerUserId, referenceYear, referenceMonth,
                periodStart, periodEnd, signedAt, signedAtZone, signatureType, signatureMethod,
                status, pointMirrorDocumentId, pointMirrorHashSha256, recordsSnapshotHashSha256,
                declarationVersion, declarationHashSha256, declarationText, ipAddress, userAgent,
                evidenceJson, createdAt, updatedAt, voidedAt, voidedByUserId, voidReason
        );
    }
}
