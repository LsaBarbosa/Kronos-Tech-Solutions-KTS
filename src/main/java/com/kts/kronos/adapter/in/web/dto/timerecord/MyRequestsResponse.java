package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record MyRequestsResponse(
        List<MyRequestItemResponse> items,
        String source
) {
}
