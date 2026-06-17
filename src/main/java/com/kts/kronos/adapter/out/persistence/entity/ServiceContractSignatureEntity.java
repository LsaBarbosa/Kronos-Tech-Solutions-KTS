package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.enuns.ContractSignatureMethod;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import com.kts.kronos.domain.model.enuns.ContractSignatureType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_service_contract_signature")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceContractSignatureEntity {

    @Id
    @Column(name = "signature_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID signatureId;

    @Column(name = "assignment_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID assignmentId;

    @Column(name = "contract_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID contractId;

    @Column(name = "employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "company_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "signer_user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID signerUserId;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "signed_at_zone", nullable = false, length = 64)
    private String signedAtZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 40)
    private ContractSignatureType signatureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_method", nullable = false, length = 40)
    private ContractSignatureMethod signatureMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ContractSignatureStatus status;

    @Column(name = "signed_document_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID signedDocumentId;

    @Column(name = "contract_document_hash_sha256", nullable = false, length = 64)
    private String contractDocumentHashSha256;

    @Column(name = "signed_pdf_hash_sha256", nullable = false, length = 64)
    private String signedPdfHashSha256;

    @Column(name = "declaration_version", nullable = false, length = 40)
    private String declarationVersion;

    @Column(name = "declaration_hash_sha256", nullable = false, length = 64)
    private String declarationHashSha256;

    @Column(name = "declaration_text", nullable = false, columnDefinition = "TEXT")
    private String declarationText;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by_user_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID voidedByUserId;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;
}
