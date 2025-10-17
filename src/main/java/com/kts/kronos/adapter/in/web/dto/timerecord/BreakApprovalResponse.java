package com.kts.kronos.adapter.in.web.dto.timerecord;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.BreakRecordApprovalRequest;

import java.time.LocalDateTime;
public record BreakApprovalResponse(
        Long breakRecordId,
        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        LocalDateTime newStartBreak,
        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        LocalDateTime newEndBreak
) {
    public static BreakApprovalResponse fromDomain(BreakRecordApprovalRequest domain) {
        return new BreakApprovalResponse(
                domain.breakRecordId(),
                domain.newStartBreak(),
                domain.newEndBreak()
        );
    }
}