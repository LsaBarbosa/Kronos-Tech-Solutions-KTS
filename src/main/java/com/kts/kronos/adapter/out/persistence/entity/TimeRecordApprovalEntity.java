package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
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

    @OneToMany(mappedBy = "timeRecordApproval", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BreakRecordApprovalEntity> breakApprovals = List.of();

    public TimeRecordApprovalRequest toDomain() {
        return new TimeRecordApprovalRequest(
                timeRecordId,
                requestingEmployeeId,
                managerId,
                newStartWork,
                newEndWork,
                createdAt,
                // Mapeia entidades aninhadas para o domínio
                breakApprovals.stream().map(BreakRecordApprovalEntity::toDomain).toList()
        );
    }

    public static TimeRecordApprovalEntity fromDomain(TimeRecordApprovalRequest domain) {
        TimeRecordApprovalEntity entity = TimeRecordApprovalEntity.builder()
                .timeRecordId(domain.timeRecordId())
                .requestingEmployeeId(domain.requestingEmployeeId())
                .managerId(domain.managerId())
                .newStartWork(domain.newStartWork())
                .newEndWork(domain.newEndWork())
                .createdAt(domain.createdAt())
                .build();

        // Mapeia e anexa as pausas
        List<BreakRecordApprovalEntity> breaks = domain.breakApprovalRequests().stream()
                .map(breakReq -> {
                    BreakRecordApprovalEntity breakEntity = BreakRecordApprovalEntity.fromDomain(breakReq);
                    breakEntity.setTimeRecordApproval(entity); // Seta a referência bidirecional
                    breakEntity.setTimeRecordId(domain.timeRecordId()); // Garante que a FK seja setada
                    return breakEntity;
                }).toList();

        entity.setBreakApprovals(breaks);
        return entity;
    }
}