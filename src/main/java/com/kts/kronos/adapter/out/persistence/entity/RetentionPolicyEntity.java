package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_retention_policy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionPolicyEntity {
    @Id
    @Column(name = "policy_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID policyId;

    @Column(name = "policy_code", nullable = false, length = 80, unique = true)
    private String policyCode;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 40)
    private RetentionPolicyType policyType;

    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;

    @Column(name = "retention_days")
    private Integer retentionDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 20)
    private RetentionExecutionMode executionMode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "preserve_labor_data", nullable = false)
    private boolean preserveLaborData;

    @Column(name = "preserve_fiscal_data", nullable = false)
    private boolean preserveFiscalData;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
