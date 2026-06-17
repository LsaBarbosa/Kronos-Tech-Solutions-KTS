package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractAssignmentEntity;
import com.kts.kronos.domain.model.ServiceContractAssignment;

public final class ServiceContractAssignmentMapper {

    private ServiceContractAssignmentMapper() {}

    public static ServiceContractAssignment toDomain(ServiceContractAssignmentEntity e) {
        if (e == null) return null;
        return new ServiceContractAssignment(
                e.getAssignmentId(),
                e.getContractId(),
                e.getCompanyId(),
                e.getEmployeeId(),
                e.getAssignedByUserId(),
                e.getStatus(),
                e.getAssignedAt(),
                e.getSignedAt(),
                e.getCancelledAt()
        );
    }

    public static ServiceContractAssignmentEntity toEntity(ServiceContractAssignment a) {
        if (a == null) return null;
        return ServiceContractAssignmentEntity.builder()
                .assignmentId(a.assignmentId())
                .contractId(a.contractId())
                .companyId(a.companyId())
                .employeeId(a.employeeId())
                .assignedByUserId(a.assignedByUserId())
                .status(a.status())
                .assignedAt(a.assignedAt())
                .signedAt(a.signedAt())
                .cancelledAt(a.cancelledAt())
                .build();
    }
}
