package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers remaining isBalanceZeroStatus branches (L161):
 * - TIME_OFF, IMPLICIT_BREAK, REQUEST_VACATION, VACATION, VACATION_REJECTED
 * Each triggers the else-if path that evaluates deeper OR conditions in the chain.
 */
class TimeRecordResponseCoverageTest {

    @ParameterizedTest
    @EnumSource(value = StatusRecord.class, names = {
            "TIME_OFF",
            "IMPLICIT_BREAK",
            "REQUEST_VACATION",
            "VACATION",
            "VACATION_REJECTED"
    })
    void fromDomain_zeroBalanceStatuses_producePlusZeroBalance(StatusRecord status) {
        TimeRecord record = buildRecord(status);
        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null   // dailyBalance=null → falls through to isBalanceZeroStatus
        );
        assertEquals("+00:00", response.balance(),
                "Expected +00:00 balance for status " + status);
    }

    // BR L117 TRUE: edited=false, originalStart=null, originalEnd != endWork
    // → hasTreatment TRUE via the 3rd OR condition (originalEndDateTime != null && !equals(endDateTime))
    @Test
    void fromDomain_notEditedButOriginalEndDiffers_hasTreatmentTrue() {
        UUID employeeId = UUID.randomUUID();
        var startWork = LocalDateTime.of(2026, 4, 21, 8, 0);
        var endWork   = LocalDateTime.of(2026, 4, 21, 17, 0);
        var originalEnd = LocalDateTime.of(2026, 4, 21, 16, 0); // different from endWork
        TimeRecord record = new TimeRecord(
                10L, startWork, endWork,
                StatusRecord.CREATED,
                false,  // edited=false
                true,
                employeeId,
                -2.53, -44.30, -2.54, -44.31,
                100L, 101L,
                null,        // originalStartWork=null → B=false at L116
                originalEnd  // originalEndWork != endWork → C=true at L117
        );
        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record, Duration.ofHours(8), null, null, null
        );
        assertTrue(response.hasTreatment(),
                "hasTreatment should be true because originalEndWork differs from endWork");
    }

    private static TimeRecord buildRecord(StatusRecord status) {
        return new TimeRecord(
                10L,
                LocalDateTime.of(2026, 4, 21, 0, 0),
                LocalDateTime.of(2026, 4, 21, 0, 0),
                status,
                false,
                true,
                UUID.randomUUID(),
                -2.53, -44.30, -2.54, -44.31,
                100L, 101L,
                null, null
        );
    }
}
