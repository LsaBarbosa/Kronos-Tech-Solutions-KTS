package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_security_incident_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityIncidentReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "incident_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID incidentId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "generated_by_user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID generatedByUserId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "report_content", columnDefinition = "TEXT")
    private String reportContent;
}
