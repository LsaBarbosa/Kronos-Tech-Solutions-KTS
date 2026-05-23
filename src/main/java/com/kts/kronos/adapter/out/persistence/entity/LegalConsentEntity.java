package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_legal_consent")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Builder
public class LegalConsentEntity {
    @Id
    @Column(name = "consent_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID consentId;

    @Column(name = "employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "user_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 80)
    private ConsentType consentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_basis", nullable = false, length = 80)
    private LegalBasis legalBasis;

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    @Column(name = "version", nullable = false, length = 30)
    private String version;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "evidence_document_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID evidenceDocumentId;

    @Column(name = "evidence_hash_sha256", length = 128)
    private String evidenceHashSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "retention_applied_at")
    private Instant retentionAppliedAt;

    @Column(name = "retention_policy_code", length = 100)
    private String retentionPolicyCode;
}
