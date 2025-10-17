package com.kts.kronos.domain.model;

import java.time.LocalDateTime;

public record BreakRecordApprovalRequest(Long breakApprovalId, // ID da aprovação da pausa (auto-gerado no DB)
                                         Long timeRecordId,     // FK para o registro principal em aprovação
                                         Long breakRecordId,    // PK da pausa original a ser alterada
                                         LocalDateTime newStartBreak,
                                         LocalDateTime newEndBreak
) {
    public BreakRecordApprovalRequest(Long timeRecordId, Long breakRecordId, LocalDateTime newStartBreak, LocalDateTime newEndBreak) {
        this(null, timeRecordId, breakRecordId, newStartBreak, newEndBreak);
    }
}