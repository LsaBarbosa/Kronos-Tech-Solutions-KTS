package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.kts.kronos.constants.Messages.END_DATE_BEFORE_START_DATE;
import static com.kts.kronos.constants.Messages.EXPORT_PERIOD_TOO_LARGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AejServiceFeature52RangeQueryTest {

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

    @BeforeEach
    void setUpLegalFields() {
        ReflectionTestUtils.setField(service, "inpiNumber", "999999999");
        ReflectionTestUtils.setField(service, "developerName", "KRONOS TECH SOLUTIONS");
        ReflectionTestUtils.setField(service, "softwareVersion", "1.0");
    }

    @Test
    @DisplayName("generateAej: usa busca em lote por employeeIds e intervalo")
    void shouldLoadRecordsByRangeInSingleBatch() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        var company = new Company(
                companyId,
                "KTS",
                "00000000000100",
                "empresa@kts.com",
                true,
                null,
                null,
                0,
                0
        );

        var employee = new Employee(
                employeeId,
                "Manager",
                "12345678901",
                "12345678901",
                "Dev",
                "manager@kts.com",
                1000.0,
                "11999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );

        byte[] signed = "signed-aej".getBytes(StandardCharsets.ISO_8859_1);

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(recordRepository.findByEmployeeIdsAndRange(
                List.of(employeeId),
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        )).thenReturn(List.of());
        when(signatureService.signData(any(byte[].class))).thenReturn(signed);

        var output = new ByteArrayOutputStream();
        service.generateAej(companyId, startDate, endDate, output);

        assertArrayEquals(signed, output.toByteArray());
        verify(recordRepository).findByEmployeeIdsAndRange(
                List.of(employeeId),
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );
        verify(recordRepository, never()).findByEmployeeId(any());
        verify(signatureService).signData(any(byte[].class));
    }

    @Test
    @DisplayName("generateAej: bloqueia quando data final é anterior à inicial")
    void shouldRejectWhenEndDateIsBeforeStartDate() {
        UUID companyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 9);

        var output = new ByteArrayOutputStream();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.generateAej(companyId, startDate, endDate, output)
        );

        assertEquals(END_DATE_BEFORE_START_DATE, exception.getMessage());
        verifyNoInteractions(companyProvider, employeeProvider, recordRepository, signatureService);
    }

    @Test
    @DisplayName("generateAej: bloqueia períodos acima do limite de segurança")
    void shouldRejectWhenPeriodIsTooLarge() {
        UUID companyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2027, 1, 2);

        var output = new ByteArrayOutputStream();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.generateAej(companyId, startDate, endDate, output)
        );

        assertEquals(
                String.format(EXPORT_PERIOD_TOO_LARGE, LegalExportRangeGuard.MAX_EXPORT_RANGE_DAYS),
                exception.getMessage()
        );
        verifyNoInteractions(companyProvider, employeeProvider, recordRepository, signatureService);
    }
}
