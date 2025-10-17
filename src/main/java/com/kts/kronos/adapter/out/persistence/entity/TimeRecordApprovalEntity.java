package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_time_record_approval")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeRecordApprovalEntity {
    @Id
    @Column(name = "time_record_id")
    private Long timeRecordId;

    @Column(name = "requesting_employee_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID requestingEmployeeId;

    @Column(name = "manager_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID managerId;

    @Column(name = "new_start_work", nullable = false)
    private LocalDateTime newStartWork;

    @Column(name = "new_end_work", nullable = false)
    private LocalDateTime newEndWork;

    @CreationTimestamp // Garante que o Hibernate preencha a data automaticamente
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TimeRecordApprovalRequest toDomain() {
        return new TimeRecordApprovalRequest(
                timeRecordId,
                requestingEmployeeId,
                managerId,
                newStartWork,
                newEndWork,
                createdAt
        );
    }

    public static TimeRecordApprovalEntity fromDomain(TimeRecordApprovalRequest domain) {
        return TimeRecordApprovalEntity.builder()
                .timeRecordId(domain.timeRecordId())
                .requestingEmployeeId(domain.requestingEmployeeId())
                .managerId(domain.managerId())
                .newStartWork(domain.newStartWork())
                .newEndWork(domain.newEndWork())
                .createdAt(domain.createdAt())
                .build();
    }
}