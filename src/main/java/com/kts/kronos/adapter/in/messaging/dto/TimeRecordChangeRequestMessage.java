package com.kts.kronos.adapter.in.messaging.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record TimeRecordChangeRequestMessage(Long timeRecordId, UUID partnerEmployeeId, UUID managerId,
                                             LocalDateTime newStartWork,
                                             LocalDateTime newEndWork) implements Serializable {
}
