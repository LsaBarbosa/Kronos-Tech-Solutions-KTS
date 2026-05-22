package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.User;

import java.util.List;
import java.util.Optional;

public record LgpdRequestDetailsResponse(
        LgpdRequestResponse request,
        EmployeeSummaryResponse employee,
        CompanySummaryResponse company,
        UserSummaryResponse assignedTo,
        List<LgpdRequestHistoryResponse> history
) {
    public static LgpdRequestDetailsResponse fromDomain(
            LgpdRequest request,
            Employee employee,
            Company company,
            Optional<User> assignedTo,
            List<LgpdRequestHistory> history
    ) {
        return new LgpdRequestDetailsResponse(
                LgpdRequestResponse.fromDomain(request),
                EmployeeSummaryResponse.fromDomain(employee),
                CompanySummaryResponse.fromDomain(company),
                assignedTo.map(UserSummaryResponse::fromDomain).orElse(null),
                history.stream()
                        .map(LgpdRequestHistoryResponse::fromDomain)
                        .toList()
        );
    }
}
