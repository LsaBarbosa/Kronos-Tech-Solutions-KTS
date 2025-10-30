package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record TimeRecordApprovalPageResponse(List<TimeRecordApprovalResponse> approvals,
                                             int totalPages,
                                             long totalElements,
                                             int currentPage,
                                             boolean isFirst,
                                             boolean isLast) {
}
