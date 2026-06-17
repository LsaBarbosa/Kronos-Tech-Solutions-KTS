package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractSignatureEntity;
import com.kts.kronos.domain.model.ServiceContractSignature;

public final class ServiceContractSignatureMapper {

    private ServiceContractSignatureMapper() {}

    public static ServiceContractSignature toDomain(ServiceContractSignatureEntity e) {
        if (e == null) return null;
        return new ServiceContractSignature(
                e.getSignatureId(),
                e.getAssignmentId(),
                e.getContractId(),
                e.getEmployeeId(),
                e.getCompanyId(),
                e.getSignerUserId(),
                e.getSignedAt(),
                e.getSignedAtZone(),
                e.getSignatureType(),
                e.getSignatureMethod(),
                e.getStatus(),
                e.getSignedDocumentId(),
                e.getContractDocumentHashSha256(),
                e.getSignedPdfHashSha256(),
                e.getDeclarationVersion(),
                e.getDeclarationHashSha256(),
                e.getDeclarationText(),
                e.getIpAddress(),
                e.getUserAgent(),
                e.getEvidenceJson(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVoidedAt(),
                e.getVoidedByUserId(),
                e.getVoidReason()
        );
    }

    public static ServiceContractSignatureEntity toEntity(ServiceContractSignature s) {
        if (s == null) return null;
        return ServiceContractSignatureEntity.builder()
                .signatureId(s.signatureId())
                .assignmentId(s.assignmentId())
                .contractId(s.contractId())
                .employeeId(s.employeeId())
                .companyId(s.companyId())
                .signerUserId(s.signerUserId())
                .signedAt(s.signedAt())
                .signedAtZone(s.signedAtZone())
                .signatureType(s.signatureType())
                .signatureMethod(s.signatureMethod())
                .status(s.status())
                .signedDocumentId(s.signedDocumentId())
                .contractDocumentHashSha256(s.contractDocumentHashSha256())
                .signedPdfHashSha256(s.signedPdfHashSha256())
                .declarationVersion(s.declarationVersion())
                .declarationHashSha256(s.declarationHashSha256())
                .declarationText(s.declarationText())
                .ipAddress(s.ipAddress())
                .userAgent(s.userAgent())
                .evidenceJson(s.evidenceJson())
                .createdAt(s.createdAt())
                .updatedAt(s.updatedAt())
                .voidedAt(s.voidedAt())
                .voidedByUserId(s.voidedByUserId())
                .voidReason(s.voidReason())
                .build();
    }
}
