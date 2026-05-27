package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.LegalConsentEntity;
import com.kts.kronos.domain.model.LegalConsent;
import org.springframework.stereotype.Component;

@Component
public class LegalConsentMapper {

    public LegalConsent toDomain(LegalConsentEntity entity) {
        return new LegalConsent(
                entity.getConsentId(),
                entity.getEmployeeId(),
                entity.getUserId(),
                entity.getConsentType(),
                entity.getLegalBasis(),
                entity.getPurpose(),
                entity.getVersion(),
                entity.getContentHashSha256(),
                entity.getGrantedAt(),
                entity.getRevokedAt(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getEvidenceDocumentId(),
                entity.getEvidenceHashSha256(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public LegalConsentEntity toEntity(LegalConsent domain) {
        return LegalConsentEntity.builder()
                .consentId(domain.consentId())
                .employeeId(domain.employeeId())
                .userId(domain.userId())
                .consentType(domain.consentType())
                .legalBasis(domain.legalBasis())
                .purpose(domain.purpose())
                .version(domain.version())
                .contentHashSha256(domain.contentHashSha256())
                .grantedAt(domain.grantedAt())
                .revokedAt(domain.revokedAt())
                .ipAddress(domain.ipAddress())
                .userAgent(domain.userAgent())
                .evidenceDocumentId(domain.evidenceDocumentId())
                .evidenceHashSha256(domain.evidenceHashSha256())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}
