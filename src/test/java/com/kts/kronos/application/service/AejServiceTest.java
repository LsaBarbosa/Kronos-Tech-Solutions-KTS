package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AejServiceTest {

    @Mock CompanyProvider companyProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock TimeRecordProvider recordRepository;
    @Mock DigitalSignatureService signatureService;

    @InjectMocks AejService service;

    @Test
    void generateAejThrowsWhenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.generateAej(companyId, LocalDate.now(), LocalDate.now(), new ByteArrayOutputStream()));
    }

    @Test
    void generateAejWritesSignedPayload() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var company = new Company(companyId, "KTS", "12.345.678/0001-95", "mail@kts.com", true, null, null, 0L, 0L);
        var employee = new Employee(employeeId, "João", "12345678900", "123", "Dev", "j@k.com", 1000, "11", true, null,
                companyId, LocalDateTime.now(), true, null, LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(12, 0), LocalTime.of(13, 0), null, null, null, null, null);
        var record = new TimeRecord(1L, LocalDateTime.of(2025, 1, 10, 9, 0), LocalDateTime.of(2025, 1, 10, 18, 0), StatusRecord.CREATED,
                false, true, employeeId, null, null, null, null, null, null, LocalDateTime.of(2025, 1, 10, 9, 0), LocalDateTime.of(2025, 1, 10, 18, 0));

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(record));
        when(signatureService.signData(any())).thenAnswer(i -> i.getArgument(0));

        var out = new ByteArrayOutputStream();
        service.generateAej(companyId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), out);

        var text = out.toString(StandardCharsets.ISO_8859_1);
        assertTrue(text.contains("01|"));
        assertTrue(text.contains("05|"));
        assertTrue(text.contains("99|"));
    }
}
