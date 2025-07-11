package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record TimeRecordListResponse(
        List<TimeRecordResponse> records
) {
}