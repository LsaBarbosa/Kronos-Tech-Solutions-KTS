package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.TimesheetSignatureEntity;
import com.kts.kronos.domain.model.TimesheetSignature;

public final class TimesheetSignatureMapper {

    private TimesheetSignatureMapper() {
    }

    public static TimesheetSignature toDomain(TimesheetSignatureEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TimesheetSignature(
                entity.getSignatureId(),
                entity.getEmployeeId(),
                entity.getCompanyId(),
                entity.getSignerUserId(),
                entity.getReferenceYear(),
                entity.getReferenceMonth(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getSignedAt(),
                entity.getSignedAtZone(),
                entity.getSignatureType(),
                entity.getSignatureMethod(),
                entity.getStatus(),
                entity.getPointMirrorDocumentId(),
                entity.getPointMirrorHashSha256(),
                entity.getRecordsSnapshotHashSha256(),
                entity.getDeclarationVersion(),
                entity.getDeclarationHashSha256(),
                entity.getDeclarationText(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getEvidenceJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVoidedAt(),
                entity.getVoidedByUserId(),
                entity.getVoidReason(),
                entity.getDocumentType(),
                entity.getDocumentVersion(),
                entity.getCanonicalEvidenceHashSha256(),
                entity.getAuditLogId(),
                entity.getPadesSignatureStatus()
        );
    }

    public static TimesheetSignatureEntity toEntity(TimesheetSignature signature) {
        if (signature == null) {
            return null;
        }
        return TimesheetSignatureEntity.builder()
                .signatureId(signature.signatureId())
                .employeeId(signature.employeeId())
                .companyId(signature.companyId())
                .signerUserId(signature.signerUserId())
                .referenceYear(signature.referenceYear())
                .referenceMonth(signature.referenceMonth())
                .periodStart(signature.periodStart())
                .periodEnd(signature.periodEnd())
                .signedAt(signature.signedAt())
                .signedAtZone(signature.signedAtZone())
                .signatureType(signature.signatureType())
                .signatureMethod(signature.signatureMethod())
                .status(signature.status())
                .pointMirrorDocumentId(signature.pointMirrorDocumentId())
                .pointMirrorHashSha256(signature.pointMirrorHashSha256())
                .recordsSnapshotHashSha256(signature.recordsSnapshotHashSha256())
                .declarationVersion(signature.declarationVersion())
                .declarationHashSha256(signature.declarationHashSha256())
                .declarationText(signature.declarationText())
                .ipAddress(signature.ipAddress())
                .userAgent(signature.userAgent())
                .evidenceJson(signature.evidenceJson())
                .createdAt(signature.createdAt())
                .updatedAt(signature.updatedAt())
                .voidedAt(signature.voidedAt())
                .voidedByUserId(signature.voidedByUserId())
                .voidReason(signature.voidReason())
                .documentType(signature.documentType())
                .documentVersion(signature.documentVersion())
                .canonicalEvidenceHashSha256(signature.canonicalEvidenceHashSha256())
                .auditLogId(signature.auditLogId())
                .padesSignatureStatus(signature.padesSignatureStatus())
                .build();
    }
}
