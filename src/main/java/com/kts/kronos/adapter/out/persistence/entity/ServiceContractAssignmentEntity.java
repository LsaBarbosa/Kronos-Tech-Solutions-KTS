package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.enuns.ServiceContractAssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_service_contract_assignment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceContractAssignmentEntity {

    @Id
    @Column(name = "assignment_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID assignmentId;

    @Column(name = "contract_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID contractId;

    @Column(name = "company_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "assigned_by_user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID assignedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ServiceContractAssignmentStatus status;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
