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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.FAILURE_TO_GENERAT_AEJ;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AejServiceTest {

    @Mock CompanyProvider companyProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock TimeRecordProvider recordRepository;
    @Mock DigitalSignatureService signatureService;

    @InjectMocks AejService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "inpiNumber", "123456789");
        ReflectionTestUtils.setField(service, "developerName", "DEV NAME TEST");
        ReflectionTestUtils.setField(service, "softwareVersion", "9.9");
    }

    @Test
    void generateAejThrowsWhenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.generateAej(companyId, LocalDate.now(), LocalDate.now(), new ByteArrayOutputStream()));
    }

    @Test
    void generateAejGeneratesAllMainRecordsAndWritesSignedContent() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        Company company = new Company(
                companyId,
                "Empresa Teste",
                "12.345.678/0001-90",
                "empresa@test.com",
                true,
                null,
                null,
                0,
                0
        );

        Employee employee = new Employee(
                employeeId,
                "Nome Muito Longo ".repeat(20),
                "111.222.333-44",
                "PIS",
                "DEV",
                "mail@test.com",
                1000,
                "(11)99999-8888",
                true,
                null,
                companyId,
                null,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                Set.of()
        );

        TimeRecord normalRecord = new TimeRecord(
                1L,
                LocalDateTime.of(2025, 1, 10, 8, 0),
                LocalDateTime.of(2025, 1, 10, 17, 0),
                StatusRecord.CREATED,
                false,
                true,
                employeeId,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2025, 1, 10, 8, 0),
                LocalDateTime.of(2025, 1, 10, 17, 0)
        );

        TimeRecord editedRecord = new TimeRecord(
                2L,
                LocalDateTime.of(2025, 1, 11, 9, 0),
                null,
                StatusRecord.UPDATED,
                true,
                true,
                employeeId,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2025, 1, 11, 8, 0),
                null
        );

        TimeRecord vacationRecord = new TimeRecord(
                3L,
                LocalDateTime.of(2025, 1, 12, 8, 0),
                null,
                StatusRecord.VACATION,
                false,
                true,
                employeeId,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2025, 1, 12, 8, 0),
                null
        );

        TimeRecord outOfRangeRecord = new TimeRecord(
                4L,
                LocalDateTime.of(2024, 12, 31, 8, 0),
                LocalDateTime.of(2024, 12, 31, 17, 0),
                StatusRecord.CREATED,
                false,
                true,
                employeeId,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2024, 12, 31, 8, 0),
                LocalDateTime.of(2024, 12, 31, 17, 0)
        );

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(outOfRangeRecord, vacationRecord, editedRecord, normalRecord));

        byte[] signed = "ASSINADO".getBytes(StandardCharsets.ISO_8859_1);
        when(signatureService.signData(any())).thenReturn(signed);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.generateAej(companyId, startDate, endDate, output);

        assertArrayEquals(signed, output.toByteArray());

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(signatureService).signData(captor.capture());
        String content = new String(captor.getValue(), StandardCharsets.ISO_8859_1);

        assertTrue(content.contains("01|1|12345678000190||Empresa Teste|2025-01-01|2025-01-31|"));
        assertTrue(content.contains("02|001|4|123456789|"));
        assertTrue(content.contains("03|000000001|11122233344|"));
        assertTrue(content.contains("04|H000000001|480|0800|1200|1300|1700|"));
        assertTrue(content.contains("05|000000001|2025-01-10T08:00:00-0300|001|E||O|H000000001||"));
        assertTrue(content.contains("05|000000001|2025-01-10T17:00:00-0300|001|S||O|H000000001||"));
        assertTrue(content.contains("05|000000001|2025-01-11T09:00:00-0300|001|E||I|H000000001||"));
        assertTrue(content.contains("07|000000001|04|2025-01-12|480||"));
        assertTrue(content.contains("08|KRONOS SYSTEM|9.9|1|00000000000000|DEV NAME TEST|suporte@kronos.com.br|"));
        assertTrue(content.contains("99|"));
        assertFalse(content.contains("2024-12-31"));
        assertTrue(content.contains("\r\n"));
    }

    @Test
    void generateAejThrowsRuntimeExceptionWhenSigningFails() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Company company = new Company(companyId, "Empresa", "12345678000190", "x@y.com", true, null, null, 0, 0);
        Employee employee = new Employee(
                employeeId,
                "Emp",
                "11122233344",
                "pis",
                "job",
                "e@e.com",
                100,
                null,
                true,
                null,
                companyId,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(signatureService.signData(any())).thenThrow(new RuntimeException("erro assinatura"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.generateAej(companyId, LocalDate.now(), LocalDate.now(), new ByteArrayOutputStream()));

        assertTrue(ex.getMessage().startsWith(FAILURE_TO_GENERAT_AEJ));
    }

    @Test
    void generateAejThrowsRuntimeExceptionWhenOutputWriteFails() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company(companyId, "Empresa", "12345678000190", "x@y.com", true, null, null, 0, 0);

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of());
        when(signatureService.signData(any())).thenReturn("OK".getBytes(StandardCharsets.ISO_8859_1));

        OutputStream failingOutput = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("falha de escrita");
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.generateAej(companyId, LocalDate.now(), LocalDate.now(), failingOutput));

        assertTrue(ex.getMessage().startsWith(FAILURE_TO_GENERAT_AEJ));
    }

    @Test
    void privateMethodsShouldCoverAllBranches() throws Exception {
        Company company = new Company(UUID.randomUUID(), "Nome", "11.222.333/0001-44", "a@a", true, null, null, 0, 0);
        Employee employeeWithDefaults = new Employee(
                UUID.randomUUID(), "Funcionario", "111.222.333-44", "pis", "job", "a@a", 0, null,
                true, null, UUID.randomUUID(), null, false, null,
                null, null, null, null,
                null, null, null, null, null
        );

        Method generateType04 = AejService.class.getDeclaredMethod("generateType04", String.class, Employee.class);
        generateType04.setAccessible(true);
        String type04 = (String) generateType04.invoke(service, "H000000001", employeeWithDefaults);
        assertEquals("04|H000000001|480|0800|1200|1300|1700|", type04);

        Method formatOnlyNumbers = AejService.class.getDeclaredMethod("formatOnlyNumbers", String.class);
        formatOnlyNumbers.setAccessible(true);
        assertEquals("", formatOnlyNumbers.invoke(service, new Object[]{null}));
        assertEquals("123", formatOnlyNumbers.invoke(service, "a1b2c3"));

        Method formatText = AejService.class.getDeclaredMethod("formatText", String.class, int.class);
        formatText.setAccessible(true);
        assertEquals("", formatText.invoke(service, null, 10));
        assertEquals("abc", formatText.invoke(service, "abc", 10));
        assertEquals("ab", formatText.invoke(service, "abcd", 2));

        Method determineSource = AejService.class.getDeclaredMethod("determineSource", boolean.class, LocalDateTime.class, LocalDateTime.class);
        determineSource.setAccessible(true);
        LocalDateTime dt = LocalDateTime.of(2025, 1, 1, 8, 0);
        assertEquals("O", determineSource.invoke(service, false, dt, dt));
        assertEquals("I", determineSource.invoke(service, true, dt, dt));

        Method isAbsence = AejService.class.getDeclaredMethod("isAbsence", TimeRecord.class);
        isAbsence.setAccessible(true);
        TimeRecord vacation = new TimeRecord(1L, LocalDateTime.now(), null, StatusRecord.VACATION, false, true,
                UUID.randomUUID(), null, null, null, null, null, null, null, null);
        TimeRecord created = new TimeRecord(1L, LocalDateTime.now(), null, StatusRecord.CREATED, false, true,
                UUID.randomUUID(), null, null, null, null, null, null, null, null);
        assertEquals(true, isAbsence.invoke(service, vacation));
        assertEquals(false, isAbsence.invoke(service, created));

        Method generateType05Lines = AejService.class.getDeclaredMethod("generateType05Lines", String.class, String.class, TimeRecord.class);
        generateType05Lines.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> linesNoAbsence = (List<String>) generateType05Lines.invoke(service, "1", "H1", created.withCheckout(LocalDateTime.now()));
        assertEquals(2, linesNoAbsence.size());
        @SuppressWarnings("unchecked")
        List<String> linesAbsence = (List<String>) generateType05Lines.invoke(service, "1", "H1", vacation);
        assertTrue(linesAbsence.isEmpty());

        Method generateType01 = AejService.class.getDeclaredMethod("generateType01", Company.class, LocalDate.class, LocalDate.class);
        generateType01.setAccessible(true);
        String t01 = (String) generateType01.invoke(service, company, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2));
        assertTrue(t01.startsWith("01|1|11222333000144||Nome|2025-01-01|2025-01-02|"));

        Method formatDateTimeIso = AejService.class.getDeclaredMethod("formatDateTimeIso", LocalDateTime.class);
        formatDateTimeIso.setAccessible(true);
        assertEquals("2025-01-01T08:00:00-0300", formatDateTimeIso.invoke(service, dt));

        Method generateType07 = AejService.class.getDeclaredMethod("generateType07", String.class, TimeRecord.class);
        generateType07.setAccessible(true);
        TimeRecord timeOffWithRange = new TimeRecord(5L,
                LocalDateTime.of(2025, 2, 2, 8, 0),
                LocalDateTime.of(2025, 2, 2, 12, 0),
                StatusRecord.TIME_OFF, false, true, UUID.randomUUID(),
                null, null, null, null, null, null, null, null);
        String t07 = (String) generateType07.invoke(service, "9", timeOffWithRange);
        assertEquals("07|9|05|2025-02-02|240||", t07);
    }
}
