package com.kts.kronos.application.service;

import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TimeRecordMidnightShiftTest {
    @Test
    void flagAtivaFechaRegistroAbertoNoDiaSeguinte() {
        var company = new Company("Empresa", "12345678000100", "e@e.com", null, null).withMidnightShiftFlag(true);

        assertTrue(TimeRecordService.shouldCloseOpenRecord(company, LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 5)));
    }

    @Test
    void flagDesativadaMantemComportamentoDeNovoRegistro() {
        var company = new Company("Empresa", "12345678000100", "e@e.com", null, null);

        assertFalse(TimeRecordService.shouldCloseOpenRecord(company, LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 5)));
    }

    @Test
    void flagAtivaNaoFechaRegistroComMaisDeUmDiaDeAtraso() {
        var company = new Company("Empresa", "12345678000100", "e@e.com", null, null).withMidnightShiftFlag(true);

        assertFalse(TimeRecordService.shouldCloseOpenRecord(company, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 5)));
    }
}
