package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Column(name = "partner_employee_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID partnerEmployeeId;

    @Column(name = "manager_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID managerId;

    @Column(name = "new_start_work", nullable = false)
    private LocalDateTime newStartWork;

    @Column(name = "new_end_work", nullable = false)
    private LocalDateTime newEndWork;

    public TimeRecordApprovalRequest toDomain() {
        return new TimeRecordApprovalRequest(
                timeRecordId, partnerEmployeeId, managerId, newStartWork, newEndWork
        );
    }

    public static TimeRecordApprovalEntity fromDomain(TimeRecordApprovalRequest domain) {
        return TimeRecordApprovalEntity.builder()
                .timeRecordId(domain.timeRecordId())
                .partnerEmployeeId(domain.partnerEmployeeId())
                .managerId(domain.managerId())
                .newStartWork(domain.newStartWork())
                .newEndWork(domain.newEndWork())
                .build();
    }
}