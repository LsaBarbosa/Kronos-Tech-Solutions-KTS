package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.AuditLog;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID actorUserId;

    @Column(name = "target_employee_id")
    private UUID targetEmployeeId;

    @Column(nullable = false)
    private String action; // Ex: ACEITE_TERMOS

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "minimized_at")
    private LocalDateTime minimizedAt;

    public AuditLog toDomain() {
        return new AuditLog(
                id,
                actorUserId,
                targetEmployeeId,
                action,
                ipAddress,
                userAgent,
                details,
                timestamp,
                companyId,
                resourceType,
                resourceId,
                correlationId,
                riskLevel
        );
    }
}
