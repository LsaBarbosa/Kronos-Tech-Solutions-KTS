package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record ManagerMonthlyAlertResponse(
        String month,
        String filter,
        List<ManagerMonthlyAlertItemResponse> items
) {
}
