package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record TimeRecordPageResponse(List<TimeRecordResponse> records,
                                     int totalPages,
                                     long totalElements,
                                     int currentPage,
                                     boolean isFirst,
                                     boolean isLast) {
}