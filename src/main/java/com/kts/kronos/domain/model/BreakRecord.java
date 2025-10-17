package com.kts.kronos.domain.model;

import java.time.LocalDateTime;

public record BreakRecord(Long breakRecordId,
                          Long timeRecordId,
                          LocalDateTime startBreak,
                          LocalDateTime endBreak,
                          boolean active
) {
    // Construtor para nova pausa (usado no service)
    public BreakRecord(Long timeRecordId, LocalDateTime startBreak) {
        this(null, timeRecordId, startBreak, null, true);
    }

    public BreakRecord withId(Long id) {
        return new BreakRecord(id, timeRecordId, startBreak, endBreak, active);
    }

    public BreakRecord withEndBreak(LocalDateTime endBreak) {
        return new BreakRecord(breakRecordId, timeRecordId, startBreak, endBreak, active);
    }

    public BreakRecord withActive(boolean active) {
        return new BreakRecord(breakRecordId, timeRecordId, startBreak, endBreak, active);
    }
}