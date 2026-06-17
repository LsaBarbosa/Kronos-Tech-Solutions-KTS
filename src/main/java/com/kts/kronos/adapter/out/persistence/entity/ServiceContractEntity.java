package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
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
@Table(name = "tb_service_contract")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceContractEntity {

    @Id
    @Column(name = "contract_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID contractId;

    @Column(name = "company_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "source_document_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID sourceDocumentId;

    @Column(name = "source_document_owner_employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID sourceDocumentOwnerEmployeeId;

    @Column(name = "created_by_user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID createdByUserId;

    @Column(name = "created_by_employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID createdByEmployeeId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "document_hash_sha256", nullable = false, length = 64)
    private String documentHashSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ServiceContractStatus status;

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
