package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_demo_job_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoJobAuditEntity {

    @Id
    @Column(name = "audit_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID auditId;

    @Column(name = "job_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID jobId;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "sandbox_key", nullable = false, length = 50)
    private String sandboxKey;

    @Column(name = "actor_user_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID actorUserId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Builder.Default
    @Column(name = "companies_count", nullable = false)
    private int companiesCount = 0;

    @Builder.Default
    @Column(name = "users_count", nullable = false)
    private int usersCount = 0;

    @Builder.Default
    @Column(name = "employees_count", nullable = false)
    private int employeesCount = 0;

    @Builder.Default
    @Column(name = "point_records_count", nullable = false)
    private int pointRecordsCount = 0;

    @Builder.Default
    @Column(name = "documents_count", nullable = false)
    private int documentsCount = 0;

    @Builder.Default
    @Column(name = "requests_count", nullable = false)
    private int requestsCount = 0;

    @Builder.Default
    @Column(name = "files_count", nullable = false)
    private int filesCount = 0;

    @Builder.Default
    @Column(name = "sessions_count", nullable = false)
    private int sessionsCount = 0;

    @Builder.Default
    @Column(name = "cache_keys_count", nullable = false)
    private int cacheKeysCount = 0;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
