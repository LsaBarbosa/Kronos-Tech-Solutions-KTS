package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record PendingExitResponse(
        List<PendingExitItemResponse> items,
        String source
) {
}
