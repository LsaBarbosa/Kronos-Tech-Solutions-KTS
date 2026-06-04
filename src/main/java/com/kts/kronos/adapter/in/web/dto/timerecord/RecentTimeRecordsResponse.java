package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record RecentTimeRecordsResponse(
        List<RecentTimeRecordItemResponse> items,
        String source
) {
}
