package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
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
@Table(name = "tb_anonymization_consolidated_result")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AnonymizationConsolidatedResultEntity {
    @Id
    @Column(name = "consolidated_execution_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID consolidatedExecutionId;

    @Column(name = "request_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID requestId;

    @Column(name = "employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "company_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "requested_by_user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consolidated_status", nullable = false, length = 50)
    private AnonymizationConsolidatedStatus consolidatedStatus;

    @Column(name = "execution_mode", nullable = false, length = 20)
    private String executionMode;

    @Column(name = "total_scanned", nullable = false)
    private long totalScanned;

    @Column(name = "total_affected", nullable = false)
    private long totalAffected;

    @Column(name = "total_skipped", nullable = false)
    private long totalSkipped;

    @Column(name = "total_errors", nullable = false)
    private long totalErrors;

    @Column(name = "failed_domains", columnDefinition = "TEXT")
    private String failedDomains;

    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AnonymizationConsolidatedResult toDomain() {
        return new AnonymizationConsolidatedResult(
                consolidatedExecutionId,
                employeeId,
                companyId,
                requestedByUserId,
                consolidatedStatus,
                executionMode,
                startedAt,
                finishedAt,
                totalScanned,
                totalAffected,
                totalSkipped,
                totalErrors,
                null,
                failedDomains != null ? java.util.Arrays.asList(failedDomains.split(",")) : java.util.List.of(),
                warnings != null ? java.util.Arrays.asList(warnings.split("\n")) : java.util.List.of()
        );
    }
}
