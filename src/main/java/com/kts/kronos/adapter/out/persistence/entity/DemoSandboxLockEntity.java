package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_demo_sandbox_lock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoSandboxLockEntity {

    @Id
    @Column(name = "lock_key", length = 100)
    private String lockKey;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "locked_by")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID lockedBy;

    @Column(name = "job_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID jobId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
