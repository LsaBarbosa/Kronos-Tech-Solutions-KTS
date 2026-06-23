package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timesheetsignature.AdminTimesheetSignatureItem;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.AdminTimesheetSignaturePageResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.PreviousMonthSignatureStatusResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetRequest;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.in.usecase.TimesheetSignatureUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.TimesheetSignatureProvider;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.TimesheetSignature;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureMethod;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureType;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimesheetSignatureService implements TimesheetSignatureUseCase {

    public static final ZoneId TIMESHEET_ZONE = ZoneId.of("America/Sao_Paulo");
    public static final String DECLARATION_VERSION_V1 = "1.0";
    public static final String DECLARATION_TEMPLATE_V1 =
            "Declaro que conferi o espelho de ponto referente ao mês de %s, " +
            "reconheço que esta assinatura eletrônica registra minha ciência sobre as marcações " +
            "exibidas neste documento e estou ciente de que posso solicitar correção ou contestação " +
            "pelos canais internos aplicáveis quando identificar divergência.";

    private static final Set<StatusRecord> BLOCKING_STATUSES = Set.of(
            StatusRecord.PENDING,
            StatusRecord.PENDING_APPROVAL,
            StatusRecord.TIME_OFF_REQUEST,
            StatusRecord.REQUEST_VACATION,
            StatusRecord.WORK_TIME_REQUEST
    );

    private final TimesheetSignatureProvider signatureProvider;
    private final TimeRecordProvider timeRecordProvider;
    private final EmployeeProvider employeeProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final PointMirrorPdfUseCase pointMirrorPdfUseCase;
    private final BiometricProtectionService biometricProtectionService;
    private final PrivacyLogReferenceService privacyLogReferenceService;
    private final AuditService auditService;
    private final DocumentUseCase documentUseCase;
    private final DigitalSignatureService digitalSignatureService;
    private final EvidenceWatermarkService evidenceWatermarkService;

    @Override
    @Transactional(readOnly = true)
    public PreviousMonthSignatureStatusResponse getMonthStatus(Integer year, Integer month) {
        Employee employee = getAuthenticatedEmployee();
        YearMonth target = resolveTargetMonth(year, month);
        return buildStatus(employee, target);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] previewMonthMirror(Integer year, Integer month) {
        Employee employee = getAuthenticatedEmployee();
        YearMonth target = resolveTargetMonth(year, month);
        LocalDate start = target.atDay(1);
        LocalDate end = target.atEndOfMonth();
        return pointMirrorPdfUseCase.generateMirror(employee.employeeId(), start, end);
    }

    @Override
    @Transactional
    public SignPreviousMonthTimesheetResponse signMonth(
            SignPreviousMonthTimesheetRequest request,
            String ipAddress,
            String userAgent
    ) {
        if (!request.confirmed()) {
            throw new BadRequestException("Confirmação explícita da declaração é obrigatória.");
        }

        Employee employee = getAuthenticatedEmployee();
        UUID currentUserId = jwtAuthenticatedUser.getuserId();

        YearMonth previous = resolveTargetMonth(request.referenceYear(), request.referenceMonth());
        LocalDate periodStart = previous.atDay(1);
        LocalDate periodEnd = previous.atEndOfMonth();

        // Bloqueio por duplicidade
        signatureProvider.findActiveByEmployeeAndPeriod(employee.employeeId(), previous.getYear(), previous.getMonthValue())
                .ifPresent(existing -> {
                    log.warn("event=timesheet_signature_block reason=already_signed employee_ref={} year={} month={}",
                            employee.employeeId(), previous.getYear(), previous.getMonthValue());
                    throw new ConflictException("Já existe uma assinatura ativa para o mês de referência.");
                });

        // Bloqueio por pendências
        List<TimeRecord> records = fetchPeriodRecords(employee.employeeId(), periodStart, periodEnd);
        List<String> blockers = collectBlockers(records);
        if (!blockers.isEmpty()) {
            auditService.registerSecurity(
                    AuditAction.TIMESHEET_SIGNATURE_BLOCKED,
                    currentUserId,
                    employee.employeeId(),
                    "MEDIUM",
                    "TIMESHEET_SIGNATURE",
                    null,
                    String.format("year=%d,month=%d,blockers=%d", previous.getYear(), previous.getMonthValue(), blockers.size()),
                    ipAddress,
                    userAgent
            );
            throw new ConflictException("Não é possível assinar: há pendências no período. Resolva antes de assinar.");
        }

        // Reconhecimento facial (reautenticação biométrica)
        biometricProtectionService.protectTimesheetSigning(employee.employeeId(), request.faceImageBase64());

        UUID recognizedEmployeeId;
        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.faceImageBase64());
            recognizedEmployeeId = faceRecognitionProvider.searchFaceByImage(new ByteArrayInputStream(imageBytes));
        } catch (IllegalArgumentException ex) {
            auditService.registerSecurity(
                    AuditAction.TIMESHEET_SIGNATURE_FACIAL_AUTH_FAILED,
                    currentUserId,
                    employee.employeeId(),
                    "HIGH",
                    "TIMESHEET_SIGNATURE",
                    null,
                    String.format("year=%d,month=%d,reason=invalid_base64", previous.getYear(), previous.getMonthValue()),
                    ipAddress,
                    userAgent
            );
            throw new ForbiddenException("Imagem biométrica inválida.");
        }

        if (!employee.employeeId().equals(recognizedEmployeeId)) {
            String mismatchDetail = recognizedEmployeeId != null
                    ? String.format("year=%d,month=%d,reason=identity_mismatch,recognized_ref=%s",
                            previous.getYear(), previous.getMonthValue(),
                            privacyLogReferenceService.employeeRef(recognizedEmployeeId))
                    : String.format("year=%d,month=%d,reason=face_not_found",
                            previous.getYear(), previous.getMonthValue());
            auditService.registerSecurity(
                    AuditAction.TIMESHEET_SIGNATURE_FACIAL_AUTH_FAILED,
                    currentUserId,
                    employee.employeeId(),
                    "HIGH",
                    "TIMESHEET_SIGNATURE",
                    null,
                    mismatchDetail,
                    ipAddress,
                    userAgent
            );
            throw new ForbiddenException("Reconhecimento facial não confirmado.");
        }

        // Hash canônico dos registros (determinístico) — usado como anti-tamper real.
        // O hash do PDF NÃO pode ser usado para esse check porque iText injeta metadados
        // variáveis (timestamp, document ID) em cada geração, produzindo bytes diferentes
        // mesmo quando os registros são idênticos.
        String recordsHash = canonicalRecordsHash(records);
        if (!recordsHash.equalsIgnoreCase(request.recordsSnapshotHashSha256())) {
            throw new ConflictException("Os registros do mês de referência mudaram após a visualização. Recarregue e tente novamente.");
        }

        Instant now = Instant.now();
        UUID signatureId = UUID.randomUUID();

        // Validação da declaração — texto+versão+hash devem bater
        String declarationText = buildDeclarationText(previous);
        String declarationHash = sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));
        if (!DECLARATION_VERSION_V1.equals(request.declarationVersion())
                || !declarationHash.equalsIgnoreCase(request.declarationHashSha256())) {
            throw new BadRequestException("A declaração informada está desatualizada. Recarregue a tela.");
        }

        // 1) Hash canônico de evidências (JSON ordenado) — calculado ANTES do PDF
        //    para que possa ser embutido no carimbo visual e no registro de auditoria.
        String canonicalEvidenceJson = buildCanonicalEvidenceJson(
                signatureId, employee, currentUserId, previous, periodStart, periodEnd,
                now, ipAddress, userAgent, declarationHash, recordsHash
        );
        String canonicalEvidenceHash = sha256Hex(canonicalEvidenceJson.getBytes(StandardCharsets.UTF_8));

        // 2) Registra a evidência de auditoria PRIMEIRO para obter o auditLogId
        //    e amarrá-lo à assinatura (rastreabilidade bidirecional).
        UUID auditLogId = auditService.registerSecurityReturningId(
                AuditAction.TIMESHEET_SIGNATURE_SIGNED,
                currentUserId,
                employee.employeeId(),
                "HIGH",
                "TIMESHEET_SIGNATURE",
                signatureId.toString(),
                String.format("year=%d,month=%d,records=%d,canonical_evidence_hash=%s",
                        previous.getYear(), previous.getMonthValue(), records.size(), canonicalEvidenceHash),
                ipAddress,
                userAgent
        );

        // 3) Gera o PDF do espelho LIMPO (sem carimbo).
        byte[] mirrorPdf = pointMirrorPdfUseCase.generateMirror(
                employee.employeeId(), periodStart, periodEnd
        );

        // 4) Aplica marca d'água de evidência (overlay transparente em cada página).
        //    Mesma técnica usada para o contrato de serviço (EvidenceWatermarkService).
        byte[] stampedPdf = evidenceWatermarkService.applyEvidenceWatermark(
                mirrorPdf,
                new EvidenceWatermarkService.EvidenceStamp(
                        employee.fullName(),
                        now,
                        DECLARATION_VERSION_V1,
                        canonicalEvidenceHash
                )
        );

        // 5) Assina com o certificado da empresa (PAdES).
        String padesStatus;
        byte[] signedPdf;
        try {
            signedPdf = digitalSignatureService.signPdf(
                    stampedPdf,
                    "Ciência de espelho de ponto — " + employee.fullName()
                            + " — " + String.format(Locale.ROOT, "%02d/%04d", previous.getMonthValue(), previous.getYear()),
                    "Kronos — assinatura eletrônica interna"
            );
            padesStatus = "SUCCESS";
        } catch (RuntimeException ex) {
            log.warn("event=timesheet_signature_pades_failed year={} month={} exception_type={}",
                    previous.getYear(), previous.getMonthValue(), ex.getClass().getSimpleName());
            throw ex;
        }

        String mirrorHash = sha256Hex(signedPdf);

        // 6) Persiste o PDF assinado como POINT_MIRROR_SIGNATURE (DocumentType
        //    dedicado para espelhos de ponto assinados — separado do POINT_RECORD_RECEIPT
        //    que é usado para comprovantes individuais de check-in/out).
        String fileName = String.format(Locale.ROOT,
                "espelho_ponto_assinado_%04d-%02d_%s.pdf",
                previous.getYear(),
                previous.getMonthValue(),
                employee.employeeId()
        );
        UUID documentId = documentUseCase.uploadGeneratedDocument(
                DocumentType.POINT_MIRROR_SIGNATURE,
                employee.employeeId(),
                null,
                signedPdf,
                fileName
        );

        TimesheetSignature signature = new TimesheetSignature(
                signatureId,
                employee.employeeId(),
                employee.companyId(),
                currentUserId,
                previous.getYear(),
                previous.getMonthValue(),
                periodStart,
                periodEnd,
                now,
                TIMESHEET_ZONE.getId(),
                TimesheetSignatureType.INTERNAL_ADVANCED,
                TimesheetSignatureMethod.FACIAL_RECOGNITION,
                TimesheetSignatureStatus.ACTIVE,
                documentId,
                mirrorHash,
                recordsHash,
                DECLARATION_VERSION_V1,
                declarationHash,
                declarationText,
                ipAddress,
                userAgent,
                canonicalEvidenceJson,
                now,
                null,
                null,
                null,
                null,
                "POINT_MIRROR",
                DECLARATION_VERSION_V1,
                canonicalEvidenceHash,
                auditLogId,
                padesStatus
        );

        TimesheetSignature saved = signatureProvider.save(signature);

        log.info("event=timesheet_signature result=success employee_ref={} year={} month={} records={}",
                employee.employeeId(), previous.getYear(), previous.getMonthValue(), records.size());

        return new SignPreviousMonthTimesheetResponse(
                saved.signatureId(),
                saved.referenceYear(),
                saved.referenceMonth(),
                saved.signedAt(),
                saved.signatureType().name(),
                saved.signatureMethod().name(),
                saved.pointMirrorHashSha256(),
                saved.recordsSnapshotHashSha256(),
                saved.pointMirrorDocumentId(),
                saved.declarationVersion()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SignedDocumentDownload downloadSignatureDocument(UUID signatureId) {
        Employee authenticated = getAuthenticatedEmployee();
        UUID currentUserId = jwtAuthenticatedUser.getuserId();

        TimesheetSignature signature = signatureProvider.findById(signatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada."));

        Role role = jwtAuthenticatedUser.getCurrentRole();
        boolean isSelf = signature.employeeId().equals(authenticated.employeeId());
        boolean adminCrossEmployee = (role == Role.MANAGER || role == Role.CTO);
        boolean sameTenant = signature.companyId().equals(authenticated.companyId());

        if (!isSelf) {
            if (!adminCrossEmployee || !sameTenant) {
                throw new ForbiddenException("Acesso negado ao documento de assinatura.");
            }
        }

        // Se o PDF assinado foi persistido no bucket no momento da assinatura,
        // servimos os bytes exatos do documento original — bit-a-bit idêntico ao
        // que foi assinado pelo certificado da empresa. Sem regeneração, sem
        // ambiguidade de divergência.
        if (signature.pointMirrorDocumentId() != null) {
            try {
                var doc = documentUseCase.downloadDocument(signature.employeeId(), signature.pointMirrorDocumentId());
                auditService.registerSecurity(
                        AuditAction.TIMESHEET_SIGNATURE_VIEWED,
                        currentUserId,
                        signature.employeeId(),
                        "LOW",
                        "TIMESHEET_SIGNATURE",
                        signature.signatureId().toString(),
                        String.format("year=%d,month=%d,source=persisted", signature.referenceYear(), signature.referenceMonth()),
                        (String) null,
                        null
                );
                return new SignedDocumentDownload(doc.data(), doc.fileName(), doc.contentType());
            } catch (java.io.IOException ex) {
                log.warn("event=timesheet_signature_download result=fallback reason=document_fetch_failed signature_id={}",
                        signature.signatureId());
                // segue para fallback de regeneração abaixo
            }
        }

        // Fallback: regenera o PDF a partir dos registros atuais.
        // Marca divergência se o conteúdo atual já não bate com o hash assinado.
        byte[] pdf = pointMirrorPdfUseCase.generateMirror(
                signature.employeeId(),
                signature.periodStart(),
                signature.periodEnd()
        );
        String currentHash = sha256Hex(pdf);
        boolean diverged = !currentHash.equalsIgnoreCase(signature.pointMirrorHashSha256());

        auditService.registerSecurity(
                AuditAction.TIMESHEET_SIGNATURE_VIEWED,
                currentUserId,
                signature.employeeId(),
                diverged ? "HIGH" : "LOW",
                "TIMESHEET_SIGNATURE",
                signature.signatureId().toString(),
                String.format("year=%d,month=%d,source=regenerated,diverged=%s", signature.referenceYear(), signature.referenceMonth(), diverged),
                (String) null,
                null
        );

        String fileName = String.format(Locale.ROOT,
                "espelho_assinado_%04d-%02d%s.pdf",
                signature.referenceYear(),
                signature.referenceMonth(),
                diverged ? "_divergente" : ""
        );
        return new SignedDocumentDownload(pdf, fileName, "application/pdf");
    }

    @Override
    @Transactional(readOnly = true)
    public AdminTimesheetSignaturePageResponse findAdmin(
            Integer year,
            Integer month,
            TimesheetSignatureStatus status,
            String employeeName,
            int page,
            int size
    ) {
        Employee authenticated = getAuthenticatedEmployee();
        Role role = jwtAuthenticatedUser.getCurrentRole();
        if (role != Role.MANAGER && role != Role.CTO) {
            throw new ForbiddenException("Acesso negado.");
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "signedAt")
        );

        Collection<UUID> matchingEmployeeIds = null;
        if (employeeName != null && !employeeName.isBlank()) {
            List<Employee> employees = employeeProvider.findByCompanyId(authenticated.companyId());
            String normalized = employeeName.toLowerCase(Locale.ROOT).trim();
            matchingEmployeeIds = employees.stream()
                    .filter(e -> e.fullName() != null && e.fullName().toLowerCase(Locale.ROOT).contains(normalized))
                    .map(Employee::employeeId)
                    .toList();
            if (matchingEmployeeIds.isEmpty()) {
                return new AdminTimesheetSignaturePageResponse(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
            }
        }

        Page<TimesheetSignature> signatures = signatureProvider.findAdminFiltered(
                pageable, authenticated.companyId(), year, month, status, matchingEmployeeIds
        );

        Map<UUID, String> employeeNames = employeeProvider
                .findByCompanyId(authenticated.companyId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Employee::employeeId, e -> Optional.ofNullable(e.fullName()).orElse("")));

        List<AdminTimesheetSignatureItem> items = signatures.getContent().stream()
                .map(s -> new AdminTimesheetSignatureItem(
                        s.signatureId(),
                        s.employeeId(),
                        employeeNames.getOrDefault(s.employeeId(), ""),
                        s.referenceYear(),
                        s.referenceMonth(),
                        s.signedAt(),
                        s.status().name(),
                        s.signatureType().name(),
                        s.signatureMethod().name(),
                        s.pointMirrorHashSha256()
                ))
                .toList();

        return new AdminTimesheetSignaturePageResponse(
                items,
                signatures.getNumber(),
                signatures.getSize(),
                signatures.getTotalElements(),
                signatures.getTotalPages()
        );
    }

    // ---------------- helpers ----------------

    private Employee getAuthenticatedEmployee() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        return employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador autenticado não encontrado."));
    }

    private YearMonth previousMonth() {
        return YearMonth.from(ZonedDateTime.now(TIMESHEET_ZONE)).minusMonths(1);
    }

    /**
     * Resolve o mês alvo a partir dos parâmetros do cliente.
     * - Se ambos null → mês imediatamente anterior ao vigente.
     * - Se ambos preenchidos → valida que é estritamente anterior ao mês corrente.
     * - Misturar (só um) é tratado como BadRequest.
     */
    private YearMonth resolveTargetMonth(Integer year, Integer month) {
        if (year == null && month == null) {
            return previousMonth();
        }
        if (year == null || month == null) {
            throw new BadRequestException("Informe ano E mês de referência, ou nenhum dos dois.");
        }
        YearMonth target;
        try {
            target = YearMonth.of(year, month);
        } catch (java.time.DateTimeException ex) {
            throw new BadRequestException("Mês de referência inválido.");
        }
        YearMonth current = YearMonth.from(ZonedDateTime.now(TIMESHEET_ZONE));
        if (!target.isBefore(current)) {
            throw new BadRequestException("Só é possível assinar meses anteriores ao vigente.");
        }
        return target;
    }

    private PreviousMonthSignatureStatusResponse buildStatus(Employee employee, YearMonth previous) {
        LocalDate start = previous.atDay(1);
        LocalDate end = previous.atEndOfMonth();
        Optional<TimesheetSignature> existing = signatureProvider.findActiveByEmployeeAndPeriod(
                employee.employeeId(), previous.getYear(), previous.getMonthValue()
        );

        if (existing.isPresent()) {
            TimesheetSignature s = existing.get();
            return new PreviousMonthSignatureStatusResponse(
                    previous.getYear(),
                    previous.getMonthValue(),
                    start,
                    end,
                    "ALREADY_SIGNED",
                    false,
                    true,
                    s.signatureId(),
                    s.signedAt(),
                    s.pointMirrorHashSha256(),
                    s.recordsSnapshotHashSha256(),
                    s.declarationVersion(),
                    s.declarationText(),
                    s.declarationHashSha256(),
                    List.of()
            );
        }

        List<TimeRecord> records = fetchPeriodRecords(employee.employeeId(), start, end);
        List<String> blockers = collectBlockers(records);
        String declarationText = buildDeclarationText(previous);
        String declarationHash = sha256Hex(declarationText.getBytes(StandardCharsets.UTF_8));
        String recordsHash = canonicalRecordsHash(records);

        String status = blockers.isEmpty() ? "ELIGIBLE" : "BLOCKED";
        return new PreviousMonthSignatureStatusResponse(
                previous.getYear(),
                previous.getMonthValue(),
                start,
                end,
                status,
                blockers.isEmpty(),
                false,
                null,
                null,
                null,
                recordsHash,
                DECLARATION_VERSION_V1,
                declarationText,
                declarationHash,
                blockers
        );
    }

    private List<TimeRecord> fetchPeriodRecords(UUID employeeId, LocalDate start, LocalDate end) {
        return timeRecordProvider.findByEmployeeIdsAndRange(
                List.of(employeeId),
                start.atStartOfDay(),
                end.atTime(23, 59, 59)
        );
    }

    private List<String> collectBlockers(List<TimeRecord> records) {
        List<String> blockers = new ArrayList<>();
        for (TimeRecord r : records) {
            if (r.statusRecord() != null && BLOCKING_STATUSES.contains(r.statusRecord())) {
                blockers.add(blockerLabel(r));
            } else if (r.startWork() != null && r.endWork() == null && r.statusRecord() == StatusRecord.PENDING) {
                blockers.add("Registro aberto sem checkout em " + r.startWork().toLocalDate());
            }
        }
        return blockers;
    }

    private String blockerLabel(TimeRecord r) {
        StatusRecord st = r.statusRecord();
        LocalDate date = r.startWork() != null
                ? r.startWork().toLocalDate()
                : (r.endWork() != null ? r.endWork().toLocalDate() : LocalDate.now(TIMESHEET_ZONE));
        return switch (st) {
            case PENDING_APPROVAL -> "Ajuste de ponto pendente em " + date;
            case TIME_OFF_REQUEST -> "Abono/esquecimento pendente em " + date;
            case REQUEST_VACATION -> "Solicitação de férias pendente em " + date;
            case WORK_TIME_REQUEST -> "Solicitação de hora trabalhada pendente em " + date;
            case PENDING -> "Registro aberto sem checkout em " + date;
            default -> "Pendência operacional em " + date;
        };
    }

    private String buildDeclarationText(YearMonth previous) {
        String mmYyyy = String.format(Locale.ROOT, "%02d/%04d", previous.getMonthValue(), previous.getYear());
        return String.format(DECLARATION_TEMPLATE_V1, mmYyyy);
    }

    static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
        }
    }

    static String canonicalRecordsHash(List<TimeRecord> records) {
        // Forma canônica: ordena por timeRecordId; serializa campos imutáveis críticos com separadores fixos.
        List<TimeRecord> ordered = new ArrayList<>(records);
        ordered.sort(Comparator.comparing(
                TimeRecord::timeRecordId,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        StringBuilder sb = new StringBuilder();
        for (TimeRecord r : ordered) {
            sb.append(r.timeRecordId()).append('|')
              .append(r.employeeId()).append('|')
              .append(r.startWork()).append('|')
              .append(r.endWork()).append('|')
              .append(r.statusRecord()).append('|')
              .append(r.active()).append('|')
              .append(r.nsrCheckin()).append('|')
              .append(r.nsrCheckout())
              .append('\n');
        }
        return sha256Hex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JSON canônico de evidências — mesma técnica do ServiceContractService:
     * chaves ordenadas alfabeticamente, sem espaços, escape manual. O SHA-256
     * desse JSON é o {@code canonical_evidence_hash_sha256} da assinatura.
     */
    private String buildCanonicalEvidenceJson(
            UUID signatureId,
            Employee employee,
            UUID currentUserId,
            YearMonth previous,
            LocalDate periodStart,
            LocalDate periodEnd,
            Instant signedAt,
            String ipAddress,
            String userAgent,
            String declarationHash,
            String recordsHash
    ) {
        StringBuilder sb = new StringBuilder(512);
        sb.append('{');
        appendJson(sb, "authMethod", "FACIAL_RECOGNITION"); sb.append(',');
        appendJson(sb, "companyId", employee.companyId().toString()); sb.append(',');
        appendJson(sb, "declarationHashSha256", declarationHash); sb.append(',');
        appendJson(sb, "declarationVersion", DECLARATION_VERSION_V1); sb.append(',');
        appendJson(sb, "documentType", "POINT_MIRROR"); sb.append(',');
        appendJson(sb, "documentVersion", DECLARATION_VERSION_V1); sb.append(',');
        appendJson(sb, "employeeId", employee.employeeId().toString()); sb.append(',');
        appendJson(sb, "faceMatchThreshold", "90.0"); sb.append(',');
        appendJson(sb, "facialVerificationStatus", "APPROVED"); sb.append(',');
        appendJson(sb, "hashAlgorithm", "SHA-256"); sb.append(',');
        appendJson(sb, "ipAddress", ipAddress); sb.append(',');
        appendJson(sb, "livenessEnabled", "false"); sb.append(',');
        appendJson(sb, "livenessStatus", "DISABLED"); sb.append(',');
        appendJson(sb, "periodEnd", periodEnd.toString()); sb.append(',');
        appendJson(sb, "periodStart", periodStart.toString()); sb.append(',');
        appendJson(sb, "recordsSnapshotHashSha256", recordsHash); sb.append(',');
        appendJson(sb, "referenceMonth", String.valueOf(previous.getMonthValue())); sb.append(',');
        appendJson(sb, "referenceYear", String.valueOf(previous.getYear())); sb.append(',');
        appendJson(sb, "signatureId", signatureId.toString()); sb.append(',');
        appendJson(sb, "signatureType", "INTERNAL_ADVANCED"); sb.append(',');
        appendJson(sb, "signedAt", signedAt.toString()); sb.append(',');
        appendJson(sb, "timezone", TIMESHEET_ZONE.getId()); sb.append(',');
        appendJson(sb, "userAgent", userAgent); sb.append(',');
        appendJson(sb, "userId", currentUserId.toString());
        sb.append('}');
        return sb.toString();
    }

    private static void appendJson(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(jsonEscape(value)).append('"');
        }
    }

    private static String jsonEscape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }
}
