package com.kts.kronos.adapter.in.messaging.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeRecordChangeRequestMessageTest {

    @Test
    void shouldConvertMessageToDomainRequest() {
        UUID partnerEmployeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 4, 21, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 21, 17, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 21, 18, 0);

        var message = new TimeRecordChangeRequestMessage(42L, partnerEmployeeId, managerId, start, end, createdAt);

        var domain = message.toDomain();

        assertEquals(42L, domain.timeRecordId());
        assertEquals(partnerEmployeeId, domain.requestingEmployeeId());
        assertEquals(managerId, domain.managerId());
        assertEquals(start, domain.newStartWork());
        assertEquals(end, domain.newEndWork());
        assertEquals(createdAt, domain.createdAt());
    }
}
