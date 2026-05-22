package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record LgpdRequestAdminListResponse(
        UUID requestId,
        String employeeFullName,
        String companyName,
        LgpdRequestType type,
        LgpdRequestStatus status,
        Instant createdAt,
        String assignedToName,
        Instant updatedAt,
        Boolean isOverdue
) {
    public static LgpdRequestAdminListResponse fromDomain(
            LgpdRequest request,
            Employee employee,
            Company company,
            Optional<User> assignedTo
    ) {
        return new LgpdRequestAdminListResponse(
                request.requestId(),
                employee.fullName(),
                company.name(),
                request.requestType(),
                request.status(),
                request.createdAt(),
                assignedTo.map(User::username).orElse(null),
                request.updatedAt(),
                false // será calculado em Sprint 5 com SLA
        );
    }
}
