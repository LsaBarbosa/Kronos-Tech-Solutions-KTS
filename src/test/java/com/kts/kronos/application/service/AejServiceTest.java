package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AejServiceTest {

    @InjectMocks
    private AejService service;

    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private TimeRecordProvider recordRepository;
    @Mock
    private DigitalSignatureService signatureService;

    @Test
    @DisplayName("generateAej: deve gerar layout completo e assinar o conteudo")
    void shouldGenerateAejWithRecordsAbsencesDefaultsAndSignature() {
        UUID companyId = UUID.randomUUID();
        UUID firstEmployeeId = UUID.randomUUID();
        UUID secondEmployeeId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);
        Company company = company(companyId, "KTS ".repeat(50));
        Employee firstEmployee = employee(firstEmployeeId, companyId, "Ana Paula", "123.456.789-01", null, null);
        Employee secondEmployee = employee(
                secondEmployeeId,
                companyId,
                "Bruno Silva",
                "987.654.321-00",
                "11999999999",
                LocalTime.of(7, 30)
        );

        LocalDateTime firstStart = LocalDateTime.of(2026, 4, 2, 8, 0);
        LocalDateTime firstEnd = LocalDateTime.of(2026, 4, 2, 17, 0);
        LocalDateTime editedStart = LocalDateTime.of(2026, 4, 3, 8, 15);
        LocalDateTime editedEnd = LocalDateTime.of(2026, 4, 3, 17, 10);
        LocalDateTime absenceStart = LocalDateTime.of(2026, 4, 4, 8, 0);
        List<TimeRecord> records = List.of(
                record(firstEmployeeId, StatusRecord.CREATED, false, firstStart, firstEnd, firstStart, firstEnd),
                record(firstEmployeeId, StatusRecord.UPDATED, true, editedStart, editedEnd, firstStart, firstEnd),
                record(firstEmployeeId, StatusRecord.TIME_OFF, false, absenceStart, absenceStart.plusHours(4), null, null),
                record(firstEmployeeId, StatusRecord.VACATION, false, absenceStart.plusDays(1), null, null, null),
                record(firstEmployeeId, StatusRecord.ABSENCE, false, absenceStart.plusDays(2), null, null, null)
        );

        ReflectionTestUtils.setField(service, "inpiNumber", "123456789");
        ReflectionTestUtils.setField(service, "softwareVersion", "2.1.0");
        ReflectionTestUtils.setField(service, "developerName", "KRONOS ".repeat(40));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(firstEmployee, secondEmployee));
        when(recordRepository.findByEmployeeIdsAndRange(
                eq(List.of(firstEmployeeId, secondEmployeeId)),
                eq(startDate.atStartOfDay()),
                eq(endDate.atTime(23, 59, 59))
        )).thenReturn(records);
        when(signatureService.signData(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.generateAej(companyId, startDate, endDate, output);

        String text = output.toString(StandardCharsets.ISO_8859_1);
        assertTrue(text.startsWith("01|1|12345678000199||"));
        assertTrue(text.contains("\r\n02|001|4|123456789|\r\n"));
        assertTrue(text.contains("03|000000001|12345678901|Ana Paula||"));
        assertTrue(text.contains("04|H000000001|480|0800|1200|1300|1700|"));
        assertTrue(text.contains("05|000000001|2026-04-02T08:00:00-0300|001|E||O|H000000001||"));
        assertTrue(text.contains("05|000000001|2026-04-03T17:10:00-0300|001|S||I|H000000001||"));
        assertTrue(text.contains("07|000000001|05|2026-04-04|240||"));
        assertTrue(text.contains("07|000000001|04|2026-04-05|480||"));
        assertTrue(text.contains("07|000000001|05|2026-04-06|480||"));
        assertTrue(text.contains("03|000000002|98765432100|Bruno Silva|11999999999|"));
        assertTrue(text.contains("04|H000000002|570|0730|1200|1300|1700|"));
        assertTrue(text.contains("\r\n08|KRONOS SYSTEM|2.1.0|1|00000000000000|"));
        assertTrue(text.endsWith("99|\r\n"));
    }

    @Test
    @DisplayName("generateAej: deve falhar quando empresa nao existir")
    void shouldFailWhenCompanyDoesNotExist() {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.generateAej(companyId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), new ByteArrayOutputStream())
        );
    }

    @Test
    @DisplayName("generateAej: deve encapsular falha de assinatura")
    void shouldWrapSignatureFailure() {
        UUID companyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company(companyId, "KTS")));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of());
        when(recordRepository.findByEmployeeIdsAndRange(
                eq(List.of()),
                eq(startDate.atStartOfDay()),
                eq(endDate.atTime(23, 59, 59))
        )).thenReturn(List.of());
        when(signatureService.signData(any())).thenThrow(new IllegalStateException("certificate unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.generateAej(companyId, startDate, endDate, new ByteArrayOutputStream())
        );

        assertEquals("Falha ao gerar arquivo fiscal AEJ: ", exception.getMessage());
    }

    @Test
    @DisplayName("generateAej: deve tolerar CNPJ nulo ao formatar apenas numeros")
    void shouldGenerateAejWhenCompanyCnpjIsNull() {
        UUID companyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);
        Company company = new Company(
                companyId,
                "KTS",
                null,
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                null,
                0,
                0
        );
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of());
        when(recordRepository.findByEmployeeIdsAndRange(
                eq(List.of()),
                eq(startDate.atStartOfDay()),
                eq(endDate.atTime(23, 59, 59))
        )).thenReturn(List.of());
        when(signatureService.signData(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.generateAej(companyId, startDate, endDate, output);

        assertTrue(output.toString(StandardCharsets.ISO_8859_1).startsWith("01|1|||KTS|"));
    }

    private static Company company(UUID companyId, String name) {
        return new Company(
                companyId,
                name,
                "12.345.678/0001-99",
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                null,
                0,
                0
        );
    }

    private static Employee employee(
            UUID employeeId,
            UUID companyId,
            String fullName,
            String cpf,
            String phone,
            LocalTime workStartTime
    ) {
        return new Employee(
                employeeId,
                fullName,
                cpf,
                "12345678901",
                "Analista",
                "employee@kts.com",
                1000.0,
                phone,
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                companyId,
                null,
                false,
                null,
                workStartTime,
                workStartTime == null ? null : LocalTime.of(17, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static TimeRecord record(
            UUID employeeId,
            StatusRecord status,
            boolean edited,
            LocalDateTime startWork,
            LocalDateTime endWork,
            LocalDateTime originalStartWork,
            LocalDateTime originalEndWork
    ) {
        return new TimeRecord(
                null,
                startWork,
                endWork,
                status,
                edited,
                true,
                employeeId,
                null,
                null,
                null,
                null,
                null,
                null,
                originalStartWork,
                originalEndWork
        );
    }
}
