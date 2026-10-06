package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timesheetsignature.AdminTimesheetSignaturePageResponse;
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
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.BiometricProtectionService;
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
import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    /** Valid base64 string representing a minimal fake face image for tests. */
    private static final String VALID_FACE_IMAGE_BASE64 = Base64.getEncoder().encodeToString("fake-face-image".getBytes(StandardCharsets.UTF_8));

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
    @Mock EvidenceWatermarkService evidenceWatermarkService;
    @Mock BiometricProtectionService biometricProtectionService;
    @Mock FaceRecognitionProvider faceRecognitionProvider;
    @Mock com.kts.kronos.application.security.PrivacyLogReferenceService privacyLogReferenceService;

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
        lenient().when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        lenient().when(pointMirrorPdfUseCase.generateMirror(eq(employeeId), eq(periodStart), eq(periodEnd)))
                .thenReturn(mirrorPdf);
        // Watermark e sign retornam o mesmo array (pass-through nos testes que precisam mock simples).
        lenient().when(evidenceWatermarkService.applyEvidenceWatermark(any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(digitalSignatureService.signPdf(any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(documentUseCase.uploadGeneratedDocument(eq(DocumentType.POINT_MIRROR_SIGNATURE), eq(employeeId), eq(null), any(), any()))
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
        UUID expectedDocumentId = UUID.randomUUID();
        byte[] companySignedPdf = "company-signed-pdf-bytes".getBytes(StandardCharsets.UTF_8);
        when(digitalSignatureService.signPdf(any(), any(), any())).thenReturn(companySignedPdf);
        when(documentUseCase.uploadGeneratedDocument(eq(DocumentType.POINT_MIRROR_SIGNATURE), eq(employeeId), eq(null), eq(companySignedPdf), any()))
                .thenReturn(expectedDocumentId);
        ArgumentCaptor<TimesheetSignature> captor = ArgumentCaptor.forClass(TimesheetSignature.class);
        when(signatureProvider.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        SignPreviousMonthTimesheetResponse response = service.signMonth(
                signRequest(VALID_FACE_IMAGE_BASE64), "10.0.0.1", "JUnit"
        );

        assertThat(response.signatureType()).isEqualTo("INTERNAL_ADVANCED");
        assertThat(response.signatureMethod()).isEqualTo("FACIAL_RECOGNITION");
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
        verify(documentUseCase).uploadGeneratedDocument(eq(DocumentType.POINT_MIRROR_SIGNATURE), eq(employeeId), eq(null), eq(companySignedPdf), any());
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
    @DisplayName("sign: 403 quando imagem biométrica é inválida (base64 corrompido)")
    void signRejectsInvalidPassword() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(List.of(closedRecord(employeeId, periodStart)));

        // "errada" não é base64 válido — o serviço lança ForbiddenException("Imagem biométrica inválida.")
        assertThatThrownBy(() -> service.signMonth(signRequest("errada%%%"), "ip", "ua"))
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

        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                previous.getYear(), previous.getMonthValue(),
                true,
                TimesheetSignatureService.DECLARATION_VERSION_V1,
                declarationHash,
                "0000000000000000000000000000000000000000000000000000000000000000", // hash que não bate
                VALID_FACE_IMAGE_BASE64
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
        when(pointMirrorPdfUseCase.generateMirror(eq(employeeId), eq(olderStart), eq(olderEnd)))
                .thenReturn("older-pdf".getBytes(StandardCharsets.UTF_8));
        when(evidenceWatermarkService.applyEvidenceWatermark(any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(digitalSignatureService.signPdf(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentUseCase.uploadGeneratedDocument(eq(DocumentType.POINT_MIRROR_SIGNATURE), eq(employeeId), eq(null), any(), any()))
                .thenReturn(UUID.randomUUID());
        when(signatureProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                older.getYear(), older.getMonthValue(),
                true,
                TimesheetSignatureService.DECLARATION_VERSION_V1,
                olderDeclarationHash,
                olderRecordsHash,
                VALID_FACE_IMAGE_BASE64
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
                null, null, null, null,
                "POINT_MIRROR", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
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

    private static TimeRecord timeOffRecord(UUID empId, LocalDate date) {
        return new TimeRecord(
                (long) (date.getDayOfYear() + 2000), date.atStartOfDay(), date.atTime(23, 59),
                StatusRecord.TIME_OFF_REQUEST, false, true, empId,
                null, null, null, null, null, null, null, null
        );
    }

    private static TimeRecord workTimeRecord(UUID empId, LocalDate date) {
        return new TimeRecord(
                (long) (date.getDayOfYear() + 3000), date.atStartOfDay(), date.atTime(23, 59),
                StatusRecord.WORK_TIME_REQUEST, false, true, empId,
                null, null, null, null, null, null, null, null
        );
    }

    private static TimeRecord openCheckoutRecord(UUID empId, LocalDate date) {
        // startWork present, endWork null, status PENDING → "Registro aberto sem checkout"
        return new TimeRecord(
                (long) (date.getDayOfYear() + 4000), date.atTime(9, 0), null,
                StatusRecord.PENDING, false, true, empId,
                null, null, null, null, null, null, null, null
        );
    }

    // ==================== PREVIEW MONTH MIRROR ====================

    @Test
    @DisplayName("previewMonthMirror: colaborador obtém PDF do espelho do mês anterior")
    void previewMonthMirrorSuccess() {
        byte[] result = service.previewMonthMirror(null, null, "127.0.0.1", "JUnit");

        assertThat(result).isEqualTo(mirrorPdf);
    }

    // ==================== DOWNLOAD SIGNATURE DOCUMENT ====================

    @Test
    @DisplayName("downloadSignatureDocument: colaborador baixa seu próprio espelho (fallback sem doc)")
    void downloadSelfFallback() {
        TimesheetSignature sig = activeSignature();
        // pointMirrorDocumentId is already null in activeSignature()
        when(signatureProvider.findById(sig.signatureId())).thenReturn(Optional.of(sig));
        when(pointMirrorPdfUseCase.generateMirror(eq(employeeId), eq(sig.periodStart()), eq(sig.periodEnd())))
                .thenReturn(mirrorPdf);

        var result = service.downloadSignatureDocument(sig.signatureId(), "ip", "ua");

        assertThat(result.data()).isEqualTo(mirrorPdf);
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("downloadSignatureDocument: divergência de hash resulta em nome de arquivo com _divergente")
    void downloadDivergedHash() {
        TimesheetSignature sig = activeSignature();
        byte[] differentPdf = "different-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(signatureProvider.findById(sig.signatureId())).thenReturn(Optional.of(sig));
        when(pointMirrorPdfUseCase.generateMirror(eq(employeeId), eq(sig.periodStart()), eq(sig.periodEnd())))
                .thenReturn(differentPdf); // hash diverges

        var result = service.downloadSignatureDocument(sig.signatureId(), "ip", "ua");

        assertThat(result.fileName()).contains("divergente");
    }

    @Test
    @DisplayName("downloadSignatureDocument: MANAGER do mesmo tenant pode baixar espelho de outro colaborador")
    void downloadByAdminSameTenant() {
        UUID managerEmpId = UUID.randomUUID();
        Employee managerEmp = baseEmployee(managerEmpId, companyId, "Gestor");
        TimesheetSignature sig = activeSignature(); // owned by employeeId

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmpId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmpId)).thenReturn(Optional.of(managerEmp));
        when(signatureProvider.findById(sig.signatureId())).thenReturn(Optional.of(sig));
        when(pointMirrorPdfUseCase.generateMirror(eq(employeeId), eq(sig.periodStart()), eq(sig.periodEnd())))
                .thenReturn(mirrorPdf);

        var result = service.downloadSignatureDocument(sig.signatureId(), "ip", "ua");

        assertThat(result.data()).isEqualTo(mirrorPdf);
    }

    @Test
    @DisplayName("downloadSignatureDocument: colaborador de outro tenant recebe ForbiddenException")
    void downloadCrossTenantForbidden() {
        UUID outsiderEmpId = UUID.randomUUID();
        UUID otherCompany = UUID.randomUUID();
        Employee outsider = baseEmployee(outsiderEmpId, otherCompany, "Outsider");
        TimesheetSignature sig = activeSignature(); // owned by employeeId in companyId

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(outsiderEmpId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(employeeProvider.findById(outsiderEmpId)).thenReturn(Optional.of(outsider));
        when(signatureProvider.findById(sig.signatureId())).thenReturn(Optional.of(sig));

        assertThatThrownBy(() -> service.downloadSignatureDocument(sig.signatureId(), "ip", "ua"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("downloadSignatureDocument: com pointMirrorDocumentId retorna documento persistido")
    void downloadWithPersistedDocument() throws java.io.IOException {
        UUID docId = UUID.randomUUID();
        TimesheetSignature sig = new TimesheetSignature(
                UUID.randomUUID(), employeeId, companyId, userId,
                previous.getYear(), previous.getMonthValue(), periodStart, periodEnd,
                java.time.Instant.now(), TimesheetSignatureService.TIMESHEET_ZONE.getId(),
                TimesheetSignatureType.INTERNAL_ADVANCED, TimesheetSignatureMethod.PASSWORD_REAUTH,
                TimesheetSignatureStatus.ACTIVE,
                docId, // pointMirrorDocumentId != null
                mirrorHash, "recordsHash",
                TimesheetSignatureService.DECLARATION_VERSION_V1, declarationHash, declarationText,
                "ip", "ua", "{}",
                java.time.Instant.now(), null, null, null, null,
                "POINT_MIRROR", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );
        DocumentWithData doc = new DocumentWithData(docId, employeeId, DocumentType.POINT_MIRROR_SIGNATURE,
                "espelho.pdf", "application/pdf", mirrorPdf, java.time.LocalDateTime.now());

        when(signatureProvider.findById(sig.signatureId())).thenReturn(Optional.of(sig));
        when(documentUseCase.downloadDocument(eq(employeeId), eq(docId))).thenReturn(doc);

        var result = service.downloadSignatureDocument(sig.signatureId(), "ip", "ua");

        assertThat(result.data()).isEqualTo(mirrorPdf);
        assertThat(result.fileName()).isEqualTo("espelho.pdf");
    }

    @Test
    @DisplayName("downloadSignatureDocument: IOException no download do documento faz fallback para regeneracao")
    void downloadFallbackOnIOException() throws java.io.IOException {
        UUID docId = UUID.randomUUID();
        TimesheetSignature sig = new TimesheetSignature(
                UUID.randomUUID(), employeeId, companyId, userId,
                previous.getYear(), previous.getMonthValue(), periodStart, periodEnd,
                java.time.Instant.now(), TimesheetSignatureService.TIMESHEET_ZONE.getId(),
                TimesheetSignatureType.INTERNAL_ADVANCED, TimesheetSignatureMethod.PASSWORD_REAUTH,
                TimesheetSignatureStatus.ACTIVE,
                docId,
                mirrorHash, "recordsHash",
                TimesheetSignatureService.DECLARATION_VERSION_V1, declarationHash, declarationText,
                "ip", "ua", "{}",
                java.time.Instant.now(), null, null, null, null,
                "POINT_MIRROR", "1.0", "evhash", UUID.randomUUID(), "SUCCESS"
        );

        when(signatureProvider.findById(sig.signatureId())).thenReturn(Optional.of(sig));
        when(documentUseCase.downloadDocument(eq(employeeId), eq(docId)))
                .thenThrow(new java.io.IOException("S3 error"));
        when(pointMirrorPdfUseCase.generateMirror(eq(employeeId), eq(periodStart), eq(periodEnd)))
                .thenReturn(mirrorPdf);

        var result = service.downloadSignatureDocument(sig.signatureId(), "ip", "ua");

        // Fallback: regenerated
        assertThat(result.data()).isEqualTo(mirrorPdf);
    }

    // ==================== FIND ADMIN ====================

    @Test
    @DisplayName("findAdmin: nao-MANAGER recebe ForbiddenException")
    void findAdminBlocksNonManager() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        assertThatThrownBy(() -> service.findAdmin(null, null, null, null, 0, 10))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("findAdmin: MANAGER lista assinaturas sem filtro de nome")
    void findAdminSuccess() {
        TimesheetSignature sig = activeSignature();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(signatureProvider.findAdminFiltered(any(), eq(companyId), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sig), PageRequest.of(0, 10), 1));

        AdminTimesheetSignaturePageResponse response = service.findAdmin(null, null, null, null, 0, 10);

        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("findAdmin: filtro de nome sem resultados retorna pagina vazia")
    void findAdminNameFilterNoMatch() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));

        AdminTimesheetSignaturePageResponse response =
                service.findAdmin(null, null, null, "NomeQuENaoExiste", 0, 10);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("findAdmin: filtro de nome com resultado retorna apenas os colaboradores filtrados")
    void findAdminNameFilterWithMatch() {
        TimesheetSignature sig = activeSignature();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        // "ana" matches "Ana Lima"
        when(signatureProvider.findAdminFiltered(any(), eq(companyId), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sig), PageRequest.of(0, 10), 1));

        AdminTimesheetSignaturePageResponse response =
                service.findAdmin(null, null, null, "ana", 0, 10);

        assertThat(response.items()).hasSize(1);
    }

    // ==================== RESOLVE TARGET MONTH ====================

    @Test
    @DisplayName("resolveTargetMonth: apenas ano informado lanca BadRequestException")
    void resolveTargetMonthOnlyYear() {
        assertThatThrownBy(() -> service.previewMonthMirror(2026, null, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ano E m");
    }

    @Test
    @DisplayName("resolveTargetMonth: apenas mes informado lanca BadRequestException")
    void resolveTargetMonthOnlyMonth() {
        assertThatThrownBy(() -> service.previewMonthMirror(null, 6, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ano E m");
    }

    @Test
    @DisplayName("resolveTargetMonth: mes invalido (13) lanca BadRequestException")
    void resolveTargetMonthInvalidDate() {
        assertThatThrownBy(() -> service.previewMonthMirror(2025, 13, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nv");
    }

    // ==================== BLOCKER LABEL ====================

    @Test
    @DisplayName("collectBlockers: TIME_OFF_REQUEST gera label de abono")
    void collectBlockersTimeOffRequest() {
        TimeRecord rec = timeOffRecord(employeeId, periodStart);
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(anyList(), any(), any()))
                .thenReturn(List.of(rec));

        PreviousMonthSignatureStatusResponse status = service.getMonthStatus(null, null);

        assertThat(status.status()).isEqualTo("BLOCKED");
        assertThat(status.blockers().get(0)).contains("Abono");
    }

    @Test
    @DisplayName("collectBlockers: REQUEST_VACATION gera label de ferias")
    void collectBlockersVacationRequest() {
        TimeRecord rec = vacationRequestRecord(employeeId, periodStart);
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(anyList(), any(), any()))
                .thenReturn(List.of(rec));

        PreviousMonthSignatureStatusResponse status = service.getMonthStatus(null, null);

        assertThat(status.blockers().get(0)).contains("rias");
    }

    @Test
    @DisplayName("collectBlockers: WORK_TIME_REQUEST gera label de hora trabalhada")
    void collectBlockersWorkTimeRequest() {
        TimeRecord rec = workTimeRecord(employeeId, periodStart);
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(anyList(), any(), any()))
                .thenReturn(List.of(rec));

        PreviousMonthSignatureStatusResponse status = service.getMonthStatus(null, null);

        assertThat(status.blockers().get(0)).contains("hora trabalhada");
    }

    @Test
    @DisplayName("collectBlockers: registro aberto sem checkout (PENDING, endWork null) gera label de checkout")
    void collectBlockersOpenCheckout() {
        TimeRecord rec = openCheckoutRecord(employeeId, periodStart);
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(anyList(), any(), any()))
                .thenReturn(List.of(rec));

        PreviousMonthSignatureStatusResponse status = service.getMonthStatus(null, null);

        assertThat(status.blockers().get(0)).contains("checkout");
    }

    // ==================== signMonth — paths adicionais ====================

    @Test
    @DisplayName("signMonth: face nao encontrada (null) → branch face_not_found")
    void signMonth_faceNotFound() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(closedRecord(employeeId, periodStart)));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(null);

        assertThatThrownBy(() -> service.signMonth(signRequest(VALID_FACE_IMAGE_BASE64), "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("facial");
    }

    @Test
    @DisplayName("signMonth: versao da declaracao errada lanca BadRequestException")
    void signMonth_declarationVersionMismatch() {
        List<TimeRecord> records = List.of(closedRecord(employeeId, periodStart));
        String recordsHash = TimesheetSignatureService.canonicalRecordsHash(records);
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(records);

        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                previous.getYear(), previous.getMonthValue(),
                true, "2.0", declarationHash, recordsHash, VALID_FACE_IMAGE_BASE64
        );
        assertThatThrownBy(() -> service.signMonth(req, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desatualizada");
    }

    @Test
    @DisplayName("signMonth: DataIntegrityViolationException na persistencia → ConflictException")
    void signMonth_dataIntegrityViolation() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(closedRecord(employeeId, periodStart)));
        when(auditService.registerSecurityReturningId(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());
        when(signatureProvider.save(any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.signMonth(signRequest(VALID_FACE_IMAGE_BASE64), "ip", "ua"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("assinatura ativa");
    }

    @Test
    @DisplayName("signMonth: RuntimeException no PAdES e relancada")
    void signMonth_padesRuntimeException() {
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(closedRecord(employeeId, periodStart)));
        when(auditService.registerSecurityReturningId(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());
        when(digitalSignatureService.signPdf(any(), any(), any()))
                .thenThrow(new RuntimeException("HSM offline"));

        assertThatThrownBy(() -> service.signMonth(signRequest(VALID_FACE_IMAGE_BASE64), "ip", "ua"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HSM");
    }

    @Test
    @DisplayName("signMonth: ipAddress null cobre appendJson null-branch; userAgent especial cobre jsonEscape")
    void signMonth_nullIpAndSpecialCharsUserAgent() {
        List<TimeRecord> records = List.of(closedRecord(employeeId, periodStart));
        String recordsHash = TimesheetSignatureService.canonicalRecordsHash(records);
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(records);
        when(auditService.registerSecurityReturningId(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());
        when(signatureProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                previous.getYear(), previous.getMonthValue(),
                true, TimesheetSignatureService.DECLARATION_VERSION_V1,
                declarationHash, recordsHash, VALID_FACE_IMAGE_BASE64
        );
        // cobre: aspas, barra, \b, \f, \n, \r, \t, char de controle (<0x20)
        String specialAgent = "\"\\\b\f\n\r\t\u0001normal";
        SignPreviousMonthTimesheetResponse response = service.signMonth(req, null, specialAgent);
        assertThat(response).isNotNull();
    }

    // ==================== collectBlockers / blockerLabel — paths adicionais ====================

    @Test
    @DisplayName("collectBlockers: record com statusRecord null nao gera bloqueio")
    void collectBlockers_nullStatusRecord() {
        TimeRecord nullStatusRec = new TimeRecord(
                999L, periodStart.atTime(9, 0), periodStart.atTime(18, 0),
                null, false, true, employeeId,
                null, null, null, null, null, null, null, null
        );
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(nullStatusRec));

        PreviousMonthSignatureStatusResponse response = service.getMonthStatus(null, null);

        assertThat(response.eligible()).isTrue();
        assertThat(response.blockers()).isEmpty();
    }

    @Test
    @DisplayName("blockerLabel: startWork null e endWork presente usa data do endWork")
    void blockerLabel_startWorkNullEndWorkPresent() {
        TimeRecord rec = new TimeRecord(
                777L, null, periodStart.atTime(18, 0),
                StatusRecord.PENDING_APPROVAL, true, true, employeeId,
                null, null, null, null, null, null, null, null
        );
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(rec));

        PreviousMonthSignatureStatusResponse response = service.getMonthStatus(null, null);

        assertThat(response.status()).isEqualTo("BLOCKED");
        assertThat(response.blockers().get(0)).contains("Ajuste");
    }

    @Test
    @DisplayName("blockerLabel: startWork null e endWork null usa LocalDate.now")
    void blockerLabel_startWorkNullEndWorkNull() {
        TimeRecord rec = new TimeRecord(
                888L, null, null,
                StatusRecord.PENDING_APPROVAL, true, true, employeeId,
                null, null, null, null, null, null, null, null
        );
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(rec));

        PreviousMonthSignatureStatusResponse response = service.getMonthStatus(null, null);

        assertThat(response.status()).isEqualTo("BLOCKED");
        assertThat(response.blockers().get(0)).contains("Ajuste");
    }

    // ==================== findAdmin — paths adicionais ====================

    @Test
    @DisplayName("findAdmin: employeeName em branco e tratado como sem filtro")
    void findAdmin_blankEmployeeName() {
        TimesheetSignature sig = activeSignature();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(signatureProvider.findAdminFiltered(any(), eq(companyId), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sig), PageRequest.of(0, 10), 1));

        AdminTimesheetSignaturePageResponse response = service.findAdmin(null, null, null, "   ", 0, 10);

        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("findAdmin: colaborador com fullName null nao passa pelo filtro de nome")
    void findAdmin_employeeNullFullName() {
        Employee nullName = new Employee(
                UUID.randomUUID(), null, "12345678901", "12345678901", "Dev", "x@e.com", 1000d,
                "11999999999", true, null, companyId, null, false, null,
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(18, 0),
                java.time.LocalTime.of(12, 0), java.time.LocalTime.of(13, 0),
                null, null, null, null, null
        );
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(nullName));

        AdminTimesheetSignaturePageResponse response = service.findAdmin(null, null, null, "ana", 0, 10);

        assertThat(response.items()).isEmpty();
    }

    // ==================== downloadSignatureDocument — paths adicionais ====================

    @Test
    @DisplayName("downloadSignatureDocument: assinatura nao encontrada lanca ResourceNotFoundException")
    void downloadSignatureDocument_notFound() {
        UUID sigId = UUID.randomUUID();
        when(signatureProvider.findById(sigId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadSignatureDocument(sigId, "ip", "ua"))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("downloadSignatureDocument: MANAGER de outro tenant e bloqueado (cross-tenant)")
    void downloadSignatureDocument_crossTenantAdmin() {
        UUID otherCompany = UUID.randomUUID();
        UUID mgId = UUID.randomUUID();
        Employee mgOther = baseEmployee(mgId, otherCompany, "Gestor Externo");
        TimesheetSignature sig = activeSignature();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(mgId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(mgId)).thenReturn(Optional.of(mgOther));
        when(signatureProvider.findById(sig.signatureId())).thenReturn(Optional.of(sig));

        assertThatThrownBy(() -> service.downloadSignatureDocument(sig.signatureId(), "ip", "ua"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("downloadSignatureDocument: CTO do mesmo tenant pode baixar espelho de outro colaborador")
    void downloadSignatureDocument_ctoBySameTenant() {
        UUID ctEmpId = UUID.randomUUID();
        Employee ctoEmp = baseEmployee(ctEmpId, companyId, "CTO Admin");
        TimesheetSignature sig = activeSignature();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(ctEmpId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findById(ctEmpId)).thenReturn(Optional.of(ctoEmp));
        when(signatureProvider.findById(sig.signatureId())).thenReturn(Optional.of(sig));
        when(pointMirrorPdfUseCase.generateMirror(eq(employeeId), any(), any()))
                .thenReturn(mirrorPdf);

        var result = service.downloadSignatureDocument(sig.signatureId(), "ip", "ua");

        assertThat(result.data()).isEqualTo(mirrorPdf);
    }

    // ==================== getAuthenticatedEmployee — orElseThrow ====================

    @Test
    @DisplayName("getMonthStatus: colaborador autenticado nao encontrado lanca ResourceNotFoundException")
    void getMonthStatus_employeeNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(unknownId);
        when(employeeProvider.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMonthStatus(null, null))
                .isInstanceOf(com.kts.kronos.application.exceptions.ResourceNotFoundException.class)
                .hasMessageContaining("autenticado");
    }

    // ==================== signMonth — additional branch coverage ====================

    @Test
    @DisplayName("signMonth: face encontrada mas errada cobre branch recognizedEmployeeId != null")
    void signMonth_faceFoundButWrongPerson() {
        UUID differentId = UUID.randomUUID();
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(closedRecord(employeeId, periodStart)));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(differentId);
        when(privacyLogReferenceService.employeeRef(differentId)).thenReturn("ref-" + differentId);

        assertThatThrownBy(() -> service.signMonth(signRequest(VALID_FACE_IMAGE_BASE64), "ip", "ua"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("facial");
    }

    @Test
    @DisplayName("signMonth: hash da declaracao incorreto lanca BadRequestException (hash branch do OR)")
    void signMonth_declarationHashMismatch() {
        List<TimeRecord> records = List.of(closedRecord(employeeId, periodStart));
        String recordsHash = TimesheetSignatureService.canonicalRecordsHash(records);
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(records);

        SignPreviousMonthTimesheetRequest req = new SignPreviousMonthTimesheetRequest(
                previous.getYear(), previous.getMonthValue(),
                true, "1.0", "hash-errado-deliberado", recordsHash, VALID_FACE_IMAGE_BASE64
        );
        assertThatThrownBy(() -> service.signMonth(req, "ip", "ua"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desatualizada");
    }

    // ==================== findAdmin (TimesheetSignature) — CTO branch ====================

    @Test
    @DisplayName("findAdmin: CTO pode listar assinaturas (CTO branch)")
    void findAdmin_ctoCanList() {
        TimesheetSignature sig = activeSignature();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(signatureProvider.findAdminFiltered(any(), eq(companyId), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sig), PageRequest.of(0, 10), 1));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));

        AdminTimesheetSignaturePageResponse response = service.findAdmin(null, null, null, null, 0, 10);

        assertThat(response.items()).hasSize(1);
    }

    // ==================== collectBlockers — else if branch paths ====================

    @Test
    @DisplayName("collectBlockers: registro CREATED com startWork nulo nao entra no else-if (A=false branch)")
    void collectBlockers_nonBlockingStartWorkNull() {
        TimeRecord rec = new TimeRecord(
                5001L, null, periodStart.atTime(18, 0),
                StatusRecord.CREATED, false, true, employeeId,
                null, null, null, null, null, null, null, null
        );
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(rec));

        PreviousMonthSignatureStatusResponse response = service.getMonthStatus(null, null);

        assertThat(response.eligible()).isTrue();
        assertThat(response.blockers()).isEmpty();
    }

    @Test
    @DisplayName("collectBlockers: registro CREATED com startWork!=null e endWork==null avalia else-if (A=true B=true C=false branch)")
    void collectBlockers_nonBlockingOpenCheckout() {
        TimeRecord rec = new TimeRecord(
                5002L, periodStart.atTime(9, 0), null,
                StatusRecord.CREATED, false, true, employeeId,
                null, null, null, null, null, null, null, null
        );
        when(signatureProvider.findActiveByEmployeeAndPeriod(employeeId, previous.getYear(), previous.getMonthValue()))
                .thenReturn(Optional.empty());
        when(timeRecordProvider.findByEmployeeIdsAndRange(any(), any(), any()))
                .thenReturn(List.of(rec));

        PreviousMonthSignatureStatusResponse response = service.getMonthStatus(null, null);

        assertThat(response.eligible()).isTrue();
        assertThat(response.blockers()).isEmpty();
    }

}
