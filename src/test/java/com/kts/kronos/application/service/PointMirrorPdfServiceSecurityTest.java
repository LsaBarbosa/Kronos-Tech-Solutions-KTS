package com.kts.kronos.application.service;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.IBlockElement;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.ScheduleExceptionProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.END_DATE_BEFORE_START_DATE;
import static com.kts.kronos.constants.Messages.EXPORT_PERIOD_TOO_LARGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PointMirrorPdfServiceSecurityTest {

    @Mock private CompanyProvider companyProvider;
    @Mock private TimeRecordProvider recordRepository;
    @Mock private DomainAuthorizationService domainAuthorizationService;
    @Mock private ScheduleExceptionProvider scheduleExceptionProvider;

    private PointMirrorPdfService service;

    @BeforeEach
    void setUp() {
        when(scheduleExceptionProvider.findByEmployeeAndDate(any(), any())).thenReturn(Optional.empty());
        var resolver = new ScheduleResolverService(scheduleExceptionProvider, recordRepository);
        service = new PointMirrorPdfService(companyProvider, recordRepository, domainAuthorizationService, null, null, resolver);
    }

    @Test
    @DisplayName("espelho: permite geração para colaborador autorizado")
    void shouldAllowMirrorGenerationForAuthorizedEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = buildEmployee(employeeId, companyId);
        var company = buildCompany(companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findByRange(
                employeeId,
                LocalDate.of(2026, 1, 1).atStartOfDay(),
                LocalDate.of(2026, 1, 1).atTime(23, 59, 59)
        )).thenReturn(List.of());

        byte[] pdf = service.generateMirror(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertTrue(pdf.length > 0);
        verify(recordRepository).findByRange(
                employeeId,
                LocalDate.of(2026, 1, 1).atStartOfDay(),
                LocalDate.of(2026, 1, 1).atTime(23, 59, 59)
        );
    }

    @Test
    @DisplayName("espelho: processa marcações, fim de semana, falta, abono e férias")
    void shouldGenerateMirrorWithDailyVariations() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        Company company = buildCompany(companyId);
        LocalDate startDate = LocalDate.of(2026, 1, 2);
        LocalDate endDate = LocalDate.of(2026, 1, 7);
        List<TimeRecord> records = List.of(
                record(1L, employeeId, StatusRecord.CREATED,
                        LocalDateTime.of(2026, 1, 2, 9, 0),
                        LocalDateTime.of(2026, 1, 2, 17, 0),
                        LocalDateTime.of(2026, 1, 2, 8, 50),
                        LocalDateTime.of(2026, 1, 2, 17, 10)),
                record(2L, employeeId, StatusRecord.IMPLICIT_BREAK,
                        LocalDateTime.of(2026, 1, 2, 12, 0),
                        LocalDateTime.of(2026, 1, 2, 13, 0),
                        null,
                        null),
                record(3L, employeeId, StatusRecord.CREATED,
                        LocalDateTime.of(2026, 1, 3, 10, 0),
                        LocalDateTime.of(2026, 1, 3, 12, 0),
                        null,
                        null),
                record(4L, employeeId, StatusRecord.TIME_OFF,
                        LocalDateTime.of(2026, 1, 5, 9, 0),
                        null,
                        null,
                        null),
                record(5L, employeeId, StatusRecord.VACATION,
                        LocalDateTime.of(2026, 1, 6, 9, 0),
                        null,
                        null,
                        null),
                record(6L, employeeId, StatusRecord.CREATED,
                        null,
                        null,
                        null,
                        null)
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findByRange(
                employeeId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        )).thenReturn(records);

        byte[] pdf = service.generateMirror(employeeId, startDate, endDate);

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("espelho: falha quando empresa do colaborador nao existir")
    void shouldFailWhenCompanyDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(buildEmployee(employeeId, companyId));
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(
                com.kts.kronos.application.exceptions.ResourceNotFoundException.class,
                () -> service.generateMirror(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1))
        );
    }

    @Test
    @DisplayName("espelho: encapsula falha de runtime na montagem do PDF")
    void shouldWrapPdfRuntimeFailure() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(buildCompany(companyId)));

        try (var ignored = mockConstruction(Document.class, (mock, context) ->
                when(mock.add(org.mockito.ArgumentMatchers.any(IBlockElement.class)))
                        .thenThrow(new IllegalStateException("pdf failed")))) {
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.generateMirror(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1))
            );

            assertEquals("Erro na geração do PDF", exception.getMessage());
        }
    }

    @Test
    @DisplayName("espelho: bloqueia geração cross-tenant")
    void shouldBlockMirrorGenerationForCrossTenantEmployee() {
        UUID employeeId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId))
                .thenThrow(new ForbiddenException("forbidden"));

        assertThrows(ForbiddenException.class,
                () -> service.generateMirror(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)));

        verify(companyProvider, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("espelho: bloqueia período com data final anterior à inicial")
    void shouldRejectMirrorWhenEndDateIsBeforeStartDate() {
        UUID employeeId = UUID.randomUUID();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.generateMirror(employeeId, LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1))
        );

        assertEquals(END_DATE_BEFORE_START_DATE, exception.getMessage());
        verifyNoInteractions(domainAuthorizationService, companyProvider, recordRepository);
    }

    @Test
    @DisplayName("espelho: bloqueia período acima do limite de segurança")
    void shouldRejectMirrorWhenPeriodIsTooLarge() {
        UUID employeeId = UUID.randomUUID();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.generateMirror(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2))
        );

        assertEquals(
                String.format(EXPORT_PERIOD_TOO_LARGE, LegalExportRangeGuard.MAX_EXPORT_RANGE_DAYS),
                exception.getMessage()
        );
        verifyNoInteractions(domainAuthorizationService, companyProvider, recordRepository);
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Nome",
                "12345678901",
                "12345678901",
                "Dev",
                "dev@kts.com",
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
    }

    private Company buildCompany(UUID companyId) {
        return new Company(
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
    }

    private TimeRecord record(
            Long id,
            UUID employeeId,
            StatusRecord status,
            LocalDateTime startWork,
            LocalDateTime endWork,
            LocalDateTime originalStartWork,
            LocalDateTime originalEndWork
    ) {
        return new TimeRecord(
                id,
                startWork,
                endWork,
                status,
                false,
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
