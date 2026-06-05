package com.kts.kronos.adapter.in.web.dto.timerecord.vacation;

import java.util.List;

public record VacationRequestPageResponse(
        List<VacationRequestResponse> requests,
        int totalPages,
        long totalElements,
        int currentPage,
        boolean isFirst,
        boolean isLast
) {
}
