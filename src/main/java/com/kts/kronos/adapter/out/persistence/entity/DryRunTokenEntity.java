package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "tb_dry_run_token", indexes = {
        @Index(name = "idx_request_id", columnList = "request_id"),
        @Index(name = "idx_token_value", columnList = "token_value"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DryRunTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "token_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID tokenId;

    @Column(name = "request_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID requestId;

    @Column(name = "token_value", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID tokenValue;

    @Column(name = "employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "company_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "generated_by_user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID generatedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DryRunTokenStatus status;

    public enum DryRunTokenStatus {
        PENDING,
        CONSUMED,
        EXPIRED
    }
}
