package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_retention_execution_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetentionExecutionLogEntity {
    @Id
    private UUID executionId;

    @Column(nullable = false, length = 100)
    private String policyCode;

    @Column(nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private RetentionResourceType resourceType;

    @Column(nullable = false, length = 30)
    private String executionMode;

    @Column(nullable = false)
    private Instant startedAt;

    @Column
    private Instant finishedAt;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private long scannedCount;

    @Column(nullable = false)
    private long affectedCount;

    @Column(nullable = false)
    private long skippedCount;

    @Column(nullable = false)
    private long errorCount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;
}
