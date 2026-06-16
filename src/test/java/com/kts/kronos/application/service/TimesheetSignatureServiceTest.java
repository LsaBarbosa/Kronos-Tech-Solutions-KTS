package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timesheetsignature.PreviousMonthSignatureStatusResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetRequest;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.TimesheetSignatureProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.TimesheetSignature;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureMethod;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureType;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimesheetSignatureServiceTest {

    @Mock TimesheetSignatureProvider signatureProvider;
    @Mock TimeRecordProvider timeRecordProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock UserProvider userProvider;
    @Mock JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock PointMirrorPdfUseCase pointMirrorPdfUseCase;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;
    @Mock DocumentUseCase documentUseCase;
    @Mock DigitalSignatureService digitalSignatureService;

    @InjectMocks
    TimesheetSignatureService service;

    UUID employeeId;
    UUID userId;
    UUID companyId;
    Employee employee;
    User user;
    YearMonth previous;
    LocalDate periodStart;
    LocalDate periodEnd;
    String declarationText;
    String declarationHash;
    byte[] mirrorPdf;
    String mirrorHash;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        employee = baseEmployee(employeeId, companyId, "Ana Lima");
        user = new User(userId, "ana.lima", "$2a$10$hash", Role.PARTNER, true, employeeId);
        previous = YearMonth.from(ZonedDateTime.now(TimesheetSignatureService.TIMESHEET_ZONE)).minusMonths(1);
        periodStart = previous.atDay(1);
        periodEnd = previous.atEndOfMonth();
        String mmYyyy = String.format(Locale.ROOT, "%02d/%04d", previous.getMonthValue(), previous.getYear());
        declarationText = String.format(TimesheetSignatureService.DECLARATION_TEMPLATE_V1, mmYyyy);
        declarationHash = TimesheetSignatureService.sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));
        mirrorPdf = "fake-pdf-bytes".getBytes(StandardCharsets.UTF_8);
        mirrorHash = TimesheetSignatureService.sha256Hex(mirrorPdf);

        lenient().when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        lenient().when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        lenient().when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        lenient().when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(pointMirrorPdfUseCase.generateMirror(eq(employeeId), eq(periodStart), eq(periodEnd)))
                .thenReturn(mirrorPdf);
        // Por padrão, o stamp+sign retorna o mesmo array (pass-through nos testes que precisam mock simples).
        lenient().when(pointMirrorPdfUseCase.generateMirrorWithSignatureStamp(eq(employeeId), eq(periodStart), eq(periodEnd), any()))
                .thenReturn(mirrorPdf);
        lenient().when(digitalSignatureService.signPdf(any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(documentUseCase.uploadGeneratedDocument(eq(DocumentType.POINT_RECORD_RECEIPT), eq(employeeId), eq(null), any(), any()))
                .thenReturn(UUID.randomUUID());
    }

    @Test
    @DisplayName("status: retorna ELIGIBLE quando não há assinatura ativa nem bloqueios")
    void statusReturnsEligible() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(
                eq(List.of(employeeId)),
                eq(periodStart.atStartOfDay()),
                eq(periodEnd.atTime(23, 59, 59))
        )).thenReturn(List.of(closedRecord(employeeId, periodStart)));

        PreviousMonthSignatureStatusResponse response = service.getMonthStatus(null, null);

        assertThat(response.status()).isEqualTo("ELIGIBLE");
        assertThat(response.eligible()).isTrue();
        assertThat(response.alreadySigned()).isFalse();
        assertThat(response.blockers()).isEmpty();
        assertThat(response.declarationVersion()).isEqualTo(TimesheetSignatureService.DECLARATION_VERSION_V1);
        assertThat(response.declarationHashSha256()).isEqualTo(declarationHash);
    }

    @Test
    @DisplayName("status: retorna BLOCKED com lista de pendências quando há registros pendentes")
    void statusReturnsBlocked() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(List.of(
                pendingApprovalRecord(employeeId, periodStart),
                vacationRequestRecord(employeeId, periodStart.plusDays(5))
        ));

        PreviousMonthSignatureStatusResponse response = service.getMonthStatus(null, null);

        assertThat(response.status()).isEqualTo("BLOCKED");
        assertThat(response.eligible()).isFalse();
        assertThat(response.blockers()).hasSize(2);
    }

    @Test
    @DisplayName("status: retorna ALREADY_SIGNED quando há assinatura ativa")
    void statusReturnsAlreadySigned() {
        TimesheetSignature existing = activeSignature();
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.of(existing));

        PreviousMonthSignatureStatusResponse response = service.getMonthStatus(null, null);

        assertThat(response.status()).isEqualTo("ALREADY_SIGNED");
        assertThat(response.alreadySigned()).isTrue();
        assertThat(response.signatureId()).isEqualTo(existing.signatureId());
    }

    @Test
    @DisplayName("sign: sucesso gera PDF carimbado, assina com cert da empresa, persiste no bucket e grava documentId")
    void signSuccess() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(List.of(closedRecord(employeeId, periodStart)));
        when(passwordEncoder.matches("senha-correta", user.password())).thenReturn(true);
        UUID expectedDocumentId = UUID.randomUUID();
        byte[] companySignedPdf = "company-signed-pdf-bytes".getBytes(StandardCharsets.UTF_8);
        when(digitalSignatureService.signPdf(any(), any(), any())).thenReturn(companySignedPdf);
        when(documentUseCase.uploadGeneratedDocument(eq(DocumentType.POINT_RECORD_RECEIPT), eq(employeeId), eq(null), eq(companySignedPdf), any()))
                .thenReturn(expectedDocumentId);
        ArgumentCaptor<TimesheetSignature> captor = ArgumentCaptor.forClass(TimesheetSignature.class);
        when(signatureProvider.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        SignPreviousMonthTimesheetResponse response = service.signMonth(
                signRequest("senha-correta"), "10.0.0.1", "JUnit"
        );

        assertThat(response.signatureType()).isEqualTo("INTERNAL_ADVANCED");
        assertThat(response.signatureMethod()).isEqualTo("PASSWORD_REAUTH");
        assertThat(response.pointMirrorDocumentId()).isEqualTo(expectedDocumentId);
        // O hash persistido é do PDF DEPOIS de assinado pela empresa (não do PDF cru)
        assertThat(response.pointMirrorHashSha256())
                .isEqualTo(TimesheetSignatureService.sha256Hex(companySignedPdf));
        TimesheetSignature persisted = captor.getValue();
        assertThat(persisted.status()).isEqualTo(TimesheetSignatureStatus.ACTIVE);
        assertThat(persisted.declarationHashSha256()).isEqualTo(declarationHash);
        assertThat(persisted.pointMirrorDocumentId()).isEqualTo(expectedDocumentId);
        assertThat(persisted.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(persisted.userAgent()).isEqualTo("JUnit");
        verify(documentUseCase).uploadGeneratedDocument(eq(DocumentType.POINT_RECORD_RECEIPT), eq(employeeId), eq(null), eq(companySignedPdf), any());
    }

    @Test
    @DisplayName("sign: 409 quando já há assinatura ativa para o período")
    void signRejectsDuplicate() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.of(activeSignature()));

        assertThatThrownBy(() -> service.signMonth(signRequest("senha"), "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("assinatura ativa");
        verify(signatureProvider, never()).save(any());
    }

    @Test
    @DisplayName("sign: 400 quando confirmação ausente")
    void signRequiresConfirmation() {
        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                previous.getYear(), previous.getMonthValue(),
                false, TimesheetSignatureService.DECLARATION_VERSION_V1, declarationHash, mirrorHash, "senha"
        );
        assertThatThrownBy(() -> service.signMonth(req, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Confirmação");
    }

    @Test
    @DisplayName("sign: 409 quando há pendências")
    void signRejectsWithPendingRecords() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(pendingApprovalRecord(employeeId, periodStart)));

        assertThatThrownBy(() -> service.signMonth(signRequest("senha"), "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pendências");
        verify(signatureProvider, never()).save(any());
    }

    @Test
    @DisplayName("sign: 403 quando senha não confere")
    void signRejectsInvalidPassword() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(List.of(closedRecord(employeeId, periodStart)));
        when(passwordEncoder.matches("errada", user.password())).thenReturn(false);

        assertThatThrownBy(() -> service.signMonth(signRequest("errada"), "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("inválida");
        verify(signatureProvider, never()).save(any());
    }

    @Test
    @DisplayName("sign: 409 quando hash dos registros informado diverge do hash atual (registros mudaram após visualização)")
    void signRejectsRecordsSnapshotMismatch() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(List.of(closedRecord(employeeId, periodStart)));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                previous.getYear(), previous.getMonthValue(),
                true,
                TimesheetSignatureService.DECLARATION_VERSION_V1,
                declarationHash,
                "0000000000000000000000000000000000000000000000000000000000000000", // hash que não bate
                "senha"
        );

        assertThatThrownBy(() -> service.signMonth(req, "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("registros");
    }

    @Test
    @DisplayName("hash canônico é determinístico para a mesma coleção de registros")
    void canonicalHashDeterministic() {
        TimeRecord r1 = closedRecord(employeeId, periodStart);
        TimeRecord r2 = closedRecord(employeeId, periodStart.plusDays(1));

        String h1 = TimesheetSignatureService.canonicalRecordsHash(List.of(r1, r2));
        String h2 = TimesheetSignatureService.canonicalRecordsHash(List.of(r2, r1)); // ordem invertida

        assertThat(h1).isEqualTo(h2); // ordenação interna estabiliza
        assertThat(h1).hasSize(64); // SHA-256 hex
    }

    @Test
    @DisplayName("hash canônico muda quando NSR ou janela de tempo muda (divergência posterior)")
    void canonicalHashChangesOnEdit() {
        TimeRecord original = closedRecord(employeeId, periodStart);
        TimeRecord edited = new TimeRecord(
                original.timeRecordId(),
                original.startWork(),
                original.endWork().plusHours(1), // janela mudou
                original.statusRecord(),
                true,
                original.active(),
                original.employeeId(),
                original.latitude(),
                original.longitude(),
                original.endLatitude(),
                original.endLongitude(),
                original.nsrCheckin(),
                original.nsrCheckout(),
                original.originalStartWork(),
                original.originalEndWork()
        );

        String h1 = TimesheetSignatureService.canonicalRecordsHash(List.of(original));
        String h2 = TimesheetSignatureService.canonicalRecordsHash(List.of(edited));

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    @DisplayName("sign: 400 quando referenceYear/Month aponta para o mês vigente")
    void signRejectsCurrentMonth() {
        YearMonth current = YearMonth.from(ZonedDateTime.now(TimesheetSignatureService.TIMESHEET_ZONE));
        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                current.getYear(), current.getMonthValue(),
                true,
                TimesheetSignatureService.DECLARATION_VERSION_V1,
                declarationHash,
                "qualquer-hash",
                "senha"
        );
        assertThatThrownBy(() -> service.signMonth(req, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("anteriores");
        verify(signatureProvider, never()).save(any());
    }

    @Test
    @DisplayName("sign: 400 quando referenceYear/Month aponta para um mês futuro")
    void signRejectsFutureMonth() {
        YearMonth future = YearMonth.from(ZonedDateTime.now(TimesheetSignatureService.TIMESHEET_ZONE)).plusMonths(2);
        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                future.getYear(), future.getMonthValue(),
                true,
                TimesheetSignatureService.DECLARATION_VERSION_V1,
                declarationHash,
                "qualquer-hash",
                "senha"
        );
        assertThatThrownBy(() -> service.signMonth(req, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("anteriores");
        verify(signatureProvider, never()).save(any());
    }

    @Test
    @DisplayName("sign: aceita mês -3 quando elegível (não só o mês imediatamente anterior)")
    void signAcceptsOlderMonth() {
        YearMonth older = YearMonth.from(ZonedDateTime.now(TimesheetSignatureService.TIMESHEET_ZONE)).minusMonths(3);
        LocalDate olderStart = older.atDay(1);
        LocalDate olderEnd = older.atEndOfMonth();
        TimeRecord olderRecord = closedRecord(employeeId, olderStart);
        String olderRecordsHash = TimesheetSignatureService.canonicalRecordsHash(List.of(olderRecord));
        String olderDeclarationText = String.format(TimesheetSignatureService.DECLARATION_TEMPLATE_V1,
                String.format(Locale.ROOT, "%02d/%04d", older.getMonthValue(), older.getYear()));
        String olderDeclarationHash = TimesheetSignatureService.sha256Hex(olderDeclarationText.getBytes(StandardCharsets.UTF_8));

        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, older.getYear(), older.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(eq(List.of(employeeId)),
                eq(olderStart.atStartOfDay()), eq(olderEnd.atTime(23, 59, 59))))
                .thenReturn(List.of(olderRecord));
        when(passwordEncoder.matches("senha", user.password())).thenReturn(true);
        when(pointMirrorPdfUseCase.generateMirrorWithSignatureStamp(eq(employeeId), eq(olderStart), eq(olderEnd), any()))
                .thenReturn("older-pdf".getBytes(StandardCharsets.UTF_8));
        when(digitalSignatureService.signPdf(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentUseCase.uploadGeneratedDocument(eq(DocumentType.POINT_RECORD_RECEIPT), eq(employeeId), eq(null), any(), any()))
                .thenReturn(UUID.randomUUID());
        when(signatureProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                older.getYear(), older.getMonthValue(),
                true,
                TimesheetSignatureService.DECLARATION_VERSION_V1,
                olderDeclarationHash,
                olderRecordsHash,
                "senha"
        );

        SignPreviousMonthTimesheetResponse response = service.signMonth(req, "ip", "ua");

        assertThat(response.referenceYear()).isEqualTo(older.getYear());
        assertThat(response.referenceMonth()).isEqualTo(older.getMonthValue());
    }

    // ---------- helpers ----------

    private SignPreviousMonthTimesheetRequest signRequest(String password) {
        // O front-end espelha de volta o recordsSnapshotHashSha256 que recebeu no status.
        // Aqui usamos o hash canônico do conjunto de registros que cada teste configurou.
        List<TimeRecord> records = List.of(closedRecord(employeeId, periodStart));
        String recordsHash = TimesheetSignatureService.canonicalRecordsHash(records);
        return new SignPreviousMonthTimesheetRequest(
                previous.getYear(),
                previous.getMonthValue(),
                true,
                TimesheetSignatureService.DECLARATION_VERSION_V1,
                declarationHash,
                recordsHash,
                password
        );
    }

    private TimesheetSignature activeSignature() {
        return new TimesheetSignature(
                UUID.randomUUID(),
                employeeId,
                companyId,
                userId,
                previous.getYear(),
                previous.getMonthValue(),
                periodStart,
                periodEnd,
                java.time.Instant.now(),
                TimesheetSignatureService.TIMESHEET_ZONE.getId(),
                TimesheetSignatureType.INTERNAL_ADVANCED,
                TimesheetSignatureMethod.PASSWORD_REAUTH,
                TimesheetSignatureStatus.ACTIVE,
                null,
                mirrorHash,
                "recordsHash",
                TimesheetSignatureService.DECLARATION_VERSION_V1,
                declarationHash,
                declarationText,
                "10.0.0.1",
                "JUnit",
                "{}",
                java.time.Instant.now(),
                null, null, null, null
        );
    }

    private static Employee baseEmployee(UUID id, UUID companyId, String name) {
        return new Employee(
                id, name, "12345678901", "12345678901", "Dev", name + "@example.com", 1000d,
                "11999999999", true, null, companyId, null, false, null,
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(18, 0),
                java.time.LocalTime.of(12, 0), java.time.LocalTime.of(13, 0),
                null, null, null, null, null
        );
    }

    private static TimeRecord closedRecord(UUID empId, LocalDate date) {
        LocalDateTime start = date.atTime(9, 0);
        LocalDateTime end = date.atTime(18, 0);
        return new TimeRecord(
                (long) (date.getDayOfYear()), start, end,
                StatusRecord.CREATED, false, true, empId,
                null, null, null, null, 1L, 2L, start, end
        );
    }

    private static TimeRecord pendingApprovalRecord(UUID empId, LocalDate date) {
        LocalDateTime start = date.atTime(9, 0);
        LocalDateTime end = date.atTime(18, 0);
        return new TimeRecord(
                (long) (date.getDayOfYear()), start, end,
                StatusRecord.PENDING_APPROVAL, true, true, empId,
                null, null, null, null, 1L, 2L, start, end
        );
    }

    private static TimeRecord vacationRequestRecord(UUID empId, LocalDate date) {
        return new TimeRecord(
                (long) (date.getDayOfYear() + 1000), date.atStartOfDay(), date.atTime(23, 59),
                StatusRecord.REQUEST_VACATION, false, true, empId,
                null, null, null, null, null, null, null, null
        );
    }
}
