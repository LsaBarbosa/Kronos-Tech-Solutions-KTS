package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointMirrorPdfServiceTest {

    @Mock private CompanyProvider companyProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private TimeRecordProvider timeRecordProvider;

    @InjectMocks private PointMirrorPdfService service;

    private UUID employeeId;
    private UUID companyId;
    private Employee employee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        employee = new Employee(
                employeeId, "João", "12345678900", "12345678901", "Dev",
                "joao@kts.com", 5000.0, "11999999999", true, null, companyId,
                LocalDateTime.now(), true, null, LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0), null, null, null, null, null
        );
    }

    @Test
    void generateMirrorShouldReturnPdfWithWorkAndTimeOffRows() {
        var company = new Company(companyId, "KTS", "00111222000199", "contato@kts.com", true, null, null, 0L, 0L);
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);

        var workedDayRecord = new TimeRecord(
                1L,
                start.atTime(9, 0),
                start.atTime(18, 0),
                StatusRecord.CREATED,
                false,
                true,
                employeeId,
                null, null, null, null,
                null, null,
                start.atTime(9, 0),
                start.atTime(18, 0)
        );

        var timeOffRecord = new TimeRecord(
                2L,
                end.atTime(9, 0),
                end.atTime(18, 0),
                StatusRecord.TIME_OFF,
                false,
                true,
                employeeId,
                null, null, null, null,
                null, null,
                end.atTime(9, 0),
                end.atTime(18, 0)
        );

        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(timeRecordProvider.findActiveByEmployeeIdAndStartWorkBetween(eq(employeeId), any(), any()))
                .thenReturn(List.of(workedDayRecord, timeOffRecord));

        var pdf = service.generateMirror(employeeId, start, end);

        assertNotNull(pdf);
        assertTrue(pdf.length > 700);
    }

    @Test
    void generateMirrorShouldThrowWhenEmployeeNotFound() {
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.generateMirror(employeeId, LocalDate.now(), LocalDate.now()));
    }
}
