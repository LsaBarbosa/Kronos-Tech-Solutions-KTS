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
import com.kts.kronos.domain.model.*;
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
import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.domain.model.enuns.StatusRecord.PENDING_APPROVAL;
import static com.kts.kronos.domain.model.enuns.StatusRecord.UPDATE_REJECTED;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TimeRecordService implements TimeRecordUseCase {

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
                LOG_START_REQ, jwtAuthenticatedUser.getEmployeeId(),
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

        /* Verifica se já existe um registro de FOLGA ou FALTA para hoje <<<
        Isso permite que o funcionário trabalhe no dia que o sistema achava que era folga.
        Necessário buscar qualquer registro do dia, independente de estar "open"
        */
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
            // Criar registro
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

        } else if ("MANAGER".equals(userRole)) {
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

        var approvalData = fetchApprovalDataOrThrow(timeRecordId);

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
        fetchApprovalDataOrThrow(timeRecordId);

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
        var managerId = jwtAuthenticatedUser.getEmployeeId();
        var manager = employeeProvider.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var companyId = manager.companyId();

        log.info(LOG_LIST_APPROVALS_INIT, managerId, companyId, page);

        var pageable = PageRequest.of(page, size);

        Page<TimeRecordApprovalRequest> approvalsPage = approvalProvider.findAllByCompanyId(pageable, employeeName, companyId);

        // Fail-Fast:
        if (approvalsPage.isEmpty()) {
            return createEmptyPageResponse(approvalsPage);
        }

        log.debug(LOG_LIST_APPROVALS_BULK, approvalsPage.getNumberOfElements(), page);

        //  Bulk Fetching de todas as dependências
        List<TimeRecordApprovalRequest> approvals = approvalsPage.getContent();

        // Agrupa e busca em lote
        Map<Long, TimeRecord> recordsMap = fetchTimeRecordsInBulk(approvals);
        Map<UUID, Employee> employeesMap = fetchEmployeesInBulk(approvals);
        Map<UUID, User> usersMap = fetchUsersInBulk(approvals);

        Map<Long, String> documentsMap = fetchLatestDocumentsInBulkForApprovals(approvals);

        // Montagem da Resposta (Cruzamento de dados O(1) em memória)
        List<TimeRecordApprovalResponse> responses = approvals.stream()
                .map(approvalData -> buildApprovalResponse(approvalData, recordsMap, employeesMap, usersMap, documentsMap))
                .filter(Objects::nonNull)
                .toList();

        log.info(LOG_LIST_APPROVALS_SUCCESS, responses.size(), managerId);

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

        log.info(LOG_VACATION_REQ_INIT, employeeId, request.startDate(), request.endDate());

        validateManagerEligibilityForVacation(request.managerId(), employee.companyId());
        validateVacationDates(request.startDate(), request.endDate());

        validateNoConflictingRecords(employeeId, request.startDate(), request.endDate());

        log.debug(LOG_VACATION_VALIDATION_OK, employeeId);

        var daysBetween = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;

        List<TimeRecord> recordsToSave = getTimeRecords(request, daysBetween, employeeId);

        List<TimeRecord> savedRecords = timeRecordProvider.saveAll(recordsToSave);

        List<Long> createdRecordIds = savedRecords.stream()
                .map(TimeRecord::timeRecordId)
                .toList();

        log.info(LOG_VACATION_SUCCESS, createdRecordIds.size(), employeeId);

        return createdRecordIds;
    }

    @Override
    public void approveVacation(VacationApprovalRequest request) {
        log.info(LOG_VACATION_APPROVE_INIT, request.timeRecordIds().size());
        validateManagerRoleForApproval();

        List<TimeRecord> records = timeRecordProvider.findByIdIn(new HashSet<>(request.timeRecordIds()));

        checkDataIntegrity(records, request.timeRecordIds(), LOG_VACATION_APPROVE_MISMATCH);

        List<TimeRecord> recordsToApprove = new ArrayList<>();

        for (TimeRecord record : records) {
            if (record.statusRecord() == StatusRecord.REQUEST_VACATION) {
                recordsToApprove.add(record.withStatus(StatusRecord.VACATION));
            } else {
                log.warn(LOG_VACATION_APPROVE_INVALID, record.timeRecordId(), record.statusRecord());
            }
        }

        if (!recordsToApprove.isEmpty()) {
            timeRecordProvider.saveAll(recordsToApprove);
            log.info(LOG_VACATION_APPROVE_SUCCESS, recordsToApprove.size());
        }
    }

    @Override
    public void rejectVacation(VacationApprovalRequest request) {
        log.info(LOG_VACATION_REJECT_INIT, request.timeRecordIds().size());

        validateManagerRoleForApproval();

        List<TimeRecord> records = timeRecordProvider.findByIdIn(new HashSet<>(request.timeRecordIds()));
        checkDataIntegrity(records, request.timeRecordIds(), LOG_VACATION_REJECT_MISMATCH);

        List<TimeRecord> recordsToReject = new ArrayList<>();

        for (TimeRecord record : records) {
            if (record.statusRecord() == StatusRecord.REQUEST_VACATION) {
                recordsToReject.add(record.withStatus(StatusRecord.VACATION_REJECTED));
            } else {
                log.warn(LOG_VACATION_REJECT_INVALID, record.timeRecordId(), record.statusRecord());
            }
        }

        if (!recordsToReject.isEmpty()) {
            timeRecordProvider.saveAll(recordsToReject);
            log.info(LOG_VACATION_REJECT_SUCCESS, recordsToReject.size());
        }
    }

    @Override
    public List<VacationRequestResponse> listVacationRequests(String statusFilter, String employeeName, int page, int size) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var companyId = getEmployee(employeeId).companyId();
        log.info(LOG_VACATION_LIST_INIT, companyId, statusFilter);

        Set<StatusRecord> targetStatuses = resolveTargetStatuses(statusFilter);
        List<Employee> allEmployeesInCompany = employeeProvider.findByCompanyId(companyId);

        Map<UUID, Employee> employeeCache = allEmployeesInCompany.stream()
                .collect(Collectors.toMap(Employee::employeeId, emp -> emp));

        Set<UUID> filteredEmployeeIds = filterEmployeeIdsByName(allEmployeesInCompany, employeeName);

        if (filteredEmployeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug(LOG_VACATION_LIST_FETCH, filteredEmployeeIds.size());

        List<TimeRecord> allRecordsInScope = timeRecordProvider.findByEmployeeIdInAndStatusesIn(
                filteredEmployeeIds,
                targetStatuses
        );

        List<VacationRequestResponse> consolidatedRequests = consolidateVacationPeriods(allRecordsInScope, employeeCache);

        consolidatedRequests.sort(Comparator.comparing(VacationRequestResponse::startDate));
        int start = Math.min(page * size, consolidatedRequests.size());
        int end = Math.min(start + size, consolidatedRequests.size());

        log.info(LOG_VACATION_LIST_SUCCESS, consolidatedRequests.size());

        return consolidatedRequests.subList(start, end);
    }

    @Override
    public Long requestTimeOff(RequestTimeOffRequest request, MultipartFile document) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);

        log.info(LOG_TIME_OFF_INIT, employeeId, request.startDate(), request.endDate());

        var parseStartTime = LocalTime.parse(request.startHour(), TIME_FORMATTER);
        var parseEndTime = LocalTime.parse(request.endHour(), TIME_FORMATTER);

        validateTimeOffDates(request.startDate(), request.endDate(), parseStartTime, parseEndTime);
        validateManagerEligibilityForTimeOff(request.managerId(), employee.companyId());

        var type = request.type() != null ? request.type() : RequestType.TIME_OFF_REQUEST;
        var initialStatus = (type == RequestType.FORGOTTEN_REGISTRATION)
                ? StatusRecord.WORK_TIME_REQUEST
                : StatusRecord.TIME_OFF_REQUEST;

        var daysBetween = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        log.debug(LOG_TIME_OFF_VALIDATION, daysBetween);

        List<TimeRecord> recordsToSave = new ArrayList<>();

        for (int i = 0; i < daysBetween; i++) {
            var currentDay = request.startDate().plusDays(i);
            recordsToSave.add(new TimeRecord(
                    null,
                    currentDay.atTime(parseStartTime),
                    currentDay.atTime(parseEndTime),
                    initialStatus,
                    true,
                    true,
                    employeeId,
                    null, null, null, null, null, null, null, null
            ));
        }

        List<TimeRecord> savedRecords = timeRecordProvider.saveAll(recordsToSave);

        if (savedRecords.isEmpty()) {
            throw new BadRequestException(FAILED_TO_CREATE_FIRST_RECORD);
        }

        var firstRecordId = savedRecords.getFirst().timeRecordId();

        if (document != null && !document.isEmpty()) {
            processTimeOffDocument(document, employeeId, savedRecords);
        }

        log.info(LOG_TIME_OFF_SUCCESS, savedRecords.size(), firstRecordId);

        return firstRecordId;
    }

    @Override
    public void approveTimeOff(TimeOffApprovalRequest request) {
        log.info(LOG_TIME_OFF_APPROVE_INIT, request);

        validateManagerRoleForApproval();

        List<TimeRecord> records = timeRecordProvider.findByIdIn(new HashSet<>(request.timeRecordIds()));
        checkDataIntegrity(records, request.timeRecordIds(), LOG_TIME_OFF_BATCH_MISMATCH);

        List<TimeRecord> recordsToApprove = new ArrayList<>();

        for (TimeRecord record : records) {
            StatusRecord newStatus = null;

            if (record.statusRecord() == StatusRecord.TIME_OFF_REQUEST) {
                newStatus = StatusRecord.TIME_OFF;
            } else if (record.statusRecord() == StatusRecord.WORK_TIME_REQUEST) {
                newStatus = StatusRecord.UPDATED;
            }

            if (newStatus != null) {
                recordsToApprove.add(record.withStatus(newStatus));
            } else {
                log.warn(LOG_TIME_OFF_BATCH_INVALID, record.timeRecordId(), record.statusRecord());
            }
        }

        if (!recordsToApprove.isEmpty()) {
            timeRecordProvider.saveAll(recordsToApprove);
            log.info(LOG_TIME_OFF_BATCH_SUCCESS, recordsToApprove.size());
        }
    }

    @Override
    public void rejectTimeOff(TimeOffApprovalRequest request) {
        log.info(LOG_TIME_OFF_REJECT_BATCH_INIT, request.timeRecordIds().size());

        validateManagerRoleForApproval();

        List<TimeRecord> records = timeRecordProvider.findByIdIn(new HashSet<>(request.timeRecordIds()));

        checkDataIntegrity(records, request.timeRecordIds(), LOG_TIME_OFF_BATCH_MISMATCH);

        List<TimeRecord> recordsToReject = new ArrayList<>();

        for (TimeRecord record : records) {
            StatusRecord newStatus = null;

            if (record.statusRecord() == StatusRecord.TIME_OFF_REQUEST) {
                newStatus = StatusRecord.TIME_OFF_REJECTED;
            } else if (record.statusRecord() == StatusRecord.WORK_TIME_REQUEST) {
                newStatus = StatusRecord.WORK_TIME_REJECTED;
            }

            if (newStatus != null) {
                recordsToReject.add(record.withStatus(newStatus));
            } else {
                log.warn(LOG_TIME_OFF_REJECT_BATCH_INVALID, record.timeRecordId(), record.statusRecord());
            }
        }

        if (!recordsToReject.isEmpty()) {
            timeRecordProvider.saveAll(recordsToReject);
            log.info(LOG_TIME_OFF_REJECT_BATCH_SUCCESS, recordsToReject.size());
        }
    }

    @Override
    public TimeRecordPageResponse listTimeOffRequests(String statusFilter, String employeeName, int page, int size) {
        var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var companyId = getEmployee(managerEmployeeId).companyId();

        log.info(LOG_TIME_OFF_LIST_INIT, companyId, statusFilter);

        Set<StatusRecord> targetStatuses = resolveTimeOffTargetStatuses(statusFilter);

        // Cache e Filtragem de Funcionários (Em Memória)
        List<Employee> allEmployeesInCompany = employeeProvider.findByCompanyId(companyId);
        Map<UUID, Employee> employeeCache = allEmployeesInCompany.stream()
                .collect(Collectors.toMap(Employee::employeeId, emp -> emp));

        Set<UUID> filteredEmployeeIds = filterEmployeeIdsByName(allEmployeesInCompany, employeeName);

        if (filteredEmployeeIds.isEmpty()) {
            return createEmptyPageResponse(page);
        }

        log.debug(LOG_TIME_OFF_LIST_FETCH, filteredEmployeeIds.size());

        // Bulk Fetching de Registros de Ponto (Push-Down Filter)
        List<TimeRecord> timeOffRecords = timeRecordProvider.findByEmployeeIdInAndStatusesIn(
                filteredEmployeeIds,
                targetStatuses
        );

        if (timeOffRecords.isEmpty()) {
            return createEmptyPageResponse(page);
        }

        // Paginação Preemptiva (Antes do DTO e dos Documentos)
        // Primeiro, ordenamos a entidade leve de domínio
        timeOffRecords.sort(Comparator.comparing(TimeRecord::startWork).reversed());

        long totalElements = timeOffRecords.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, (int) totalElements);
        int end = Math.min(start + size, (int) totalElements);

        // EApenas os registros que vão aparecer na tela do usuário
        List<TimeRecord> pageRecords = timeOffRecords.subList(start, end);

        if (pageRecords.isEmpty()) {
            return new TimeRecordPageResponse(List.of(), totalPages, totalElements, page, page == 0, page >= totalPages - 1);
        }

        log.debug(LOG_TIME_OFF_LIST_PAGINATION, totalElements, pageRecords.size());

        // Bulk Fetching de Documentos (Apenas para a página atual!)
        Map<Long, String> documentsMap = fetchFirstDocumentPathInBulk(pageRecords);


        var reference = Duration.ofHours(8);
        var companyName = companyUseCase.getCompanyNameById(companyId);

        List<TimeRecordResponse> pageContent = pageRecords.stream()
                .map(tr -> {
                    var emp = employeeCache.get(tr.employeeId());
                    var recordEmployeeData = new EmployeeData(emp.fullName(), companyName);

                    // Pega o documento mapeado em memória (O(1) de complexidade)
                    var documentPath = documentsMap.get(tr.timeRecordId());

                    return TimeRecordResponse.fromDomain(tr, reference, recordEmployeeData, documentPath, null);
                })
                .toList();

        log.info(LOG_TIME_OFF_LIST_SUCCESS, page + 1, totalPages);

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
            log.error(CROSS_COMPANY_DETECTED,
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
                log.warn(INVALID_FACE, expectedEmployeeId, recognizedEmployeeId);
                throw new BadRequestException(FACE_MISMATCH);
            }

            log.info(FACIAL_VALIDATION_SUCCESS, expectedEmployeeId);

        } catch (BadRequestException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(INVALID_BASE64_IMAGE);
        } catch (RuntimeException ex) {
            log.error(FACIAL_RECOGNITION_ERROR, ex.getMessage(), ex);
            throw new BadRequestException(INVALID_BASE64_IMAGE);
        }
    }

    private void isHomeOffice(GeolocationRequest request, Employee employee, UUID employeeId) {
        if (!employee.homeOffice()) {
            checkGeolocation(employeeId, request.latitude(), request.longitude());
        } else {
            log.info(SKIP_GEOLOCATION_VALIDATION, employeeId);
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
            String fileName = String.format(PROOF_PDF,
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
            log.error(GENERATE_PROOF_NSR_ERROR, nsr, e.getMessage());
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
        var startOfDay = day.atStartOfDay();
        var endOfDay = day.plusDays(1).atStartOfDay().minusNanos(1);

        List<TimeRecord> allDayRecords = timeRecordProvider.findByRange(employeeId, startOfDay, endOfDay).stream()
                .filter(tr -> tr.startWork() != null)
                .sorted(Comparator.comparing(TimeRecord::startWork))
                .collect(Collectors.toCollection(ArrayList::new));

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
                // Se a pausa original for consumida, delete
                if (preceding.startWork().isAfter(newStart) || preceding.startWork().isEqual(newStart)) {
                    deleteImplicitBreakRecord(preceding);
                } else {
                    // Caso contrário, ajusta o fim da pausa
                    TimeRecord updatedBreak = preceding.withCheckout(newStart).withStatus(StatusRecord.IMPLICIT_BREAK);
                    timeRecordProvider.save(updatedBreak);
                    log.info("Pausa {} ajustada para terminar em {}.", preceding.timeRecordId(), newStart.format(TIME_FORMATTER));
                }
            }
        }

        // 4. Checar e ajustar o registro SUCEDENTE (N)
        if (index < allDayRecords.size() - 1) {
            TimeRecord succeeding = allDayRecords.get(index + 1);

            // 4a. Se o seguinte for uma PAUSA, ajustamos (ou removemos) a pausa
            if (succeeding.statusRecord() == StatusRecord.IMPLICIT_BREAK) {
                // Se a pausa original for consumida, delete
                if (succeeding.endWork() != null && (succeeding.endWork().isBefore(newEnd) || succeeding.endWork().isEqual(newEnd))) {
                    deleteImplicitBreakRecord(succeeding);
                } else {
                    // Caso contrário, ajusta o início da pausa
                    TimeRecord updatedBreak = new TimeRecord(succeeding.timeRecordId(), newEnd, // Novo start (usa newEnd direto)
                            succeeding.endWork(), StatusRecord.IMPLICIT_BREAK, succeeding.edited(), succeeding.active(), succeeding.employeeId(), null, null, null, null, null, null, null, null);
                    timeRecordProvider.save(updatedBreak);
                    log.info(BREAK_UPDATED_TO_START, succeeding.timeRecordId(), newEnd.format(TIME_FORMATTER));
                }
            }
        }
    }


    /**
     * Valida se o novo intervalo de tempo se sobrepõe a qualquer REGISTRO DE TRABALHO adjacente
     * (não-pausa) no mesmo dia.
     */
    private void validateNonBreakOverlap(UUID employeeId, Long currentRecordId, LocalDateTime newStart, LocalDateTime newEnd) {
        LocalDate day = newStart.toLocalDate();
        var startOfDay = day.atStartOfDay();
        var endOfDay = day.plusDays(1).atStartOfDay().minusNanos(1);
        Set<StatusRecord> nonBreakStatuses = EnumSet.complementOf(EnumSet.of(StatusRecord.IMPLICIT_BREAK, StatusRecord.DAY_OFF, StatusRecord.TIME_OFF, StatusRecord.ABSENCE));

        // 1. Buscar todos os registros de trabalho (non-breaks) do dia, exceto o que está sendo editado
        List<TimeRecord> workSegments = timeRecordProvider.findByRange(employeeId, startOfDay, endOfDay).stream()
                .filter(tr -> !tr.timeRecordId().equals(currentRecordId))
                .filter(tr -> tr.startWork() != null)
                .filter(tr -> nonBreakStatuses.contains(tr.statusRecord()))
                .filter(tr -> tr.endWork() != null) // Só checa segmentos fechados
                .sorted(Comparator.comparing(TimeRecord::startWork))
                .toList();

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
        Set<Long> recordIds = records.stream()
                .map(TimeRecord::timeRecordId)
                .collect(Collectors.toSet());

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

    private TimeRecordApprovalPageResponse createEmptyPageResponse(Page<?> emptyPage) {
        return new TimeRecordApprovalPageResponse(
                Collections.emptyList(),
                emptyPage.getTotalPages(),
                emptyPage.getTotalElements(),
                emptyPage.getNumber(),
                emptyPage.isFirst(),
                emptyPage.isLast()
        );
    }

    private Map<Long, TimeRecord> fetchTimeRecordsInBulk(List<TimeRecordApprovalRequest> approvals) {
        Set<Long> recordIds = approvals.stream()
                .map(TimeRecordApprovalRequest::timeRecordId)
                .collect(Collectors.toSet());

        return timeRecordProvider.findByIdIn(recordIds).stream()
                .collect(Collectors.toMap(TimeRecord::timeRecordId, tr -> tr));
    }

    private Map<UUID, Employee> fetchEmployeesInBulk(List<TimeRecordApprovalRequest> approvals) {
        List<UUID> employeeIds = approvals.stream().map(TimeRecordApprovalRequest::requestingEmployeeId).distinct().toList();
        return employeeProvider.findByIdIn(employeeIds).stream()
                .collect(Collectors.toMap(Employee::employeeId, emp -> emp));
    }

    private Map<UUID, User> fetchUsersInBulk(List<TimeRecordApprovalRequest> approvals) {
        List<UUID> managerIds = approvals.stream().map(TimeRecordApprovalRequest::managerId).distinct().toList();
        return userProvider.findByIdIn(managerIds).stream()
                .collect(Collectors.toMap(User::userId, user -> user));
    }

    private Map<Long, String> fetchLatestDocumentsInBulkForApprovals(List<TimeRecordApprovalRequest> approvals) {
        Set<Long> recordIds = approvals.stream()
                .map(TimeRecordApprovalRequest::timeRecordId)
                .collect(Collectors.toSet());

        List<Document> allDocs = documentProvider.findByTimeRecordIdIn(recordIds);

        return allDocs.stream()
                .collect(Collectors.groupingBy(
                        Document::timeRecordId,
                        Collectors.collectingAndThen(
                                // Pega o primeiro ou o mais recente, conforme  regra
                                Collectors.minBy(Comparator.comparing(Document::uploadedAt)),
                                optDoc -> optDoc.map(doc -> "/documents/" + doc.documentId()).orElse(null)
                        )
                ));
    }

    private TimeRecordApprovalResponse buildApprovalResponse(
            TimeRecordApprovalRequest approvalData,
            Map<Long, TimeRecord> recordsMap,
            Map<UUID, Employee> employeesMap,
            Map<UUID, User> usersMap,
            Map<Long, String> documentsMap) {

        // Lookups O(1) - Ultra rápido, sem tocar no banco de dados
        var timeRecord = recordsMap.get(approvalData.timeRecordId());
        var partnerEmployee = employeesMap.get(approvalData.requestingEmployeeId());
        var managerUser = usersMap.get(approvalData.managerId());
        var documentPath = documentsMap.get(approvalData.timeRecordId());

        // Se houver inconsistência no banco de dados (ex: funcionário deletado fisicamente), ignora o registro
        if (partnerEmployee == null || managerUser == null || timeRecord == null) {
            log.warn(LOG_LIST_APPROVALS_WARN, approvalData.timeRecordId());
            return null;
        }

        return new TimeRecordApprovalResponse(
                approvalData.timeRecordId(),
                partnerEmployee.fullName(),
                managerUser.username(),
                approvalData.newStartWork(),
                approvalData.newEndWork(),
                timeRecord.startWork(),
                timeRecord.endWork(),
                documentPath
        );
    }

    private static List<TimeRecord> getTimeRecords(RequestVacationRequest request, long daysBetween, UUID employeeId) {
        List<TimeRecord> recordsToSave = new ArrayList<>();

        for (int i = 0; i < daysBetween; i++) {
            var currentDay = request.startDate().plusDays(i);
            var midnight = currentDay.atStartOfDay();

            recordsToSave.add(new TimeRecord(
                    null,
                    midnight,
                    midnight,
                    StatusRecord.REQUEST_VACATION,
                    false,
                    true,
                    employeeId,
                    null, null, null, null, null, null, null, null
            ));
        }
        return recordsToSave;
    }

    private void validateManagerEligibilityForVacation(UUID managerId, UUID employeeCompanyId) {
        var managerUser = userProvider.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(MANAGER_NOT_FOUND));

        if (managerUser.role() != Role.MANAGER) {
            throw new ForbiddenException(ROLE_IS_NOT_MANAGER);
        }

        var isSameCompany = employeeProvider.findById(managerUser.employeeId())
                .map(e -> e.companyId().equals(employeeCompanyId))
                .orElse(false);

        if (!isSameCompany) {
            throw new BadRequestException(MANAGER_DIFFERENT_COMPANY);
        }
    }

    private void validateVacationDates(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BadRequestException(ERR_VACATION_INVALID_DATES);
        }
    }

    private void validateNoConflictingRecords(UUID employeeId, LocalDate start, LocalDate end) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);

        var existingRecords = timeRecordProvider.findByRange(employeeId, startDateTime, endDateTime);

        if (!existingRecords.isEmpty()) {
            log.warn(ERROR_REQUEST_VACATION, employeeId);
            throw new BadRequestException(ERR_VACATION_CONFLICT);
        }
    }

    private void validateManagerRoleForApproval() {
        var userRole = jwtAuthenticatedUser.getRoleFromToken();

        if (!("MANAGER".equals(userRole))) {
            log.warn(AUTHORIZATION_ERROR, userRole);
            throw new ForbiddenException(ERR_ONLY_MANAGERS_CAN_GRANT);
        }
    }

    private Set<StatusRecord> resolveTargetStatuses(String statusFilter) {
        if (statusFilter == null) {
            return EnumSet.of(REQUEST_VACATION, VACATION, VACATION_REJECTED);
        }

        return switch (statusFilter.toUpperCase()) {
            case PENDING_STATUS -> Set.of(REQUEST_VACATION);
            case APPROVED_STATUS -> Set.of(VACATION);
            case REJECTED_STATUS -> Set.of(VACATION_REJECTED);
            default -> EnumSet.of(REQUEST_VACATION, VACATION, VACATION_REJECTED);
        };
    }

    private Set<UUID> filterEmployeeIdsByName(List<Employee> employees, String employeeName) {
        if (employeeName == null || employeeName.isBlank()) {
            return employees.stream().map(Employee::employeeId).collect(Collectors.toSet());
        }

        var lowerCaseName = employeeName.toLowerCase();
        return employees.stream()
                .filter(emp -> emp.fullName().toLowerCase().contains(lowerCaseName))
                .map(Employee::employeeId)
                .collect(Collectors.toSet());
    }

    private void processTimeOffDocument(MultipartFile document, UUID employeeId, List<TimeRecord> savedRecords) {
        var firstRecordId = savedRecords.getFirst().timeRecordId();

        try {
            documentService.uploadDocumentForTimeRecord(
                    DocumentType.TIME_OFF,
                    employeeId,
                    firstRecordId,
                    document
            );

            log.debug(LOG_TIME_OFF_DOC_LINK, firstRecordId);

            // Se for só 1 dia de abono, não precisamos fazer mais nada.
            if (savedRecords.size() > 1) {
                // 5.2 Recupera os metadados do arquivo recém-salvo
                var uploadedDocs = documentProvider.findByTimeRecordId(firstRecordId);
                if (uploadedDocs.isEmpty()) {
                    throw new IllegalStateException(DOC_NOT_FOUND);
                }
                var uploadedDoc = uploadedDocs.getFirst();

                // 5.3 Gera os vínculos de documento para os dias RESTANTES em memória
                List<Document> remainingDocsToSave = savedRecords.stream()
                        .skip(1) // Pula o primeiro dia ( já foi salvo pelo DocumentService)
                        .map(tr -> new Document(
                                employeeId,
                                DocumentType.TIME_OFF,
                                uploadedDoc.fileName(),
                                uploadedDoc.contentType(),
                                uploadedDoc.storagePath(),
                                TIME_ZONE_BRAZIL,
                                tr.timeRecordId(),
                                false,
                                false
                        ))
                        .toList();

                documentProvider.saveAll(remainingDocsToSave);
                log.debug(DOCS_SAVED, remainingDocsToSave.size());
            }
        } catch (IOException e) {
            log.error(ERROR_SAVE_DOCUMENT, firstRecordId, e.getMessage());
            throw new BadRequestException(NOT_ABLE_TO_READ_FILE + e.getMessage());
        }
    }

    private void validateTimeOffDates(LocalDate start, LocalDate end, LocalTime startTime, LocalTime endTime) {
        if (start.isAfter(end)) {
            throw new BadRequestException(START_DATE_BIGGER_THAN_END_DATE);
        }
        if (start.equals(end) && startTime.isAfter(endTime)) {
            throw new BadRequestException(HOURS_EXCEPTIONS);
        }
    }

    private void validateManagerEligibilityForTimeOff(UUID managerId, UUID employeeCompanyId) {
        var managerUser = userProvider.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(MANAGER_NOT_FOUND));

        if (managerUser.role() != Role.MANAGER) {
            throw new BadRequestException(USER_NOT_IS_MANAGER);
        }

        var isSameCompany = employeeProvider.findById(managerUser.employeeId())
                .map(e -> e.companyId().equals(employeeCompanyId))
                .orElse(false);

        if (!isSameCompany) {
            throw new BadRequestException(MANAGER_DIFFERENT_COMPANY);
        }
    }

    private static void checkDataIntegrity(List<TimeRecord> records, List<Long> request, String logTimeOffBatchMismatch) {
        if (records.size() != request.size()) {
            log.error(logTimeOffBatchMismatch, request.size(), records.size());
            throw new ResourceNotFoundException(ERR_RECORDS_NOT_FOUND_BATCH);
        }
    }

    private Set<StatusRecord> resolveTimeOffTargetStatuses(String statusFilter) {
        if (statusFilter == null) {
            return EnumSet.of(StatusRecord.TIME_OFF_REQUEST, StatusRecord.TIME_OFF, StatusRecord.TIME_OFF_REJECTED);
        }

        return switch (statusFilter.toUpperCase()) {
            case PENDING_STATUS -> Set.of(StatusRecord.TIME_OFF_REQUEST, StatusRecord.WORK_TIME_REQUEST);
            case APPROVED_STATUS -> Set.of(StatusRecord.TIME_OFF, StatusRecord.UPDATED);
            case REJECTED_STATUS -> Set.of(StatusRecord.TIME_OFF_REJECTED, StatusRecord.WORK_TIME_REJECTED);
            default -> EnumSet.of(StatusRecord.TIME_OFF_REQUEST, StatusRecord.TIME_OFF, StatusRecord.TIME_OFF_REJECTED);
        };
    }

    private Map<Long, String> fetchFirstDocumentPathInBulk(List<TimeRecord> records) {
        Set<Long> recordIds = records.stream()
                .map(TimeRecord::timeRecordId)
                .collect(Collectors.toSet());

        List<Document> allDocs = documentProvider.findByTimeRecordIdIn(recordIds);

        // Agrupa e pega o ID do primeiro documento encontrado para cada registro
        return allDocs.stream()
                .collect(Collectors.groupingBy(
                        Document::timeRecordId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.isEmpty() ? null : list.getFirst().documentId().toString()
                        )
                ));
    }

    private TimeRecordPageResponse createEmptyPageResponse(int page) {
        return new TimeRecordPageResponse(List.of(), 0, 0, page, true, true);
    }
    private TimeRecordApprovalRequest fetchApprovalDataOrThrow(Long timeRecordId) {
        return approvalProvider.findByTimeRecordId(timeRecordId).orElseThrow(() -> {
            log.warn(LOG_APPROVAL_NOT_FOUND, timeRecordId);
            return new ResourceNotFoundException(ERR_APPROVAL_REQ_NOT_FOUND + timeRecordId);
        });
    }
    private void deleteImplicitBreakRecord(TimeRecord breakRecord) {
        timeRecordProvider.deleteTimeRecord(breakRecord);
        log.info(BREAK_EDITED, breakRecord.timeRecordId());
    }
}
