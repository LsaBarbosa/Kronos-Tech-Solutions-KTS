package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationRequestResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.constants.Messages;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.RequestType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.domain.model.enuns.StatusRecord.PENDING_APPROVAL;
import static com.kts.kronos.domain.model.enuns.StatusRecord.UPDATE_REJECTED;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TimeRecordService implements TimeRecordUseCase {

    public static final String COMPANY_NOT_FOUND = "Empresa não encontrada para o funcionário.";
    public static final String ERR_CHECKOUT_STATUS = "Não é possível realizar checkout. Status atual: ";

    // Sucesso (Templates para String.format ou concatenação controlada)
    public static final String MSG_CHECKOUT = "Saída às %s! (NSR: %s)";
    public static final String MSG_CHECKIN = "Entrada às %s! (NSR: %s)";
    public static final String MSG_CHECKIN_GAP = "Entrada após pausa às %s! (NSR: %s)";
    public static final String MSG_CHECKIN_DAYOFF = "Registro de folga convertido para trabalho às %s! (NSR: %s)";

    // Logs
    public static final String LOG_START_REQ = "Iniciando registro de ponto. EmployeeId: {}, Geo: [{}, {}]";
    public static final String LOG_VALIDATION_OK = "Validações de segurança (Biometria/Geo) concluídas para EmployeeId: {}";
    public static final String LOG_CHECKOUT_ATTEMPT = "Tentativa de Checkout detectada. Registro Aberto ID: {}";
    public static final String LOG_CHECKOUT_SUCCESS = "Checkout realizado com sucesso. ID: {}, NSR: {}, Hora: {}";
    public static final String LOG_CHECKOUT_IGNORE = "Registro aberto ID: {} ignorado (Data diferente da atual). Iniciando fluxo de Check-in.";
    public static final String LOG_CHECKIN_CONVERT = "Convertendo registro de FOLGA/FALTA (ID: {}) para TRABALHO. NSR: {}";
    public static final String LOG_BREAK_DETECTED = "Pausa implícita detectada e registrada. Início: {}, Fim: {}";
    public static final String LOG_CHECKIN_SUCCESS = "Check-in realizado com sucesso. Novo ID: {}, NSR: {}, Tipo: {}";
    public static final String INVALID_CHECKOUT = "Tentativa inválida de checkout. Status atual: {}";
    public static final String ERR_TIME_INCONSISTENCY = "O horário final não pode ser anterior ao inicial no mesmo dia.";
    public static final String ERR_MANAGER_REQUIRED = "ID do gestor é obrigatório para esta operação.";
    public static final String ERR_MANAGER_NOT_FOUND = "Gestor não encontrado na base de usuários.";
    public static final String ERR_USER_NOT_MANAGER = "O usuário informado não possui perfil de Gestor.";
    public static final String ERR_MANAGER_DIFF_COMPANY = "O gestor pertence a uma empresa diferente.";
    public static final String ERR_UNAUTHORIZED_ROLE = "Perfil de usuário não autorizado para esta operação.";

    // Logs de Atualização
    public static final String LOG_UPDATE_REQ = "Solicitação de atualização de ponto recebida. RecordID: {}, UserRole: {}";
    public static final String LOG_PARTNER_APPROVAL = "Alteração enviada para aprovação. Employee: {}, Manager: {}";
    public static final String LOG_MANAGER_UPDATE = "Alteração direta realizada por Gestor/CTO. RecordID: {}";
    public static final String LOG_DATE_VALIDATION_ERR = "Tentativa de atualização com datas inconsistentes. RecordID: {}";

    // Erros de Aprovação
    public static final String ERR_APPROVAL_REQ_NOT_FOUND = "Solicitação de aprovação não encontrada para o registro ID: ";

    // Logs de Fluxo de Aprovação
    public static final String LOG_APPROVAL_START = "Iniciando processo de aprovação para o registro ID: {}";
    public static final String LOG_APPROVAL_NOT_FOUND = "Falha na aprovação: Solicitação não encontrada para o registro ID: {}";
    public static final String LOG_ADJUSTING_ADJACENT = "Ajustando registros adjacentes. EmployeeID: {}, RecordID: {}";
    public static final String LOG_APPROVAL_CLEANUP = "Limpeza: Dados de solicitação removidos da tabela de aprovação para o registro ID: {}";
    public static final String LOG_APPROVAL_SUCCESS = "Solicitação APROVADA com sucesso. RecordID: {}, EmployeeID: {}";

    // Logs de Rejeição
    public static final String LOG_REJECT_START = "Iniciando processo de REJEIÇÃO de ajuste. RecordID: {}";
    public static final String LOG_REJECT_SUCCESS = "Solicitação REJEITADA com sucesso. O registro retornou ao estado original. RecordID: {}";
    public static final String LOG_REJECT_VALIDATION = "Validação: Solicitação de aprovação pendente localizada para RecordID: {}";

    // Logs de Exclusão
    public static final String LOG_DELETE_INIT = "Solicitação de EXCLUSÃO recebida. RecordID: {}, EmployeeUUID: {}";
    public static final String LOG_DELETE_VALIDATION = "Validação: Registro pertence ao funcionário e está em status permitível. Status: {}";
    public static final String LOG_DELETE_DEPENDENCIES = "Limpando dependências: Removendo solicitações de aprovação vinculadas ao RecordID: {}";
    public static final String LOG_DELETE_SUCCESS = "Registro excluído permanentemente com sucesso. RecordID: {}, Data Original: {}";

    // Erros de Exclusão
    public static final String ERR_DELETE_CLOSED_RECORD = "Operação negada: Não é permitido excluir registros já fechados ou processados (Status: %s).";
    public static final String DELETE_BLOCKED = "Tentativa de exclusão de registro bloqueado. RecordID: {}, Status: {}";

    // Logs de Alternância de Estado (Toggle)
    public static final String LOG_TOGGLE_INIT = "Iniciando alternância de ativação (Soft Delete/Restore). RecordID: {}, EmployeeUUID: {}";
    public static final String LOG_TOGGLE_SUCCESS = "Status do registro alterado com sucesso. RecordID: {}, Status do Registro Anterior Ativo: {}, Status do Registro Atual Ativo: {}";

    // Erros de Alternância
    public static final String ERR_TOGGLE_CLOSED = "Operação negada: Não é permitido inativar/ativar um registro já processado (Status: %s).";
    public static final String TOGGLE_BLOCKED = "Tentativa de alternância de ativação em registro bloqueado. RecordID: {}, Status: {}";

    // Logs de Atualização de Status
    public static final String LOG_UPDATE_STATUS_INIT = "Iniciando alteração manual de status. RecordID: {}, Novo Status Solicitado: {}";
    public static final String LOG_UPDATE_STATUS_IDEMPOTENT = "O status atual já é {}. Nenhuma alteração realizada para o RecordID: {}";
    public static final String LOG_UPDATE_STATUS_SUCCESS = "Status do registro alterado com sucesso. RecordID: {}, Transição: [{}] -> [{}]";

    // Erros de Validação de Status
    public static final String ERR_STATUS_PENDING = "Operação negada: O registro está bloqueado aguardando aprovação.";
    public static final String ERR_STATUS_UPDATED = "Operação negada: O registro já foi atualizado anteriormente e não aceita nova mutação direta.";
    public static final String ERR_STATUS_CLOSED = "Operação negada: Não é possível alterar o status de um registro já fechado ou processado na folha.";
    public static final String UPDATE_STATUS_BLOCKED = "Tentativa de alterar status de um registro bloqueado (Pendente). RecordID: {}";
    public static final String RECORD_ALREADY_UPDATED_BLOCKED = "Tentativa de alterar status de um registro já atualizado. RecordID: {}";
    public static final String UPDATE_RECORD_CLOSED_BLOCKED = "Tentativa de alterar status de um registro fechado. RecordID: {}";

    // Logs de Relatórios
    public static final String LOG_REPORT_INIT = "Iniciando geração de relatório simples. TargetEmployeeID: {}, Datas Solicitadas: {}";
    public static final String LOG_REPORT_EMPTY = "Nenhum registro encontrado para o TargetEmployeeID: {} nas datas informadas.";
    public static final String LOG_REPORT_SUCCESS = "Relatório gerado com sucesso para TargetEmployeeID: {}. Dias processados: {}";

    // Erros de Relatórios
    public static final String ERR_INVALID_REFERENCE = "O formato da hora de referência é inválido. Esperado: HH:mm";
    public static final String PARSE_ERROR = "Erro ao fazer parse da referência de jornada: {}";

    // Logs do Relatório Detalhado (ListReport)
    public static final String LOG_LIST_REPORT_INIT = "Iniciando geração de relatório detalhado. TargetEmployeeID: {}, Datas Solicitadas: {}";
    public static final String LOG_LIST_REPORT_EMPTY_DATES = "Geração abortada: Nenhuma data fornecida para o TargetEmployeeID: {}";
    public static final String LOG_LIST_REPORT_FETCH_DOCS = "Buscando documentos em lote para {} registros.";
    public static final String LOG_LIST_REPORT_SUCCESS = "Relatório detalhado gerado com sucesso para TargetEmployeeID: {}. Registros processados: {}";

    private final TimeRecordProvider timeRecordProvider;
    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final DocumentService documentService;
    private final DocumentProvider documentProvider;
    private final CompanyUseCase companyUseCase;
    private final UserProvider userProvider;
    private final TimeRecordApprovalProvider approvalProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final ReceiptPdfService receiptPdfService;
    private final AdfUseCase adfUseCase;
    private final NsrProvider nsrProvider;
    private final NtpTimeService ntpTimeService;

    @Override
    public ActionResponse registerTime(GeolocationRequest request) {

        log.info(
                LOG_START_REQ,jwtAuthenticatedUser.getEmployeeId(),
                request.latitude(),
                request.longitude()
        );


        // BLINDAGEM CONTRA FRAUDE DE RELÓGIO (NTP)
        ntpTimeService.validateSystemTime(10);

        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);


        validateFaceRecognition(employeeId, request.faceImageBase64());
        isHomeOffice(request, employee, employeeId);

        log.debug(LOG_VALIDATION_OK, employeeId);

        var openRecordOpt = timeRecordProvider.findOpenByEmployeeId(employee.employeeId());
        var currentTime = LocalDateTime.now(SAO_PAULO);
        var currentTimeParsed = currentTime.format(TIME_FORMATTER);
        var todayDate = currentTime.toLocalDate();

        var company = companyProvider.findById(employee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        // ---------------------------------------------------------------------
        // CENÁRIO A: CHECKOUT (Saída)
        // ---------------------------------------------------------------------
        if (openRecordOpt.isPresent()) {
            var open = openRecordOpt.get();
            var openRecordDate = open.startWork().atZone(SAO_PAULO).toLocalDate();

            if (openRecordDate.isEqual(todayDate)) {

                log.info(LOG_CHECKOUT_ATTEMPT, open.timeRecordId());

                if (open.statusRecord() != PENDING) {
                    log.warn(INVALID_CHECKOUT, open.statusRecord());
                    throw new BadRequestException(ERR_CHECKOUT_STATUS + open.statusRecord() + ")");
                }

                var nsrCheckout = nsrProvider.generateNextNsr(employee.companyId());

                var updated = new TimeRecord(
                        open.timeRecordId(),
                        open.startWork(),
                        currentTime,
                        open.statusRecord().onCheckout(),
                        open.edited(),
                        open.active(),
                        open.employeeId(),
                        open.latitude(),
                        open.longitude(),
                        request.latitude(),
                        request.longitude(),
                        open.nsrCheckin(),
                        nsrCheckout,
                        open.originalStartWork(),
                        currentTime
                );

                timeRecordProvider.save(updated);

                // C. Auditoria Fiscal (AFD) - Grava linha tipo 7
                adfUseCase.logMarking(company, employee, currentTime, nsrCheckout);

                generateAndSaveReceipt(employee, updated.timeRecordId(), currentTime, nsrCheckout, Messages.EXIT);

                log.info(LOG_CHECKOUT_SUCCESS, updated.timeRecordId(), nsrCheckout, currentTimeParsed);

                return new ActionResponse(
                        String.format(MSG_CHECKOUT, currentTimeParsed, nsrCheckout),
                        CHECKOUT
                );

            } else {
                log.info(LOG_CHECKOUT_IGNORE, open.timeRecordId());
            }
        }

        // ---------------------------------------------------------------------
        // CENÁRIO B: CHECKIN (Entrada) - Com lógica de suporte a DIA DE FOLGA
        // ---------------------------------------------------------------------

        var nsrCheckin = nsrProvider.generateNextNsr(employee.companyId());
        var actionType = CHECKIN; // Default

        // Verifica se já existe um registro de FOLGA ou FALTA para hoje <<<
        // Isso permite que o funcionário trabalhe no dia que o sistema achava que era folga.
        // Necessário buscar qualquer registro do dia, independente de estar "open"
        var startOfDay = todayDate.atStartOfDay();
        var endOfDay = todayDate.atTime(23, 59, 59);

        List<TimeRecord> recordsToday = timeRecordProvider.findByRange(
                employee.employeeId(), startOfDay, endOfDay); //

        Optional<TimeRecord> dayOffOrAbsenceRecord = recordsToday.stream()
                .filter(r -> r.statusRecord() == StatusRecord.DAY_OFF || r.statusRecord() == StatusRecord.ABSENCE)
                .findFirst();

        TimeRecord recordToSave;

        if (dayOffOrAbsenceRecord.isPresent()) {
            // CENÁRIO: Transformar FOLGA/FALTA em TRABALHO
            var existing = dayOffOrAbsenceRecord.get();

            log.warn(LOG_CHECKIN_CONVERT, existing.timeRecordId(), nsrCheckin);

            recordToSave = new TimeRecord(
                    existing.timeRecordId(),
                    currentTime,
                    null,
                    PENDING,
                    false,
                    true,
                    employee.employeeId(),
                    request.latitude(),
                    request.longitude(),
                    null, null,
                    nsrCheckin,
                    null,
                    currentTime,
                    null
            );

            actionType = CHECKIN_ON_DAY_OFF;

        } else {
            // CENÁRIO PADRÃO: Criar novo registro
            // Lógica de Pausa Implícita (Gap)
            var latestRecordOpt = timeRecordProvider.
                    findTopByEmployeeIdOrderByStartWorkDesc(employee.employeeId());

            if (latestRecordOpt.isPresent()) {
                var latest = latestRecordOpt.get();
                var latestEndWork = latest.endWork();
                var currentStartDay = currentTime.toLocalDate();
                var latestEndDay = latestEndWork != null ? latestEndWork.atZone(SAO_PAULO).toLocalDate() : null;

                // Se o último registro fechado foi HOJE, cria o registro de intervalo (gap)
                if (latestEndWork != null && currentStartDay.equals(latestEndDay)) {
                    var breakRecord = new TimeRecord(
                            null,
                            latestEndWork,
                            currentTime,
                            StatusRecord.IMPLICIT_BREAK,
                            false,
                            true,
                            employee.employeeId(),
                            null, null, null, null,
                            null, null,
                            latestEndWork, currentTime
                    );
                    timeRecordProvider.save(breakRecord);
                    actionType = CHECKIN_AFTER_BREAK;

                    log.info(LOG_BREAK_DETECTED, latestEndWork, currentTime);
                 }
            }

            recordToSave = new TimeRecord(
                    null,
                    currentTime,
                    null,
                    PENDING,
                    false,
                    true,
                    employee.employeeId(),
                    request.latitude(),
                    request.longitude(),
                    null, null,
                    nsrCheckin,
                    null,
                    currentTime,
                    null
            );
        }

        var savedRecord = timeRecordProvider.save(recordToSave);

        adfUseCase.logMarking(company, employee, currentTime, nsrCheckin);

        generateAndSaveReceipt(employee, savedRecord.timeRecordId(), currentTime, nsrCheckin, "ENTRADA");

        log.info(LOG_CHECKIN_SUCCESS, savedRecord.timeRecordId(), nsrCheckin, actionType);

        String message = switch (actionType) {
            case CHECKIN_AFTER_BREAK -> String.format(MSG_CHECKIN_GAP, currentTimeParsed, nsrCheckin);
            case CHECKIN_ON_DAY_OFF -> String.format(MSG_CHECKIN_DAYOFF, currentTimeParsed, nsrCheckin);
            default -> String.format(MSG_CHECKIN, currentTimeParsed, nsrCheckin);
        };

        return new ActionResponse(message, actionType);
    }

    @Override
    public void updateTimeRecord(Long timeRecordId, UpdateTimeRecordRequest req) {
        var userRole = jwtAuthenticatedUser.getRoleFromToken();
        var employeeId = jwtAuthenticatedUser.getEmployeeId();

        log.info(LOG_UPDATE_REQ, timeRecordId, userRole);
        var employee = getEmployee(employeeId);
        var record = getTimeRecord(timeRecordId);

        isRecordBelongsEmployee(employee.employeeId(), record);

        var parseStartTime = LocalTime.parse(req.startHour(), TIME_FORMATTER);
        var parseEndTime = LocalTime.parse(req.endHour(), TIME_FORMATTER);

        var newStart = LocalDateTime.of(req.startDate(), parseStartTime);
        var newEnd = LocalDateTime.of(req.endDate(), parseEndTime);

        validateTimeConsistency(
                req.startDate(), req.endDate(), parseStartTime, parseEndTime, timeRecordId
        );

        // 2. Roteamento de Fluxo baseado no Perfil
        if ("PARTNER".equals(userRole)) {
            handlePartnerUpdateFlow(req, employee, record, newStart, newEnd);

        } else if ("MANAGER".equals(userRole) || "CTO".equals(userRole)) {
            handleManagerUpdateFlow(employee, record, newStart, newEnd);

        } else {
            log.warn("Tentativa de atualização não autorizada. Role: {}", userRole);
            throw new ForbiddenException(ERR_UNAUTHORIZED_ROLE);
        }
    }

    @Override
    public void approveTimeRecordChange(Long timeRecordId) {
        log.info(LOG_APPROVAL_START, timeRecordId);
        var record = findRecordAndCheckStatus(timeRecordId);

        var approvalData = approvalProvider.findByTimeRecordId(timeRecordId).orElseThrow(() -> {
            log.warn(LOG_APPROVAL_NOT_FOUND, timeRecordId);
            return new ResourceNotFoundException(ERR_APPROVAL_REQ_NOT_FOUND + timeRecordId);
        });

        log.debug(LOG_ADJUSTING_ADJACENT, record.employeeId(), timeRecordId);
        // Executa o ajuste dos registros de Pausa vizinhos ANTES de aplicar o ponto ---
        adjustAdjacentRecordsOnUpdate(
                record.employeeId(), record, approvalData.newStartWork(), approvalData.newEndWork()
        );

        var approvedRecord = record
                .withCheckin(approvalData.newStartWork())
                .withCheckout(approvalData.newEndWork())
                .withStatus(StatusRecord.UPDATED);

        timeRecordProvider.save(approvedRecord);
        approvalProvider.deleteByTimeRecordId(timeRecordId);

        log.debug(LOG_APPROVAL_CLEANUP, timeRecordId);
        log.info(LOG_APPROVAL_SUCCESS, timeRecordId, record.employeeId());
    }

    @Override
    public void rejectTimeRecordChange(Long timeRecordId) {

        log.info(LOG_REJECT_START, timeRecordId);
        var record = findRecordAndCheckStatus(timeRecordId);

        // Isso evita que um admin rejeite um registro cancelado ou aprovado por outro via race condition.
        approvalProvider.findByTimeRecordId(timeRecordId)
                .orElseThrow(() -> {
                    log.warn(LOG_APPROVAL_NOT_FOUND, timeRecordId);
                    return new ResourceNotFoundException(ERR_APPROVAL_REQ_NOT_FOUND + timeRecordId);
                });

        log.debug(LOG_REJECT_VALIDATION, timeRecordId);

        var rejectedRecord = record
                .withStatus(UPDATE_REJECTED)
                .withEdited(false);

        timeRecordProvider.save(rejectedRecord);
        approvalProvider.deleteByTimeRecordId(timeRecordId);
        log.info(LOG_REJECT_SUCCESS, timeRecordId);
    }

    @Override
    public void deleteTimeRecord(UUID employeeId, Long recordId) {
        log.info(LOG_DELETE_INIT, recordId, employeeId);

        var employee = getEmployee(employeeId);
        var record = getTimeRecord(recordId);

        isRecordBelongsEmployee(employee.employeeId(), record);

        // IMPEDIR a exclusão se o registro já estiver consolidado (CLOSED).
        if (StatusRecord.CLOSED.equals(record.statusRecord())) {
            log.warn(DELETE_BLOCKED, recordId, record.statusRecord());
            throw new BadRequestException(String.format(ERR_DELETE_CLOSED_RECORD, record.statusRecord()));
        }

        log.debug(LOG_DELETE_VALIDATION, record.statusRecord());

        var approvalExists = approvalProvider.findByTimeRecordId(recordId);
        if (approvalExists.isPresent()) {
            log.debug(LOG_DELETE_DEPENDENCIES, recordId);
            approvalProvider.deleteByTimeRecordId(recordId);
        }

        var originalDate = record.originalStartWork();
        timeRecordProvider.deleteTimeRecord(record);

        log.info(LOG_DELETE_SUCCESS, recordId, originalDate);
    }

    @Override
    public void toggleActivate(UUID employeeId, Long timeRecordId) {
        log.info(LOG_TOGGLE_INIT, timeRecordId, employeeId);
        var record = getRecord(employeeId, timeRecordId);

        if (StatusRecord.CLOSED.equals(record.statusRecord())) {
            log.warn(TOGGLE_BLOCKED, timeRecordId, record.statusRecord());
            throw new BadRequestException(String.format(ERR_TOGGLE_CLOSED, record.statusRecord()));
        }

        boolean currentActiveState = record.active();
        boolean newActiveState = !currentActiveState;
        var toggle = record.withActive(newActiveState);

        timeRecordProvider.save(toggle);
        log.info(LOG_TOGGLE_SUCCESS, timeRecordId, currentActiveState, newActiveState);
    }

    @Override
    public void updateStatus(UUID employeeId, Long timeRecordId, UpdateTimeRecordStatusRequest req) {
        log.info(LOG_UPDATE_STATUS_INIT, timeRecordId, req.statusRecord());
        var record = getRecord(employeeId, timeRecordId);
        var currentStatus = record.statusRecord();
        var newStatus = req.statusRecord();

        if (validationStatus(timeRecordId, currentStatus, newStatus)) return;

        var updateStatus = record.withStatus(newStatus);
        timeRecordProvider.save(updateStatus);
        log.info(LOG_UPDATE_STATUS_SUCCESS, timeRecordId, currentStatus, newStatus);
    }

    @Override
    public SimpleReportResponse simpleReport(UUID employeeId, SimpleReportRequest req) {
        var targetEmployeeId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        var employeeData = getEmployeeData(targetEmployeeId);
        var referenceDuration = parseReferenceTime(req.reference());

        log.info(LOG_REPORT_INIT, targetEmployeeId, req.dates().length);

        var workStatuses = Set.of(CREATED, UPDATED, PENDING_APPROVAL, PENDING);
        var specialStatuses = Set.of(DAY_OFF, TIME_OFF, ABSENCE, REQUEST_VACATION, VACATION, VACATION_REJECTED);
        var validStatusesForReport = new HashSet<>(workStatuses);
        validStatusesForReport.addAll(specialStatuses);
        validStatusesForReport.add(StatusRecord.IMPLICIT_BREAK);

        Set<LocalDate> requestedDates = Arrays.stream(req.dates()).collect(Collectors.toSet());

        List<TimeRecord> filteredRecords = timeRecordProvider
                .findByEmployeeAndDatesAndStatuses(targetEmployeeId, requestedDates, validStatusesForReport);

        if (filteredRecords.isEmpty()) {
            log.info(LOG_REPORT_EMPTY, targetEmployeeId);
            return new SimpleReportResponse(employeeData.employeeName(), employeeData.companyName(),
                    Collections.emptyList(), "00:00", "00:00", "+00:00");
        }

        Map<LocalDate, List<TimeRecord>> recordsByDay = filteredRecords.stream()
                .collect(Collectors.groupingBy(
                        tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<SimpleReportDay> days = new ArrayList<>();
        var totalWorkedDuration = Duration.ZERO;
        var totalBreakDuration = Duration.ZERO;
        var totalBalance = Duration.ZERO;

        for (var entry : recordsByDay.entrySet()) {
            var startDate = entry.getKey();
            List<TimeRecord> dailyRecords = entry.getValue();

            dailyRecords.sort(Comparator.comparing(
                    TimeRecord::startWork, Comparator.nullsLast(Comparator.naturalOrder())));

            var dailyReport = processDailyRecords(
                    startDate, dailyRecords, referenceDuration, workStatuses, specialStatuses);
            days.add(dailyReport.dayResponse());
            totalWorkedDuration = totalWorkedDuration.plus(dailyReport.worked());
            totalBreakDuration = totalBreakDuration.plus(dailyReport.breakTime());
            totalBalance = totalBalance.plus(dailyReport.balance());
        }

        var finalWorked = formatDuration(totalWorkedDuration, false);
        var finalBreak = formatDuration(totalBreakDuration, false);
        var finalBalance = formatDuration(totalBalance, true);

        log.info(LOG_REPORT_SUCCESS, targetEmployeeId, days.size());

        return new SimpleReportResponse(
                employeeData.employeeName(),
                employeeData.companyName(),
                days,
                finalWorked,
                finalBreak,
                finalBalance
        );
    }

   @Override
    public List<TimeRecordResponse> listReport(UUID employeeId, ListReportRequest req) {
        var targetEmployeeId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        var employeeData = getEmployeeData(targetEmployeeId);
        var reference = getDuration(req.reference());

       if (req.dates() == null || req.dates().length == 0) {
           log.warn(LOG_LIST_REPORT_EMPTY_DATES, targetEmployeeId);
           return Collections.emptyList();
       }

        //  Definição de datas
       final Set<LocalDate> finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
       log.info(LOG_LIST_REPORT_INIT, targetEmployeeId, finalDatesSet.size());

        var allPossibleReportStatuses = EnumSet.of(
                CREATED, PENDING, UPDATED, UPDATE_REJECTED, DAY_OFF, ABSENCE,
                PENDING_APPROVAL, TIME_OFF, TIME_OFF_REQUEST, TIME_OFF_REJECTED,
                IMPLICIT_BREAK, REQUEST_VACATION, VACATION, VACATION_REJECTED,
                WORK_TIME_REQUEST,
                WORK_TIME_REJECTED
        );

       Set<StatusRecord> finalFilterStatuses = (req.statuses() == null || req.statuses().isEmpty())
               ? allPossibleReportStatuses
               : req.statuses().stream()
               .filter(allPossibleReportStatuses::contains)
               .collect(Collectors.toSet());

       List<TimeRecord> recordsInRange = timeRecordProvider.findByEmployeeAndDatesAndStatuses(
               targetEmployeeId, finalDatesSet, finalFilterStatuses
       );

       if (recordsInRange.isEmpty()) {
           return Collections.emptyList();
       }

        // =================================================================================
        // LÓGICA DE CÁLCULO DE SALDO ÚNICO POR DIA
        // =================================================================================

       Map<LocalDate, String> dailyBalanceMap = calculateDailyBalances(recordsInRange, reference);
       Map<Long, String> latestDocumentsMap = fetchLatestDocumentsInBulk(recordsInRange);

       var response = recordsInRange.stream()
               .sorted(Comparator.comparing(TimeRecord::startWork))
               .map(tr -> {
                   var date = tr.startWork().atZone(SAO_PAULO).toLocalDate();
                   var dailyBalance = dailyBalanceMap.getOrDefault(date, "+00:00");
                   var documentPath = latestDocumentsMap.get(tr.timeRecordId());

                   return TimeRecordResponse.fromDomain(tr, reference, employeeData, documentPath, dailyBalance);
               })
               .toList();
       log.info(LOG_LIST_REPORT_SUCCESS, targetEmployeeId, response.size());
       return response;
    }

    @Override
    public TimeRecordApprovalPageResponse listPendingApprovals(int page, int size, String employeeName) {

        // 1. SEGURANÇA: Identifica a empresa do Manager logado
        var managerId = jwtAuthenticatedUser.getEmployeeId();
        var manager = employeeProvider.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var companyId = manager.companyId();

        var pageable = PageRequest.of(page, size);

        // 2. BUSCA SEGURA: Passa o companyId para filtrar no banco
        Page<TimeRecordApprovalRequest> approvalsPage = approvalProvider.findAllByCompanyId(pageable, employeeName, companyId);

        List<TimeRecordApprovalResponse> responses = new ArrayList<>();

        for (TimeRecordApprovalRequest approvalData : approvalsPage.getContent()) {
            // ... (O resto da lógica de montagem do DTO permanece igual) ...
            var timeRecord = timeRecordProvider.findById(approvalData.timeRecordId()).orElse(null);
            var partnerEmployee = employeeProvider.findById(approvalData.requestingEmployeeId()).orElse(null);
            var managerUser = userProvider.findById(approvalData.managerId()).orElse(null);

            var docs = documentProvider.findByTimeRecordId(approvalData.timeRecordId());
            String documentPath = null;
            if (!docs.isEmpty()) {
                // Pega o ID do primeiro documento encontrado
                documentPath = "/documents/" + docs.getFirst().documentId();
            }

            if (partnerEmployee != null && managerUser != null && timeRecord != null) {
                responses.add(new TimeRecordApprovalResponse(
                        approvalData.timeRecordId(),
                        partnerEmployee.fullName(),
                        managerUser.username(),
                        approvalData.newStartWork(),
                        approvalData.newEndWork(),
                        timeRecord.startWork(),
                        timeRecord.endWork(),
                        documentPath
                ));
            }
        }

        return new TimeRecordApprovalPageResponse(
                responses,
                approvalsPage.getTotalPages(),
                approvalsPage.getTotalElements(),
                approvalsPage.getNumber(),
                approvalsPage.isFirst(),
                approvalsPage.isLast()
        );
    }

    @Override
    public List<Long> requestVacation(RequestVacationRequest request) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);
        var managerUser = userProvider.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException(MANAGER_NOT_FOUND));

        if (managerUser.role() != Role.MANAGER) {
            throw new ForbiddenException(ROLE_IS_NOT_MANAGER);
        }

        if (!employeeProvider.findById(managerUser.employeeId()).map(e -> e.companyId().equals(employee.companyId())).orElse(false)) {
            throw new BadRequestException(MANAGER_DIFFERENT_COMPANY);
        }

        var start = request.startDate();
        var end = request.endDate();
        List<Long> createdRecordIds = new ArrayList<>();

        // Validação básica: data de início não pode ser após a data de fim
        if (start.isAfter(end)) {
            throw new BadRequestException(START_DATE_BIGGER_THAN_END_DATE);
        }


        long daysBetween = ChronoUnit.DAYS.between(start, end) + 1;
        for (int i = 0; i < daysBetween; i++) {
            var currentDay = start.plusDays(i);
            var midnight = currentDay.atStartOfDay();

            // 2. Cria um registro para cada dia com status REQUEST_VACATION e 00:00 como hora
            var vacationRequestRecord = new TimeRecord(
                    null,
                    midnight,
                    midnight, // Saída também às 00:00 para garantir horas trabalhadas = 0
                    StatusRecord.REQUEST_VACATION,
                    false,
                    true,
                    employeeId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Validação: evita duplicidade no dia
            if (timeRecordProvider.existsByEmployeeIdAndDate(employeeId, currentDay)) {
                throw new BadRequestException(ALREADY_REQUESTED + currentDay.format(DATE_FORMATTER));
            }

            timeRecordProvider.save(vacationRequestRecord);
            createdRecordIds.add(vacationRequestRecord.timeRecordId());
            log.info("Solicitação de férias (REQUEST_VACATION) criada para o dia {} para o funcionário {}", currentDay.format(DATE_FORMATTER), employeeId);
        }

        return createdRecordIds; // Retorna os IDs criados para referência
    }

    @Override
    public void approveVacation(VacationApprovalRequest request) {
        // Validação da Role: Apenas MANAGER ou CTO podem aprovar
        var userRole = jwtAuthenticatedUser.getRoleFromToken();
        if (!("MANAGER".equals(userRole) || "CTO".equals(userRole))) {
            throw new ForbiddenException(ONLY_MANAGERS_CAN_GRANT_VACATION);
        }

        // Aprova (muda o status) todos os registros na lista
        for (var recordId : request.timeRecordIds()) {
            var record = timeRecordProvider.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + recordId));

            if (record.statusRecord() == REQUEST_VACATION) {
                var approvedRecord = record.withStatus(VACATION); // 4. Manager aprova -> VACATION
                timeRecordProvider.save(approvedRecord);
                log.info("Solicitação de férias (ID: {}) APROVADA. Status mudou para VACATION.", recordId);
            } else {
                // Ignore ou lance exceção se tentar aprovar algo que não está em REQUEST_VACATION
                log.warn("Tentativa de aprovar registro de férias (ID: {}) com status inválido: {}", recordId, record.statusRecord());
            }
        }
    }

    @Override
    public void rejectVacation(VacationApprovalRequest request) {
        // Validação da Role: Apenas MANAGER ou CTO podem rejeitar
        var userRole = jwtAuthenticatedUser.getRoleFromToken();
        if (!("MANAGER".equals(userRole) || "CTO".equals(userRole))) {
            throw new ForbiddenException(ONLY_MANAGERS_CAN_REJECT_VACATION);
        }

        // Rejeita (muda o status) todos os registros na lista
        for (var recordId : request.timeRecordIds()) {
            var record = timeRecordProvider.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + recordId));

            if (record.statusRecord() == REQUEST_VACATION) {
                var rejectedRecord = record.withStatus(VACATION_REJECTED); // 4. Manager rejeita -> VACATION_REJECTED
                timeRecordProvider.save(rejectedRecord);
                log.info("Solicitação de férias (ID: {}) REJEITADA. Status mudou para VACATION_REJECTED.", recordId);
            } else {
                log.warn("Tentativa de rejeitar registro de férias (ID: {}) com status inválido: {}", recordId, record.statusRecord());
            }
        }
    }

    @Override
    public List<VacationRequestResponse> listVacationRequests(String statusFilter, String employeeName, int page, int size) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var companyId = getEmployee(employeeId).companyId();

        // 2. Definir os Status a serem buscados
        Set<StatusRecord> targetStatuses = switch (statusFilter.toUpperCase()) {
            case PENDING_STATUS -> Set.of(REQUEST_VACATION);
            case APPROVED_STATUS -> Set.of(VACATION);
            case REJECTED_STATUS -> Set.of(VACATION_REJECTED);
            default -> EnumSet.of(REQUEST_VACATION, VACATION, VACATION_REJECTED);
        };


        List<Employee> allEmployeesInCompany = employeeProvider.findByCompanyId(companyId);
        Map<UUID, Employee> employeeCache = allEmployeesInCompany.stream()
                .collect(Collectors.toMap(Employee::employeeId, emp -> emp));


        Set<UUID> filteredEmployeeIds = employeeName != null && !employeeName.isBlank()
                ? allEmployeesInCompany.stream()
                .filter(emp -> emp.fullName().toLowerCase().contains(employeeName.toLowerCase()))
                .map(Employee::employeeId)
                .collect(Collectors.toSet())
                : employeeCache.keySet();


        List<TimeRecord> allRecordsInScope = filteredEmployeeIds.stream()
                .flatMap(empId -> timeRecordProvider.findByEmployeeId(empId).stream())
                .filter(tr -> tr.startWork() != null)
                .filter(tr -> targetStatuses.contains(tr.statusRecord()))
                .toList();

        // 6. Agrupar por funcionário e Status, depois consolidar períodos
        List<VacationRequestResponse> consolidatedRequests = consolidateVacationPeriods(allRecordsInScope, employeeCache);

        // 7. Ordenar e Paginar (implementação manual)
        consolidatedRequests.sort(Comparator.comparing(VacationRequestResponse::startDate));
        int start = Math.min(page * size, consolidatedRequests.size());
        int end = Math.min(start + size, consolidatedRequests.size());

        return consolidatedRequests.subList(start, end);
    }

    @Override
    public Long requestTimeOff(RequestTimeOffRequest request, MultipartFile document) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);

        var parseStartTime = LocalTime.parse(request.startHour(), TIME_FORMATTER);
        var parseEndTime = LocalTime.parse(request.endHour(), TIME_FORMATTER);

        // Validações de lógica
        if (request.startDate().isAfter(request.endDate())) {
            throw new BadRequestException(START_DATE_BIGGER_THAN_END_DATE);
        }
        if (request.startDate().equals(request.endDate()) && parseStartTime.isAfter(parseEndTime)) {
            throw new BadRequestException(HOURS_EXCEPTIONS);
        }

        // Validação do Manager
        var managerUser = userProvider.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException(MANAGER_NOT_FOUND));
        if (managerUser.role() != Role.MANAGER) {
            throw new BadRequestException(USER_NOT_IS_MANAGER);
        }
        if (!employeeProvider.findById(managerUser.employeeId()).map(e -> e.companyId().equals(employee.companyId())).orElse(false)) {
            throw new BadRequestException(MANAGER_DIFFERENT_COMPANY);
        }

        var type = request.type() != null ? request.type() : RequestType.TIME_OFF_REQUEST;

        StatusRecord initialStatus;
        if (type == RequestType.FORGOTTEN_REGISTRATION) {
            initialStatus = WORK_TIME_REQUEST; // Esquecimento -> Solicitação de Trabalho
        } else {
            initialStatus = TIME_OFF_REQUEST;  // Abono -> Solicitação de Abono
        }

        var start = request.startDate();
        var end = request.endDate();
        long daysBetween = ChronoUnit.DAYS.between(start, end) + 1;

        // Variáveis para reutilizar os metadados e o caminho do arquivo físico (storagePath)
        String uploadedStoragePath = null;
        String documentFileName = null;
        String documentContentType = null;
        Long firstRecordId = null;

        for (int i = 0; i < daysBetween; i++) {
            var currentDay = start.plusDays(i);
            var currentStart = currentDay.atTime(parseStartTime);
            var currentEnd = currentDay.atTime(parseEndTime);

            // 1. Cria o registro de ponto
            var dailyTimeOffRecord = new TimeRecord(
                    null,
                    currentStart,
                    currentEnd,
                    initialStatus,
                    true,
                    true,
                    employeeId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            var savedRecord = timeRecordProvider.save(dailyTimeOffRecord);
            if (i == 0) {
                firstRecordId = savedRecord.timeRecordId();
            }

            // 3. Lógica de Upload Físico e Associação de Documento
            if (document != null && !document.isEmpty()) {
                if (i == 0) {
                    // Primeiro dia (i=0): Faz upload físico e salva a primeira Document Entity.
                    try {
                        documentService.uploadDocumentForTimeRecord(
                                DocumentType.TIME_OFF,
                                employeeId,
                                savedRecord.timeRecordId(),
                                document
                        );

                        // --- CORREÇÃO AQUI: Trata o retorno como LISTA ---
                        var uploadedDocs = documentProvider.findByTimeRecordId(savedRecord.timeRecordId());

                        if (uploadedDocs.isEmpty()) {
                            throw new IllegalStateException(DOC_NOT_FOUND);
                        }

                        // Pega o primeiro (ou único) documento da lista
                        var uploadedDoc = uploadedDocs.getFirst();

                        uploadedStoragePath = uploadedDoc.storagePath();
                        documentFileName = uploadedDoc.fileName();
                        documentContentType = uploadedDoc.contentType();
                        // --------------------------------------------------

                    } catch (IOException e) {
                        log.error("Falha ao salvar o documento de abono para o registro {}: {}", savedRecord.timeRecordId(), e.getMessage());
                        throw new BadRequestException(NOT_ABLE_TO_READ_FILE + e.getMessage());
                    }
                } else if (uploadedStoragePath != null) {
                    // Para os dias seguintes, reutiliza o caminho físico
                    var docToLink = new Document(
                            employeeId,
                            DocumentType.TIME_OFF,
                            documentFileName,
                            documentContentType,
                            uploadedStoragePath,
                            TIME_ZONE_BRAZIL,
                            savedRecord.timeRecordId(), false, false
                    );
                    documentProvider.save(docToLink);
                }
            }
        }

        if (firstRecordId == null) {
            throw new BadRequestException(FAILED_TO_CREATE_FIRST_RECORD);
        }
        return firstRecordId;
    }

    @Override
    public void approveTimeOff(Long timeRecordId) {
        var record = getTimeRecord(timeRecordId);

        if (record.statusRecord() == StatusRecord.TIME_OFF_REQUEST) {
            var approvedRecord = record.withStatus(StatusRecord.TIME_OFF);
            timeRecordProvider.save(approvedRecord);
            log.info("Abono aprovado para registro {}", timeRecordId);
        } else if (record.statusRecord() == StatusRecord.WORK_TIME_REQUEST) {
            var approvedRecord = record.withStatus(StatusRecord.UPDATED);
            timeRecordProvider.save(approvedRecord);
            log.info("Esquecimento aprovado (convertido em trabalho) para registro {}", timeRecordId);
        } else {
            throw new BadRequestException(INVALID_RECORD + record.statusRecord() + ").");
        }
    }

    @Override
    public void rejectTimeOff(Long timeRecordId) {
        var record = getTimeRecord(timeRecordId);


        if (record.statusRecord() == StatusRecord.TIME_OFF_REQUEST) {
            var rejectedRecord = record.withStatus(StatusRecord.TIME_OFF_REJECTED);
            timeRecordProvider.save(rejectedRecord);
            log.info("Abono negada para registro {}", timeRecordId);
        } else if (record.statusRecord() == StatusRecord.WORK_TIME_REQUEST) {
            var rejectedRecord = record.withStatus(StatusRecord.WORK_TIME_REJECTED);
            timeRecordProvider.save(rejectedRecord);
            log.info("Alteração para esquecimento negado para registro {}", timeRecordId);
        } else {
            throw new BadRequestException(INVALID_RECORD + record.statusRecord() + ").");
        }

    }

    @Override
    public TimeRecordPageResponse listTimeOffRequests(String statusFilter, String employeeName, int page, int size) {

        var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var companyId = getEmployee(managerEmployeeId).companyId();

        Set<StatusRecord> targetStatuses = switch (statusFilter.toUpperCase()) {
            case PENDING_STATUS -> Set.of(StatusRecord.TIME_OFF_REQUEST, StatusRecord.WORK_TIME_REQUEST);
            case APPROVED_STATUS -> Set.of(StatusRecord.TIME_OFF, StatusRecord.UPDATED);
            case REJECTED_STATUS -> Set.of(StatusRecord.TIME_OFF_REJECTED, StatusRecord.WORK_TIME_REJECTED);
            default -> EnumSet.of(StatusRecord.TIME_OFF_REQUEST, StatusRecord.TIME_OFF, StatusRecord.TIME_OFF_REJECTED);
        };

        List<Employee> allEmployeesInCompany = employeeProvider.findByCompanyId(companyId);
        Map<UUID, Employee> employeeCache = allEmployeesInCompany.stream()
                .collect(Collectors.toMap(Employee::employeeId, emp -> emp));

        Set<UUID> filteredEmployeeIds = employeeName != null && !employeeName.isBlank()
                ? employeeCache.values().stream()
                .filter(emp -> emp.fullName().toLowerCase().contains(employeeName.toLowerCase()))
                .map(Employee::employeeId)
                .collect(Collectors.toSet())
                : employeeCache.keySet();

        List<TimeRecord> timeOffRecords = filteredEmployeeIds.stream()
                .flatMap(empId -> timeRecordProvider.findByEmployeeId(empId).stream())
                .filter(tr -> tr.startWork() != null)
                .filter(tr -> targetStatuses.contains(tr.statusRecord()))
                .toList();

        var reference = Duration.ofHours(8);
        var companyName = companyUseCase.getCompanyNameById(companyId);

        List<TimeRecordResponse> mappedResponses = timeOffRecords.stream()
                .map(tr -> {
                    var emp = employeeCache.get(tr.employeeId());
                    var recordEmployeeData = new EmployeeData(emp.fullName(), companyName);

                    // --- CORREÇÃO AQUI: Trata o retorno como LISTA ---
                    var docs = documentProvider.findByTimeRecordId(tr.timeRecordId());

                    // Se a lista não estiver vazia, pega o ID do primeiro documento. Senão, null.
                    String documentPath = docs.isEmpty() ? null : docs.getFirst().documentId().toString();
                    // --------------------------------------------------

                    return TimeRecordResponse.fromDomain(tr, reference, recordEmployeeData, documentPath, null);
                })
                .sorted(Comparator.comparing(TimeRecordResponse::startWork).reversed())
                .collect(Collectors.toList());

        long totalElements = mappedResponses.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, (int) totalElements);
        int end = Math.min(start + size, (int) totalElements);

        List<TimeRecordResponse> pageContent = mappedResponses.subList(start, end);

        return new TimeRecordPageResponse(
                pageContent,
                totalPages,
                totalElements,
                page,
                page == 0,
                page >= totalPages - 1
        );
    }

    private List<VacationRequestResponse> consolidateVacationPeriods(List<TimeRecord> records, Map<UUID, Employee> employeeCache) {

        // 1. Agrupar por EmployeeId e Status
        Map<UUID, Map<StatusRecord, List<TimeRecord>>> grouped = records.stream()
                .collect(Collectors.groupingBy(
                        TimeRecord::employeeId,
                        Collectors.groupingBy(TimeRecord::statusRecord)
                ));

        List<VacationRequestResponse> consolidated = new ArrayList<>();

        for (var entryByEmployee : grouped.entrySet()) {
            UUID empId = entryByEmployee.getKey();
            Employee employee = employeeCache.get(empId);
            if (employee == null) continue;

            for (var entryByStatus : entryByEmployee.getValue().entrySet()) {
                List<TimeRecord> dailyRecords = entryByStatus.getValue();

                // Ordenar por data
                dailyRecords.sort(Comparator.comparing(tr -> tr.startWork().toLocalDate()));

                List<TimeRecord> currentPeriod = new ArrayList<>();
                for (TimeRecord record : dailyRecords) {
                    LocalDate currentDay = record.startWork().toLocalDate();

                    if (currentPeriod.isEmpty()) {
                        currentPeriod.add(record);
                        continue;
                    }

                    LocalDate lastDayInPeriod = currentPeriod.getLast().startWork().toLocalDate();

                    // Verifica se o dia atual é o dia imediatamente consecutivo
                    if (currentDay.isEqual(lastDayInPeriod.plusDays(1))) {
                        currentPeriod.add(record);
                    } else {
                        // O período contínuo quebrou. Finaliza o período anterior.
                        consolidated.add(VacationRequestResponse.fromConsolidatedPeriod(employee, currentPeriod));
                        currentPeriod = new ArrayList<>();
                        currentPeriod.add(record);
                    }
                }

                // Adicionar o último período remanescente, se houver
                if (!currentPeriod.isEmpty()) {
                    consolidated.add(VacationRequestResponse.fromConsolidatedPeriod(employee, currentPeriod));
                }
            }
        }
        return consolidated;
    }
    private static boolean validationStatus(Long timeRecordId, StatusRecord currentStatus, StatusRecord newStatus) {
        if (currentStatus == newStatus) {
            log.debug(LOG_UPDATE_STATUS_IDEMPOTENT, currentStatus, timeRecordId);
            return true;
        }
        if (currentStatus == PENDING_APPROVAL) {
            log.warn(UPDATE_STATUS_BLOCKED, timeRecordId);
            throw new BadRequestException(AWAITING_APPROVAL);
        }
        if (currentStatus == StatusRecord.UPDATED) {
            log.warn(RECORD_ALREADY_UPDATED_BLOCKED, timeRecordId);
            throw new BadRequestException(ALREADY_UPDATED);
        }
        if (currentStatus == StatusRecord.CLOSED) {
            log.warn(UPDATE_RECORD_CLOSED_BLOCKED, timeRecordId);
            throw new BadRequestException(ERR_STATUS_CLOSED);
        }
        return false;
    }
    private TimeRecord findRecordAndCheckStatus(Long timeRecordId) {
        var record = timeRecordProvider.findById(timeRecordId).orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + timeRecordId));

        if (record.statusRecord() != PENDING_APPROVAL) {
            throw new BadRequestException(RECORD_IS_NOT_AWAITING_APPROVAL);
        }
        return record;
    }
    private static void isRecordBelongsEmployee(UUID employeeId, TimeRecord record) {
        if (!record.employeeId().equals(employeeId)) {
            throw new BadRequestException(RECORD_NOT_BELONGS_EMPLOYEE);
        }
    }
    private static Duration getDuration(String reference) {
        String[] parts = reference.split(":");
        return Duration.ofHours(Long.parseLong(parts[0])).plusMinutes(Long.parseLong(parts[1]));
    }
    private Employee getEmployee(UUID uuid) {
        return employeeProvider.findById(uuid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + uuid));
    }
    private EmployeeData getEmployeeData(UUID employeeId) {
        var employee = getEmployee(employeeId);
        var company = companyProvider.findById(employee.companyId()).orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));
        var employeeName = employee.fullName();
        var companyName = company.name();
        return new EmployeeData(employeeName, companyName);
    }
    private TimeRecord getTimeRecord(Long timeRecordId) {
        return timeRecordProvider.findById(timeRecordId).orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + timeRecordId));
    }
    private void validateManagerEligibility(UUID managerId, UUID employeeCompanyId) {
        var managerUser = userProvider.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(ERR_MANAGER_NOT_FOUND));

        if (managerUser.role() != Role.MANAGER) {
            throw new BadRequestException(ERR_USER_NOT_MANAGER);
        }

        var managerEmployee = employeeProvider.findById(managerUser.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (!managerEmployee.companyId().equals(employeeCompanyId)) {
            log.error("Tentativa de aprovação cross-company detectada. ManagerID: {}, CompanyID: {}",
                    managerId, employeeCompanyId);
            throw new BadRequestException(ERR_MANAGER_DIFF_COMPANY);
        }
    }
    private TimeRecord getRecord(UUID employeeId, Long timeRecordId) {
        var employee = getEmployee(employeeId);
        var record = getTimeRecord(timeRecordId);

        isRecordBelongsEmployee(employee.employeeId(), record);
        return record;
    }
    private void checkGeolocation(UUID employeeId, double requestLatitude, double requestLongitude) {
        var employee = getEmployee(employeeId);

        var company = companyProvider.findById(employee.companyId()).orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND_FOR_THE_EMPLOYEE));

        final double ALLOWED_DISTANCE_METERS = 80.0;
        var companyLocation = company.location();

        if (companyLocation == null) {
            throw new BadRequestException(ADDRESS_COMPANY_IS_NOT_REGISTERED);
        }

        // Você precisará de uma função para calcular a distância entre os pontos
        double distance = calculateDistanceInMeters(companyLocation.latitude(), companyLocation.longitude(), requestLatitude, requestLongitude);

        if (distance > ALLOWED_DISTANCE_METERS) {
            throw new BadRequestException(GEOLOCATION_OUT_OF_RANGE);
        }
    }
    private List<TimeRecord> getRecords(UUID employeeId, Boolean active) {
        // Encontra todos os registros (segmentos)
        List<TimeRecord> immutableRecords = active == null ? timeRecordProvider.findByEmployeeId(employeeId) : timeRecordProvider.findByEmployeeIdAndActive(employeeId, active);

        // CORREÇÃO: Cria uma lista mutável a partir da imutável para permitir a ordenação.
        List<TimeRecord> records = new ArrayList<>(immutableRecords);

        // Ordena os registros por data/hora de início
        records.sort(Comparator.comparing(TimeRecord::startWork, Comparator.nullsLast(Comparator.naturalOrder())));
        return records;
    }
    private double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        // Implementação da fórmula de Haversine ou outra mais precisa.
        final int R = 6371; // Raio da Terra em km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // Retorna a distância em metros
    }
    private void validateFaceRecognition(UUID expectedEmployeeId, String faceImageBase64) {
        try {
            // 1. Decodifica a string Base64 para um array de bytes
            byte[] imageBytes = Base64.getDecoder().decode(faceImageBase64);

            // 2. Cria um InputStream a partir dos bytes
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

            // 3. Executa a busca facial no Rekognition
            UUID recognizedEmployeeId = faceRecognitionProvider.searchFaceByImage(inputStream);

            if (recognizedEmployeeId == null) {
                throw new BadRequestException(FACE_NOT_RECOGNIZED);
            }

            // 4. Compara o ID retornado pelo Rekognition com o ID do usuário autenticado
            if (!expectedEmployeeId.equals(recognizedEmployeeId)) {
                log.warn("Tentativa de registro de ponto com face inválida. Autenticado: {}, Reconhecido: {}", expectedEmployeeId, recognizedEmployeeId);
                throw new BadRequestException(FACE_MISMATCH);
            }

            log.info("✅ Validação facial concluída com sucesso para o colaborador: {}", expectedEmployeeId);

        } catch (IllegalArgumentException e) {
            // Ocorre se a string Base64 for malformada
            throw new BadRequestException(INVALID_BASE64_IMAGE);
        } catch (RuntimeException e) {
            // Captura falhas de serviço do Rekognition (lançadas pelo provider)
            log.error("Erro no serviço de reconhecimento facial: {}", e.getMessage(), e);
            throw new BadRequestException(INVALID_BASE64_IMAGE);
        }
    }
    private void isHomeOffice(GeolocationRequest request, Employee employee, UUID employeeId) {
        if (!employee.homeOffice()) {
            // Se NÃO estiver em home office, a validação de geolocalização é obrigatória
            checkGeolocation(employeeId, request.latitude(), request.longitude());
        } else {
            // Log para indicar que a validação foi pulada
            log.info("Funcionário {} está em Home Office. Validação de geolocalização ignorada.", employeeId);
        }
    }
    private void generateAndSaveReceipt(Employee employee, Long timeRecordId, LocalDateTime recordTime, Long nsr, String typeSuffix) {
        try {
            // 1. Busca dados da empresa (Caching recomendado em produção)
            var company = companyProvider.findById(employee.companyId())
                    .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

            // 2. Gera os bytes do PDF (Assinado e com Hash) via ReceiptPdfService
            byte[] pdfContent = receiptPdfService.generateReceipt(company, employee, recordTime, nsr);

            // 3. Define nomenclatura padrão do arquivo
            String fileName = String.format("comprovante_%d_%s_%s.pdf",
                    nsr,
                    typeSuffix,
                    recordTime.format(RECEIPT_DATE_FMT));

            // 4. Salva usando o método otimizado do DocumentService
            documentService.uploadGeneratedDocument(
                    DocumentType.POINT_RECORD_RECEIPT,
                    employee.employeeId(),
                    timeRecordId,
                    pdfContent,
                    fileName
            );

        } catch (Exception e) {
            // Loga erro crítico mas não aborta a transação principal do ponto para não prejudicar o usuário
            log.error("FALHA AO GERAR COMPROVANTE (NSR {}): {}", nsr, e.getMessage());
        }
    }
    private void validateTimeConsistency(LocalDate startDate, LocalDate endDate,
                                         LocalTime startTime, LocalTime endTime, Long recordId) {
        if (startDate.equals(endDate) && startTime.isAfter(endTime)) {
            log.warn(LOG_DATE_VALIDATION_ERR, recordId);
            throw new BadRequestException(ERR_TIME_INCONSISTENCY);
        }
    }
    private void handlePartnerUpdateFlow(UpdateTimeRecordRequest req, Employee employee,
                                         TimeRecord record, LocalDateTime newStart, LocalDateTime newEnd) {
        // Valida colisão (apenas para parceiro, pois Manager tem poder de override)
        validateNonBreakOverlap(employee.employeeId(), record.timeRecordId(), newStart, newEnd);

        if (req.managerId() == null) {
            throw new BadRequestException(ERR_MANAGER_REQUIRED);
        }

        // Validação Hierárquica do Gestor
        validateManagerEligibility(req.managerId(), employee.companyId());

        // Criação do Payload de Aprovação
        var approvalRequest = new TimeRecordApprovalRequest(
                record.timeRecordId(),
                employee.employeeId(),
                req.managerId(),
                newStart,
                newEnd,
                TIME_ZONE_BRAZIL
        );

        approvalProvider.save(approvalRequest);

        // Atualiza status do registro original para bloqueado/pendente
        var updatedRecord = record.withStatus(PENDING_APPROVAL).withEdited(true);
        timeRecordProvider.save(updatedRecord);

        log.info(LOG_PARTNER_APPROVAL, employee.employeeId(), req.managerId());
    }
    private void handleManagerUpdateFlow(Employee employee, TimeRecord record,
                                         LocalDateTime newStart, LocalDateTime newEnd) {
        // Ajuste inteligente de pausas adjacentes (Lógica complexa isolada)
        adjustAdjacentRecordsOnUpdate(employee.employeeId(), record, newStart, newEnd);

        var statusUpdate = record.statusRecord().onUpdate();

        var updated = record
                .withCheckin(newStart)
                .withCheckout(newEnd)
                .withEdited(true)
                .withStatus(statusUpdate);

        timeRecordProvider.save(updated);

        log.info(LOG_MANAGER_UPDATE, record.timeRecordId());
    }
    /**
     * Ajusta os registros de Pausa Implícita (IMPLICIT_BREAK) vizinhos ao registro de trabalho
     * sendo atualizado.
     * * Esta lógica DEVE ser chamada apenas na aplicação final da mudança (MANAGER/CTO ou Approve).
     */
    private void adjustAdjacentRecordsOnUpdate(UUID employeeId, TimeRecord recordToUpdate, LocalDateTime newStart, LocalDateTime newEnd) {
        // 1. Obter todos os registros (incluindo breaks) do dia, ordenados.
        LocalDate day = newStart.toLocalDate();
        List<TimeRecord> allDayRecords = timeRecordProvider.findByEmployeeId(employeeId).stream().filter(tr -> tr.startWork() != null && tr.startWork().toLocalDate().equals(day)).sorted(Comparator.comparing(TimeRecord::startWork)).collect(Collectors.toCollection(ArrayList::new));

        if (allDayRecords.isEmpty()) return;

        // 2. Encontrar o índice do registro que está sendo atualizado (no contexto de todos os registros do dia)
        int index = -1;
        for (int i = 0; i < allDayRecords.size(); i++) {
            if (allDayRecords.get(i).timeRecordId().equals(recordToUpdate.timeRecordId())) {
                index = i;
                break;
            }
        }

        if (index == -1) return; // Registro não encontrado, ignorar ajuste

        // 3. Checar e ajustar o registro PRECEDENTE (P)
        if (index > 0) {
            TimeRecord preceding = allDayRecords.get(index - 1);

            // 3a. Se o anterior for uma PAUSA, ajustamos (ou removemos) a pausa
            if (preceding.statusRecord() == StatusRecord.IMPLICIT_BREAK) {

                LocalDateTime newEndBreak = newStart;

                // Se a pausa original for consumida (startBreak >= newEndBreak), delete
                if (preceding.startWork().isAfter(newEndBreak) || preceding.startWork().isEqual(newEndBreak)) {
                    timeRecordProvider.deleteTimeRecord(preceding);
                    log.info("Pausa {} consumida pela edição e deletada.", preceding.timeRecordId());
                } else {
                    // Caso contrário, ajusta o fim da pausa
                    TimeRecord updatedBreak = preceding.withCheckout(newEndBreak).withStatus(StatusRecord.IMPLICIT_BREAK);
                    timeRecordProvider.save(updatedBreak);
                    log.info("Pausa {} ajustada para terminar em {}.", preceding.timeRecordId(), newEndBreak.format(TIME_FORMATTER));
                }
            }
        }

        // 4. Checar e ajustar o registro SUCEDENTE (N)
        if (index < allDayRecords.size() - 1) {
            TimeRecord succeeding = allDayRecords.get(index + 1);

            // 4a. Se o seguinte for uma PAUSA, ajustamos (ou removemos) a pausa
            if (succeeding.statusRecord() == StatusRecord.IMPLICIT_BREAK) {

                LocalDateTime newStartBreak = newEnd;

                // Se a pausa original for consumida (endBreak <= newStartBreak), delete
                if (succeeding.endWork() != null && (succeeding.endWork().isBefore(newStartBreak) || succeeding.endWork().isEqual(newStartBreak))) {
                    timeRecordProvider.deleteTimeRecord(succeeding);
                    log.info("Pausa {} consumida pela edição e deletada.", succeeding.timeRecordId());
                } else {
                    // Caso contrário, ajusta o início da pausa
                    TimeRecord updatedBreak = new TimeRecord(succeeding.timeRecordId(), newStartBreak, // Novo start
                            succeeding.endWork(), // Fim original
                            StatusRecord.IMPLICIT_BREAK, succeeding.edited(), succeeding.active(), succeeding.employeeId(), null, null, null, null, null, null, null, null);
                    timeRecordProvider.save(updatedBreak);
                    log.info("Pausa {} ajustada para começar em {}.", succeeding.timeRecordId(), newStartBreak.format(TIME_FORMATTER));
                }
            }
        }
    }
    /**
     * Calcula a duração total das pausas (gaps) entre os segmentos de trabalho no mesmo dia.
     * Presume que a lista de TimeRecords está ordenada por startWork.
     */
    private Duration calculateTotalBreakDuration(List<TimeRecord> segments, ZoneId zoneId) {
        Duration totalBreak = Duration.ZERO;

        for (int i = 0; i < segments.size() - 1; i++) {
            TimeRecord currentSegment = segments.get(i);
            TimeRecord nextSegment = segments.get(i + 1);

            // 1. O segmento atual deve ter um Checkout (endWork)
            if (currentSegment.endWork() == null) {
                continue;
            }

            // 2. Ambos devem ser no mesmo dia (data de início)
            LocalDate currentDay = currentSegment.startWork().atZone(zoneId).toLocalDate();
            LocalDate nextDay = nextSegment.startWork().atZone(zoneId).toLocalDate();

            if (currentDay.equals(nextDay)) {
                // 3. O próximo segmento deve ter um Checkin (startWork)
                // O gap entre o Check-out do anterior e o Check-in do próximo
                Duration breakDuration = Duration.between(currentSegment.endWork(), nextSegment.startWork());
                totalBreak = totalBreak.plus(breakDuration);
            }
        }
        return totalBreak;
    }
    /**
     * Valida se o novo intervalo de tempo se sobrepõe a qualquer REGISTRO DE TRABALHO adjacente
     * (não-pausa) no mesmo dia.
     */
    private void validateNonBreakOverlap(UUID employeeId, Long currentRecordId, LocalDateTime newStart, LocalDateTime newEnd) {
        LocalDate day = newStart.toLocalDate();
        Set<StatusRecord> nonBreakStatuses = EnumSet.complementOf(EnumSet.of(StatusRecord.IMPLICIT_BREAK, StatusRecord.DAY_OFF, StatusRecord.TIME_OFF, StatusRecord.ABSENCE));

        // 1. Buscar todos os registros de trabalho (non-breaks) do dia, exceto o que está sendo editado
        List<TimeRecord> workSegments = timeRecordProvider.findByEmployeeId(employeeId).stream().filter(tr -> !tr.timeRecordId().equals(currentRecordId)).filter(tr -> tr.startWork() != null && tr.startWork().toLocalDate().equals(day)).filter(tr -> nonBreakStatuses.contains(tr.statusRecord())).filter(tr -> tr.endWork() != null) // Só checa segmentos fechados
                .sorted(Comparator.comparing(TimeRecord::startWork)).toList();

        for (TimeRecord segment : workSegments) {
            // Verifica se o novo registro começa antes do fim de outro segmento
            if (newStart.isBefore(segment.endWork()) && newEnd.isAfter(segment.startWork())) {
                throw new BadRequestException(NEW_REGISTER_OVERRIDES_AN_EXISTING_WORK_RECORD + segment.startWork().format(GENERATION_DATE_FMT) + " - " + segment.endWork().format(GENERATION_DATE_FMT) + ").");
            }
        }
    }
    private String formatDuration(Duration duration, boolean includeSign) {
        long hours = Math.abs(duration.toHours());
        long minutes = Math.abs(duration.toMinutesPart());
        String formatted = String.format("%02d:%02d", hours, minutes);

        if (includeSign) {
            return duration.isNegative() ? "-" + formatted : "+" + formatted;
        }
        return formatted;
    }
    private Duration parseReferenceTime(String reference) {
        try {
            String[] parts = reference.split(":");
            return Duration.ofHours(Long.parseLong(parts[0])).plusMinutes(Long.parseLong(parts[1]));
        } catch (Exception e) {
            log.error(PARSE_ERROR, reference);
            throw new BadRequestException(ERR_INVALID_REFERENCE);
        }
    }
    private DailyCalculationResult processDailyRecords(LocalDate startDate, List<TimeRecord> dailyRecords,
                                                       Duration referenceDuration, Set<StatusRecord> workStatuses,
                                                       Set<StatusRecord> specialStatuses) {

        var firstStartHour = dailyRecords.getFirst().startWork().atZone(SAO_PAULO).toLocalTime().format(TIME_FORMATTER);

        var lastEndWork = dailyRecords.stream()
                .map(TimeRecord::endWork)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(startDate.atStartOfDay());

        var endDate = lastEndWork.atZone(SAO_PAULO).toLocalDate();

        boolean hasPending = dailyRecords.stream().anyMatch(tr -> tr.statusRecord() == StatusRecord.PENDING);
        var lastEndHour = hasPending ? "" : lastEndWork.atZone(SAO_PAULO).toLocalTime().format(TIME_FORMATTER);

        // Duração Líquida Trabalhada
        var dailyWorkedLiquid = dailyRecords.stream()
                .filter(tr -> workStatuses.contains(tr.statusRecord()) || specialStatuses.contains(tr.statusRecord()))
                .filter(tr -> tr.endWork() != null)
                .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                .reduce(Duration.ZERO, Duration::plus);

        // Duração de Pausas
        var dailyBreakDuration = dailyRecords.stream()
                .filter(tr -> tr.statusRecord() == StatusRecord.IMPLICIT_BREAK && tr.endWork() != null)
                .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                .reduce(Duration.ZERO, Duration::plus);

        // Determina o status primário do dia
        var dailyStatus = dailyRecords.stream()
                .filter(tr -> workStatuses.contains(tr.statusRecord()) || specialStatuses.contains(tr.statusRecord()))
                .min(Comparator.comparing(TimeRecord::startWork))
                .map(TimeRecord::statusRecord)
                .orElse(StatusRecord.DAY_OFF);

        // Cálculo do Balanço (Banco de Horas / Horas Extras)
        var dailyBalance = Duration.ZERO;
        if (!specialStatuses.contains(dailyStatus)) {
            dailyBalance = hasPending ? Duration.ZERO : dailyWorkedLiquid.minus(referenceDuration);
        }

        // Cria o DTO do dia
        var reportDay = new SimpleReportDay(
                startDate, endDate, firstStartHour, lastEndHour,
                formatDuration(dailyWorkedLiquid, false),
                formatDuration(dailyBreakDuration, false),
                formatDuration(dailyBalance, true)
        );

        return new DailyCalculationResult(reportDay, dailyWorkedLiquid, dailyBreakDuration, dailyBalance);
    }

    private Map<LocalDate, String> calculateDailyBalances(List<TimeRecord> recordsInRange, Duration reference) {
        Map<LocalDate, List<TimeRecord>> recordsByDay = recordsInRange.stream()
                .collect(Collectors.groupingBy(tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate()));

        Map<LocalDate, String> dailyBalanceMap = new HashMap<>();

        recordsByDay.forEach((date, dailyRecords) -> {
            var isAbsence = dailyRecords.stream().anyMatch(tr -> tr.statusRecord() == StatusRecord.ABSENCE);
            var isDayOffOrVacation = dailyRecords.stream().anyMatch(tr ->
                    tr.statusRecord() == StatusRecord.DAY_OFF ||
                            tr.statusRecord() == StatusRecord.VACATION ||
                            tr.statusRecord() == StatusRecord.TIME_OFF
            );

            if (isAbsence) {
                dailyBalanceMap.put(date, String.format("-%02d:%02d", reference.toHours(), reference.toMinutesPart()));
            } else if (isDayOffOrVacation) {
                dailyBalanceMap.put(date, "+00:00");
            } else {
                var netWorkDuration = dailyRecords.stream()
                        .filter(tr -> tr.endWork() != null && tr.statusRecord() != StatusRecord.IMPLICIT_BREAK)
                        .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                        .reduce(Duration.ZERO, Duration::plus);

                var balance = netWorkDuration.minus(reference);
                dailyBalanceMap.put(date, formatDuration(balance, true)); // Utilizando aquele método utilitário que criamos antes!
            }
        });

        return dailyBalanceMap;
    }

    private Map<Long, String> fetchLatestDocumentsInBulk(List<TimeRecord> records) {
        // 1. Coleta todos os IDs dos registros desta página/relatório
        List<Long> recordIds = records.stream()
                .map(TimeRecord::timeRecordId)
                .toList();

        log.debug(LOG_LIST_REPORT_FETCH_DOCS, recordIds.size());

        List<Document> allDocs = documentProvider.findByTimeRecordIdIn(recordIds);

        // 2. Agrupa por TimeRecordId e extrai apenas o ID do documento mais recente em formato String
        return allDocs.stream()
                .collect(Collectors.groupingBy(
                        Document::timeRecordId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(Document::uploadedAt)),
                                optDoc -> optDoc.map(doc -> doc.documentId().toString()).orElse(null)
                        )
                ));
    }
}