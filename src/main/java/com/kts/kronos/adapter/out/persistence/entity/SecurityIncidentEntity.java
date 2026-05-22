package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_security_incident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityIncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "detected_at")
    private Instant detectedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SecurityIncidentSeverity severity;

    @Column(nullable = false, name = "personal_data_involved")
    private boolean personalDataInvolved;

    @Column(nullable = false, name = "sensitive_data_involved")
    private boolean sensitiveDataInvolved;

    @Column(name = "affected_subjects_estimate")
    private Integer affectedSubjectsEstimate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SecurityIncidentStatus status;

    @Column(name = "notified_anpd_at")
    private Instant notifiedAnpdAt;

    @Column(name = "notified_subjects_at")
    private Instant notifiedSubjectsAt;

    @Column(name = "created_by_user_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID createdByUserId;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public SecurityIncident toDomain() {
        return new SecurityIncident(
                incidentId,
                title,
                description,
                detectedAt,
                confirmedAt,
                severity,
                personalDataInvolved,
                sensitiveDataInvolved,
                affectedSubjectsEstimate,
                status,
                notifiedAnpdAt,
                notifiedSubjectsAt,
                createdByUserId,
                createdAt,
                updatedAt
        );
    }
}
