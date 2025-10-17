package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.time.LocalDateTime;
import java.util.List;

public record TimeRecordApprovalResponse(Long timeRecordId,
                                         String partnerName,
                                         String managerUsername,
                                         LocalDateTime newStartWork,
                                         LocalDateTime newEndWork,
                                         LocalDateTime currentStartWork,
                                         LocalDateTime currentEndWork,
                                         List<BreakApprovalResponse> breakRequests

) {
}