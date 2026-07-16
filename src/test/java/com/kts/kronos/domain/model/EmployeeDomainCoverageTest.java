package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeDomainCoverageTest {

    private Employee buildBase(String faceS3ObjectKey, LocalTime workStart, LocalTime workEnd,
                                LocalTime breakStart, LocalTime breakEnd) {
        return new Employee(
                UUID.randomUUID(), "Full Name", "12345678901", "98765432100",
                "Dev", "dev@kts.com", 5000.0, "11999999999", true,
                new Address("Rua A", "10", "01001000", "SP", "SP"),
                UUID.randomUUID(), LocalDateTime.now(), false,
                faceS3ObjectKey,
                workStart, workEnd, breakStart, breakEnd,
                WorkScheduleType.TRADITIONAL_5X2, LocalDate.now(),
                DayOfWeek.MONDAY, null, Set.of()
        );
    }

    // ── hasFaceImage: null faceS3ObjectKey → short-circuit FALSE ─────────────────

    @Test
    void hasFaceImage_withNullKey_returnsFalse() {
        Employee emp = buildBase(null, null, null, null, null);
        assertFalse(emp.hasFaceImage());
    }

    // ── hasFaceImage: blank faceS3ObjectKey → !isBlank()=FALSE → FALSE ──────────

    @Test
    void hasFaceImage_withBlankKey_returnsFalse() {
        Employee emp = buildBase("   ", null, null, null, null);
        assertFalse(emp.hasFaceImage());
    }

    // ── getDailyWorkMinutes: workStart≠null, workEnd=null → || second part TRUE ──

    @Test
    void getDailyWorkMinutes_withNullWorkEndTime_returns480() {
        Employee emp = buildBase(null, LocalTime.of(8, 0), null, null, null);
        assertEquals(480L, emp.getDailyWorkMinutes());
    }

    // ── getDailyWorkMinutes: work times set, no break → && FALSE branch ──────────

    @Test
    void getDailyWorkMinutes_withNoBreakTimes_returnsWorkDuration() {
        // workStart=8:00, workEnd=16:00, no break → 480 min, no subtraction
        Employee emp = buildBase(null, LocalTime.of(8, 0), LocalTime.of(16, 0), null, null);
        assertEquals(480L, emp.getDailyWorkMinutes());
    }

    // ── getDailyWorkMinutes: breakStart≠null, breakEnd=null → && second=FALSE → no subtraction ──

    @Test
    void getDailyWorkMinutes_withBreakStartButNullBreakEnd_returnsFullDuration() {
        // breakStart=12:00, breakEnd=null → breakStart!=null=TRUE, breakEnd!=null=FALSE → FALSE
        Employee emp = buildBase(null, LocalTime.of(8, 0), LocalTime.of(16, 0),
                LocalTime.of(12, 0), null);
        assertEquals(480L, emp.getDailyWorkMinutes()); // no subtraction
    }

    // ── getDailyWorkMinutes: both break times set → && TRUE → subtraction applied ──

    @Test
    void getDailyWorkMinutes_withFullBreakTimes_returnsReducedDuration() {
        // breakStart=12:00, breakEnd=13:00 → && both TRUE → 480 - 60 = 420 min
        Employee emp = buildBase(null, LocalTime.of(8, 0), LocalTime.of(16, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0));
        assertEquals(420L, emp.getDailyWorkMinutes());
    }

    // ── anonymize: covers L=1 (the return statement + constructor call) ──────────

    @Test
    void anonymize_returnsAnonymizedEmployee() {
        Employee emp = buildBase("face/key.jpg", LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0));
        UUID deletedBy = UUID.randomUUID();
        LocalDateTime deletedAt = LocalDateTime.now();

        Employee anonymized = emp.anonymize("ANONIMO", "***.***.***-**", "anon@anon.com", "***.***.***-*",
                deletedBy, deletedAt, "LGPD request");

        assertEquals("ANONIMO", anonymized.fullName());
        assertEquals("***.***.***-**", anonymized.cpf());
        assertEquals("anon@anon.com", anonymized.email());
        assertNull(anonymized.phone());
        assertNull(anonymized.address());
        assertFalse(anonymized.active());
        assertEquals(deletedBy, anonymized.deletedBy());
        assertEquals(deletedAt, anonymized.deletedAt());
        assertEquals("LGPD request", anonymized.deactivationReason());
    }
}
