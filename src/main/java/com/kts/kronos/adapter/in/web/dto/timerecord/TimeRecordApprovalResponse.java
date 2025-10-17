package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.time.LocalDateTime;

public record TimeRecordApprovalResponse(Long timeRecordId,
                                         String partnerName,
                                         String managerUsername,
                                         LocalDateTime newStartWork,
                                         LocalDateTime newEndWork,
                                         LocalDateTime currentStartWork,
                                         LocalDateTime currentEndWork
) {
}