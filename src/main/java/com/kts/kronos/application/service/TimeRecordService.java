package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationRequestResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.application.port.out.projection.VacationRequestPeriodProjection;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.RequestType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final TimeRecordProvider recordRepository;
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
    private final NsrProvider nsrProvider;     // Provider de Sequência Atômica
    private final NtpTimeService ntpTimeService; // Validação de Relógio
    private final DomainAuthorizationService domainAuthorizationService;
    private final BiometricProtectionService biometricProtectionService;
    private final LegalConsentProvider legalConsentProvider;
    private final PrivacyLogReferenceService privacyLogReferenceService;
    @Autowired
    private KronosMetrics kronosMetrics = new KronosMetrics();
    @Autowired
    private KronosTracing kronosTracing = new KronosTracing();

    @Override
    public ActionResponse registerTime(GeolocationRequest request) {
        long startedAt = System.nanoTime();
        final StatusRecord[] convertedFromStatus = new StatusRecord[1];
        final boolean[] implicitBreakCreated = {false};

        try {
            ActionResponse response = kronosTracing.observe("kronos.time_record.register", () -> {
                ntpTimeService.validateSystemTime(10);

                var employeeId = jwtAuthenticatedUser.getEmployeeId();
                var employee = getEmployee(employeeId);

                // LGPD-102: Validate biometric consent before allowing facial checkin
                boolean hasBiometricConsent = legalConsentProvider.existsActive(
                        employeeId,
                        ConsentType.BIOMETRIC_AUTHENTICATION
                );
                if (!hasBiometricConsent) {
                    throw new TermsNotAcceptedException(
                            "Consentimento biométrico necessário para registrar ponto com biometria.",
                            "https://termo.kronossolutions.tech/"
                    );
                }

                biometricProtectionService.protectCheckIn(
                        employeeId,
                        request.faceImageBase64(),
                        request.livenessPassed()
                );

                validateFaceRecognition(employeeId, request.faceImageBase64());
                isHomeOffice(request, employee, employeeId);

                var openRecordOpt = recordRepository.findOpenByEmployeeId(employee.employeeId());
                var currentTime = LocalDateTime.now(SAO_PAULO);
                var currentTimeParsed = currentTime.format(TIME_FORMATTER);
                var todayDate = currentTime.toLocalDate();

                var company = companyProvider.findById(employee.companyId())
                        .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

                if (openRecordOpt.isPresent()) {
                    var open = openRecordOpt.get();
                    var openRecordDate = open.startWork().atZone(SAO_PAULO).toLocalDate();

                    if (openRecordDate.isEqual(todayDate)) {
                        if (open.statusRecord() != PENDING) {
                            throw new BadRequestException(STATUS_CHECKOUT + open.statusRecord() + ")");
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

                        recordRepository.save(updated);
                        adfUseCase.logMarking(company, employee, currentTime, nsrCheckout);
                        generateAndSaveReceipt(employee, updated.timeRecordId(), currentTime, nsrCheckout, EXIT);

                        return new ActionResponse("Saída às " + currentTimeParsed + "! (NSR: " + nsrCheckout + ")", CHECKOUT);
                    }
                }

                var nsrCheckin = nsrProvider.generateNextNsr(employee.companyId());
                var actionType = CHECKIN;

                var startOfDay = todayDate.atStartOfDay();
                var endOfDay = todayDate.atTime(23, 59, 59);

                List<TimeRecord> recordsToday = recordRepository.findByRange(
                        employee.employeeId(), startOfDay, endOfDay);

                Optional<TimeRecord> dayOffOrAbsenceRecord = recordsToday.stream()
                        .filter(r -> r.statusRecord() == StatusRecord.DAY_OFF || r.statusRecord() == StatusRecord.ABSENCE)
                        .findFirst();

                TimeRecord recordToSave;

                if (dayOffOrAbsenceRecord.isPresent()) {
                    var existing = dayOffOrAbsenceRecord.get();
                    convertedFromStatus[0] = existing.statusRecord();
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
                    var latestRecordOpt = recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employee.employeeId());

                    if (latestRecordOpt.isPresent()) {
                        var latest = latestRecordOpt.get();
                        var latestEndWork = latest.endWork();
                        var currentStartDay = currentTime.toLocalDate();
                        var latestEndDay = latestEndWork != null ? latestEndWork.atZone(SAO_PAULO).toLocalDate() : null;

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
                            recordRepository.save(breakRecord);
                            actionType = CHECKIN_AFTER_BREAK;
                            implicitBreakCreated[0] = true;
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

                var savedRecord = recordRepository.save(recordToSave);
                adfUseCase.logMarking(company, employee, currentTime, nsrCheckin);
                generateAndSaveReceipt(employee, savedRecord.timeRecordId(), currentTime, nsrCheckin, "ENTRADA");

                var message = switch (actionType) {
                    case CHECKIN_AFTER_BREAK -> "Entrada após pausa às " + currentTimeParsed + "! (NSR: " + nsrCheckin + ")";
                    case CHECKIN_ON_DAY_OFF ->
                            "Registro de folga convertido para trabalho às " + currentTimeParsed + "! (NSR: " + nsrCheckin + ")";
                    default -> "Entrada às " + currentTimeParsed + "! (NSR: " + nsrCheckin + ")";
                };

                return new ActionResponse(message, actionType);
            });

            if (CHECKOUT.equals(response.actionType())) {
                kronosMetrics.timeRecordCheckoutSuccess();
                kronosMetrics.recordTimeRecordDuration("checkout", Duration.ofNanos(System.nanoTime() - startedAt));
                log.info("event=time_record_register result=success action=checkout");
            } else {
                kronosMetrics.timeRecordCheckinSuccess();
                kronosMetrics.recordTimeRecordDuration("checkin", Duration.ofNanos(System.nanoTime() - startedAt));
                if (implicitBreakCreated[0]) {
                    kronosMetrics.timeRecordImplicitBreak();
                }
                if (convertedFromStatus[0] == StatusRecord.DAY_OFF) {
                    kronosMetrics.timeRecordDayOffConverted();
                }
                if (convertedFromStatus[0] == StatusRecord.ABSENCE) {
                    kronosMetrics.timeRecordAbsenceConverted();
                }
                log.info("event=time_record_register result=success action=checkin");
            }

            return response;
        } catch (RuntimeException e) {
            String reason = resolveTimeRecordFailureReason(e);
            kronosMetrics.timeRecordFailure(reason);
            log.warn("event=time_record_register result=failure reason={}", reason);
            throw e;
        }
    }

    @Override
    public void updateTimeRecord(Long timeRecordId, UpdateTimeRecordRequest req) {
        var userRole = jwtAuthenticatedUser.getCurrentRole();
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);
        var record = getTimeRecord(timeRecordId);

        isRecordBelongsEmployee(employee.employeeId(), record);

        var parseStartTime = LocalTime.parse(req.startHour(), TIME_FORMATTER);
        var parseEndTime = LocalTime.parse(req.endHour(), TIME_FORMATTER);

        var newStart = LocalDateTime.of(req.startDate(), parseStartTime);
        var newEnd = LocalDateTime.of(req.endDate(), parseEndTime);


        if (req.startDate().equals(req.endDate()) && parseStartTime.isAfter(parseEndTime)) {
            throw new BadRequestException(HOURS_EXCEPTIONS);
        }

        // --- VALIDAÇÃO DE PRÉ-APROVAÇÃO ---

        if (userRole == Role.PARTNER) {
            // Apenas valida sobreposição contra segmentos de trabalho adjacentes
            validateNonBreakOverlap(employeeId, record.timeRecordId(), newStart, newEnd);

            if (req.managerId() == null) {
                throw new BadRequestException(MANAGER_ID_REQUIRED);
            }
            getManagerApprover(req.managerId(), employee.companyId(), USER_IS_NOT_MANAGER, false);

            // 1. Cria o payload simplificado
            var approvalRequest = new TimeRecordApprovalRequest(timeRecordId, employeeId, req.managerId(), newStart, newEnd, TIME_ZONE_BRAZIL);

            // 2. Persiste a solicitação
            approvalProvider.save(approvalRequest);

            var updatedRecord = record.withStatus(PENDING_APPROVAL).withEdited(true);
            recordRepository.save(updatedRecord);

        } else if (userRole == Role.MANAGER || userRole == Role.CTO) {
            // Lógica para o MANAGER/CTO (aprovação direta)

            // NOVO: Executa o ajuste dos registros de Pausa vizinhos
            adjustAdjacentRecordsOnUpdate(employeeId, record, newStart, newEnd);

            var statusUpdate = record.statusRecord().onUpdate();
            var updated = record.withCheckin(newStart).withCheckout(newEnd).withEdited(true).withStatus(statusUpdate);
            recordRepository.save(updated);
        } else {
            throw new ForbiddenException(UNAUTHORIZED_ROLE);
        }
    }

    @Override
    public void approveTimeRecordChange(Long timeRecordId) {
        var record = findRecordAndCheckStatus(timeRecordId);

        // Busca a solicitação
        var approvalData = approvalProvider.findByTimeRecordId(timeRecordId).orElseThrow(()
                -> new ResourceNotFoundException(REQUEST_NOT_FOUND + timeRecordId));

        // --- NOVO: Executa o ajuste dos registros de Pausa vizinhos ANTES de aplicar o ponto ---
        adjustAdjacentRecordsOnUpdate(record.employeeId(), record, approvalData.newStartWork(), approvalData.newEndWork());
        // ---------------------------------------------------------------------------------------

        // 1. Aplica as alterações no registro principal
        var approvedRecord = record.withCheckin(approvalData.newStartWork()).withCheckout(approvalData.newEndWork()).withStatus(StatusRecord.UPDATED);

        // 2. Salva o registro principal atualizado
        recordRepository.save(approvedRecord);

        // 3. Limpa o registro de aprovação
        approvalProvider.deleteByTimeRecordId(timeRecordId);

        log.info("Solicitação para o registro {} foi APROVADA.", timeRecordId);
        kronosMetrics.timeAdjustmentApproved();
    }

    @Override
    public void rejectTimeRecordChange(Long timeRecordId) {
        var record = findRecordAndCheckStatus(timeRecordId);

        // Reverte o status do registro.
        var rejectedRecord = record.withStatus(UPDATE_REJECTED).withEdited(false);
        recordRepository.save(rejectedRecord);

        // Limpa o registro de aprovação
        approvalProvider.deleteByTimeRecordId(timeRecordId);

        log.info("Solicitação para o registro {} foi REJEITADA.", timeRecordId);
        kronosMetrics.timeAdjustmentRejected();
    }

    @Override
    public void deleteTimeRecord(UUID employeeId, Long recordId) {
        var record = getRecord(employeeId, recordId);
        recordRepository.deleteTimeRecord(record);
    }

    @Override
    public void toggleActivate(UUID employeeId, Long timeRecordId) {
        var record = getRecord(employeeId, timeRecordId);
        var toggle = record.withActive(!record.active());
        recordRepository.save(toggle);
    }

    @Override
    public void updateStatus(UUID employeeId, Long timeRecordId, UpdateTimeRecordStatusRequest req) {
        var record = getRecord(employeeId, timeRecordId);
        var currentStatus = record.statusRecord();
        if (currentStatus == PENDING_APPROVAL) {
            throw new BadRequestException(AWAITING_APPROVAL);
        }
        if (currentStatus == StatusRecord.UPDATED) {
            throw new BadRequestException(ALREADY_UPDATED);
        }
        var updateStatus = record.withStatus(req.statusRecord());
        recordRepository.save(updateStatus);
    }

    @Override
    public List<TimeRecordResponse> listReport(UUID employeeId, ListReportRequest req) {
        var targetEmployeeId = domainAuthorizationService.authorizeEmployeeAccess(employeeId).employeeId();
        var employeeData = getEmployeeData(targetEmployeeId);
        var reference = getDuration(req.reference());

        if (req.dates() == null || req.dates().length == 0) {
            return Collections.emptyList();
        }

        final Set<LocalDate> finalDatesSet = Arrays.stream(req.dates())
                .collect(Collectors.toCollection(TreeSet::new));

        var allPossibleReportStatuses = EnumSet.of(
                CREATED, PENDING, UPDATED, UPDATE_REJECTED, DAY_OFF, ABSENCE,
                PENDING_APPROVAL, TIME_OFF, TIME_OFF_REQUEST, TIME_OFF_REJECTED,
                IMPLICIT_BREAK, REQUEST_VACATION, VACATION, VACATION_REJECTED,
                WORK_TIME_REQUEST, WORK_TIME_REJECTED
        );

        Set<StatusRecord> finalFilterStatuses;
        if (req.statuses() == null || req.statuses().isEmpty()) {
            finalFilterStatuses = allPossibleReportStatuses;
        } else {
            finalFilterStatuses = req.statuses().stream()
                    .filter(allPossibleReportStatuses::contains)
                    .collect(Collectors.toSet());
        }

        var rangeStart = finalDatesSet.stream()
                .min(LocalDate::compareTo)
                .orElseThrow()
                .atStartOfDay();

        var rangeEnd = finalDatesSet.stream()
                .max(LocalDate::compareTo)
                .orElseThrow()
                .atTime(23, 59, 59);

        List<TimeRecord> recordsInRange = recordRepository.findReportRecords(
                        targetEmployeeId,
                        rangeStart,
                        rangeEnd,
                        finalFilterStatuses,
                        req.active()
                ).stream()
                .filter(tr -> finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate()))
                .sorted(Comparator.comparing(TimeRecord::startWork))
                .collect(Collectors.toCollection(ArrayList::new));

        Map<Long, String> documentPathByTimeRecordId = getLatestDocumentPathByTimeRecordId(recordsInRange);

        Map<LocalDate, List<TimeRecord>> recordsByDay = recordsInRange.stream()
                .collect(Collectors.groupingBy(tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate()));

        Map<LocalDate, String> dailyBalanceMap = new HashMap<>();

        recordsByDay.forEach((date, dailyRecords) -> {
            var isAbsence = dailyRecords.stream().anyMatch(tr -> tr.statusRecord() == ABSENCE);
            var isDayOffOrVacation = dailyRecords.stream().anyMatch(tr ->
                    tr.statusRecord() == DAY_OFF ||
                            tr.statusRecord() == VACATION ||
                            tr.statusRecord() == TIME_OFF
            );

            String balanceStr;

            if (isAbsence) {
                balanceStr = String.format("-%02d:%02d", reference.toHours(), reference.toMinutesPart());
            } else if (isDayOffOrVacation) {
                balanceStr = "+00:00";
            } else {
                List<TimeRecord> closedRecords = dailyRecords.stream()
                        .filter(tr -> tr.endWork() != null)
                        .sorted(Comparator.comparing(TimeRecord::startWork))
                        .toList();

                if (closedRecords.isEmpty()) {
                    balanceStr = "+00:00";
                } else {
                    var netWorkDuration = closedRecords.stream()
                            .filter(tr -> tr.statusRecord() != StatusRecord.IMPLICIT_BREAK)
                            .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                            .reduce(Duration.ZERO, Duration::plus);

                    var balance = netWorkDuration.minus(reference);

                    var sign = balance.isNegative() ? "-" : "+";
                    balanceStr = sign + String.format(
                            "%02d:%02d",
                            Math.abs(balance.toHours()),
                            Math.abs(balance.toMinutesPart())
                    );
                }
            }

            dailyBalanceMap.put(date, balanceStr);
        });

        return recordsInRange.stream()
                .map(tr -> {
                    var date = tr.startWork().atZone(SAO_PAULO).toLocalDate();
                    var dailyBalance = dailyBalanceMap.getOrDefault(date, "+00:00");

                    return TimeRecordResponse.fromDomain(
                            tr,
                            reference,
                            employeeData,
                            documentPathByTimeRecordId.get(tr.timeRecordId()),
                            dailyBalance
                    );
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public TimeRecordApprovalPageResponse listPendingApprovals(int page, int size, String employeeName) {
        var managerId = jwtAuthenticatedUser.getEmployeeId();
        var manager = employeeProvider.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var companyId = manager.companyId();

        var pageable = PageRequest.of(page, size);
        Page<TimeRecordApprovalRequest> approvalsPage =
                approvalProvider.findAllByCompanyId(pageable, employeeName, companyId);

        if (approvalsPage.isEmpty()) {
            return new TimeRecordApprovalPageResponse(
                    Collections.emptyList(),
                    approvalsPage.getTotalPages(),
                    approvalsPage.getTotalElements(),
                    approvalsPage.getNumber(),
                    approvalsPage.isFirst(),
                    approvalsPage.isLast()
            );
        }

        Set<Long> timeRecordIds = approvalsPage.getContent().stream()
                .map(TimeRecordApprovalRequest::timeRecordId)
                .collect(Collectors.toSet());

        Set<UUID> requestingEmployeeIds = approvalsPage.getContent().stream()
                .map(TimeRecordApprovalRequest::requestingEmployeeId)
                .collect(Collectors.toSet());

        Set<UUID> managerUserIds = approvalsPage.getContent().stream()
                .map(TimeRecordApprovalRequest::managerId)
                .collect(Collectors.toSet());

        Map<Long, TimeRecord> timeRecordsById = recordRepository.findAllByIds(timeRecordIds).stream()
                .collect(Collectors.toMap(TimeRecord::timeRecordId, tr -> tr));

        Map<UUID, Employee> employeesById = employeeProvider.findAllByIds(requestingEmployeeIds).stream()
                .collect(Collectors.toMap(Employee::employeeId, employee -> employee));

        Map<UUID, User> managerUsersById = userProvider.findAllByIds(managerUserIds).stream()
                .collect(Collectors.toMap(User::userId, user -> user));

        Map<Long, String> latestDocumentIdByTimeRecordId = buildLatestDocumentIdMap(timeRecordIds);

        List<TimeRecordApprovalResponse> responses = approvalsPage.getContent().stream()
                .map(approvalData -> {
                    var timeRecord = timeRecordsById.get(approvalData.timeRecordId());
                    var partnerEmployee = employeesById.get(approvalData.requestingEmployeeId());
                    var managerUser = managerUsersById.get(approvalData.managerId());

                    if (partnerEmployee == null || managerUser == null || timeRecord == null) {
                        return null;
                    }

                    var documentId = latestDocumentIdByTimeRecordId.get(approvalData.timeRecordId());
                    var documentPath = documentId == null ? null : "/documents/" + documentId;

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
                })
                .filter(Objects::nonNull)
                .toList();

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
        getManagerApprover(request.managerId(), employee.companyId(), ROLE_IS_NOT_MANAGER, true);

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
            if (recordRepository.existsByEmployeeIdAndDate(employeeId, currentDay)) {
                throw new BadRequestException(ALREADY_REQUESTED + currentDay.format(DATE_FORMATTER));
            }

            recordRepository.save(vacationRequestRecord);
            createdRecordIds.add(vacationRequestRecord.timeRecordId());
            log.info("event=vacation_request_record_created date={} employeeRef={}",
                    currentDay.format(DATE_FORMATTER), privacyLogReferenceService.employeeRef(employeeId));
        }

        kronosMetrics.vacationRequested();
        return createdRecordIds; // Retorna os IDs criados para referência
    }

    @Override
    public void approveVacation(VacationApprovalRequest request) {
        if (!jwtAuthenticatedUser.hasAnyRole(Role.MANAGER, Role.CTO)) {
            throw new ForbiddenException(ONLY_MANAGERS_CAN_GRANT_VACATION);
        }

        // Aprova (muda o status) todos os registros na lista
        for (var recordId : request.timeRecordIds()) {
            var record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + recordId));
            domainAuthorizationService.authorizeEmployeeAccess(record.employeeId());

            if (record.statusRecord() == REQUEST_VACATION) {
                var approvedRecord = record.withStatus(VACATION); // 4. Manager aprova -> VACATION
                recordRepository.save(approvedRecord);
                log.info("Solicitação de férias (ID: {}) APROVADA. Status mudou para VACATION.", recordId);
            } else {
                // Ignore ou lance exceção se tentar aprovar algo que não está em REQUEST_VACATION
                log.warn("Tentativa de aprovar registro de férias (ID: {}) com status inválido: {}", recordId, record.statusRecord());
            }
        }
        kronosMetrics.vacationApproved();
    }

    @Override
    public void rejectVacation(VacationApprovalRequest request) {
        if (!jwtAuthenticatedUser.hasAnyRole(Role.MANAGER, Role.CTO)) {
            throw new ForbiddenException(ONLY_MANAGERS_CAN_REJECT_VACATION);
        }

        // Rejeita (muda o status) todos os registros na lista
        for (var recordId : request.timeRecordIds()) {
            var record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + recordId));
            domainAuthorizationService.authorizeEmployeeAccess(record.employeeId());

            if (record.statusRecord() == REQUEST_VACATION) {
                var rejectedRecord = record.withStatus(VACATION_REJECTED); // 4. Manager rejeita -> VACATION_REJECTED
                recordRepository.save(rejectedRecord);
                log.info("Solicitação de férias (ID: {}) REJEITADA. Status mudou para VACATION_REJECTED.", recordId);
            } else {
                log.warn("Tentativa de rejeitar registro de férias (ID: {}) com status inválido: {}", recordId, record.statusRecord());
            }
        }
        kronosMetrics.vacationRejected();
    }

    @Override
    public List<VacationRequestResponse> listVacationRequests(String statusFilter, String employeeName, int page, int size) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var companyId = getEmployee(employeeId).companyId();
        var normalizedStatusFilter = statusFilter == null ? "" : statusFilter.trim().toUpperCase();

        Set<String> targetStatuses = switch (normalizedStatusFilter) {
            case PENDING_STATUS -> Set.of(REQUEST_VACATION.name());
            case APPROVED_STATUS -> Set.of(VACATION.name());
            case REJECTED_STATUS -> Set.of(VACATION_REJECTED.name());
            default -> EnumSet.of(REQUEST_VACATION, VACATION, VACATION_REJECTED).stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
        };

        var pageable = PageRequest.of(page, size);
        Page<VacationRequestPeriodProjection> periodsPage =
                recordRepository.findVacationRequestPeriodsByCompanyId(pageable, companyId, targetStatuses, employeeName);

        return periodsPage.getContent().stream()
                .map(period -> new VacationRequestResponse(
                        period.getEmployeeId(),
                        period.getEmployeeName(),
                        period.getStartDate(),
                        period.getEndDate(),
                        period.getStatus(),
                        parseTimeRecordIdsCsv(period.getTimeRecordIdsCsv())
                ))
                .toList();
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
        getManagerApprover(request.managerId(), employee.companyId(), USER_NOT_IS_MANAGER, false);

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
        String documentChecksumSha256 = null;
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

            var savedRecord = recordRepository.save(dailyTimeOffRecord);
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
                        documentChecksumSha256 = uploadedDoc.checksumSha256();
                        // --------------------------------------------------

                    } catch (IOException e) {
                        log.warn("event=time_record_time_off_document result=failure reason=document_read exception_type={}",
                                e.getClass().getSimpleName());
                        throw new BadRequestException(NOT_ABLE_TO_READ_FILE);
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
                            savedRecord.timeRecordId(),
                            false,
                            false,
                            documentChecksumSha256
                    );
                    documentProvider.save(docToLink);
                }
            }
        }

        if (firstRecordId == null) {
            throw new BadRequestException(FAILED_TO_CREATE_FIRST_RECORD);
        }
        kronosMetrics.timeOffRequested();
        return firstRecordId;
    }

    @Override
    public void approveTimeOff(Long timeRecordId) {
        var record = getTimeRecord(timeRecordId);
        domainAuthorizationService.authorizeEmployeeAccess(record.employeeId());

        if (record.statusRecord() == StatusRecord.TIME_OFF_REQUEST) {
            var approvedRecord = record.withStatus(StatusRecord.TIME_OFF);
            recordRepository.save(approvedRecord);
            log.info("Abono aprovado para registro {}", timeRecordId);
        } else if (record.statusRecord() == StatusRecord.WORK_TIME_REQUEST) {
            var approvedRecord = record.withStatus(StatusRecord.UPDATED);
            recordRepository.save(approvedRecord);
            log.info("Esquecimento aprovado (convertido em trabalho) para registro {}", timeRecordId);
        } else {
            throw new BadRequestException(INVALID_RECORD + record.statusRecord() + ").");
        }
        kronosMetrics.timeOffApproved();
    }

    @Override
    public void rejectTimeOff(Long timeRecordId) {
        var record = getTimeRecord(timeRecordId);
        domainAuthorizationService.authorizeEmployeeAccess(record.employeeId());


        if (record.statusRecord() == StatusRecord.TIME_OFF_REQUEST) {
            var rejectedRecord = record.withStatus(StatusRecord.TIME_OFF_REJECTED);
            recordRepository.save(rejectedRecord);
            log.info("Abono negada para registro {}", timeRecordId);
        } else if (record.statusRecord() == StatusRecord.WORK_TIME_REQUEST) {
            var rejectedRecord = record.withStatus(StatusRecord.WORK_TIME_REJECTED);
            recordRepository.save(rejectedRecord);
            log.info("Alteração para esquecimento negado para registro {}", timeRecordId);
        } else {
            throw new BadRequestException(INVALID_RECORD + record.statusRecord() + ").");
        }
        kronosMetrics.timeOffRejected();
    }

    @Override
    public TimeRecordPageResponse listTimeOffRequests(String statusFilter, String employeeName, int page, int size) {
        var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var companyId = getEmployee(managerEmployeeId).companyId();
        var normalizedStatusFilter = statusFilter == null ? "" : statusFilter.trim().toUpperCase();

        Set<StatusRecord> targetStatuses = switch (normalizedStatusFilter) {
            case PENDING_STATUS -> Set.of(StatusRecord.TIME_OFF_REQUEST, StatusRecord.WORK_TIME_REQUEST);
            case APPROVED_STATUS -> Set.of(StatusRecord.TIME_OFF, StatusRecord.UPDATED);
            case REJECTED_STATUS -> Set.of(StatusRecord.TIME_OFF_REJECTED, StatusRecord.WORK_TIME_REJECTED);
            default -> EnumSet.of(
                    StatusRecord.TIME_OFF_REQUEST,
                    StatusRecord.WORK_TIME_REQUEST,
                    StatusRecord.TIME_OFF,
                    StatusRecord.UPDATED,
                    StatusRecord.TIME_OFF_REJECTED,
                    StatusRecord.WORK_TIME_REJECTED
            );
        };

        var pageable = PageRequest.of(page, size);
        Page<TimeRecord> recordsPage = recordRepository.findTimeOffRequestsByCompanyId(
                pageable,
                companyId,
                targetStatuses,
                employeeName
        );

        Map<UUID, Employee> employeesById = employeeProvider.findAllByIds(
                        recordsPage.getContent().stream()
                                .map(TimeRecord::employeeId)
                                .collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Employee::employeeId, employee -> employee));

        Map<Long, String> latestDocumentIdByTimeRecordId = buildLatestDocumentIdMap(
                recordsPage.getContent().stream()
                        .map(TimeRecord::timeRecordId)
                        .collect(Collectors.toSet())
        );

        var reference = Duration.ofHours(8);
        var companyName = companyUseCase.getCompanyNameById(companyId);

        List<TimeRecordResponse> pageContent = recordsPage.getContent().stream()
                .map(tr -> {
                    var emp = employeesById.get(tr.employeeId());
                    if (emp == null) {
                        return null;
                    }
                    var recordEmployeeData = new EmployeeData(emp.fullName(), companyName);

                    String documentPath = latestDocumentIdByTimeRecordId.get(tr.timeRecordId());

                    return TimeRecordResponse.fromDomain(tr, reference, recordEmployeeData, documentPath, null);
                })
                .filter(Objects::nonNull)
                .toList();

        return new TimeRecordPageResponse(
                pageContent,
                recordsPage.getTotalPages(),
                recordsPage.getTotalElements(),
                recordsPage.getNumber(),
                recordsPage.isFirst(),
                recordsPage.isLast()
        );
    }

    @Override
    public TodayTimeRecordStatusResponse getTodayStatus() {
        LocalDate today = LocalDate.now(SAO_PAULO);
        var employeeOpt = getAuthenticatedEmployeeIfPresent();

        if (employeeOpt.isEmpty()) {
            return neutralTodayStatus(today);
        }

        var employee = employeeOpt.get();

        if (!legalConsentProvider.existsActive(employee.employeeId(), ConsentType.BIOMETRIC_AUTHENTICATION)) {
            return new TodayTimeRecordStatusResponse(
                    today,
                    "TERMS_REQUIRED",
                    "ACCEPT_TERMS",
                    null,
                    null,
                    List.of(),
                    "PERSISTED",
                    SAO_PAULO.getId()
            );
        }

        var records = recordRepository.findByRange(
                employee.employeeId(),
                today.atStartOfDay(),
                today.atTime(23, 59, 59)
        ).stream()
                .sorted(Comparator.comparing(TimeRecord::startWork))
                .toList();

        var recordEvents = buildTodayRecordEvents(records);

        if (recordEvents.isEmpty()) {
            return new TodayTimeRecordStatusResponse(
                    today,
                    "READY_TO_CHECKIN",
                    "CHECK_IN",
                    null,
                    null,
                    List.of(),
                    "PERSISTED",
                    SAO_PAULO.getId()
            );
        }

        var lastEvent = recordEvents.getLast();
        var latestRecord = records.getLast();
        boolean hasCheckout = latestRecord.endWork() != null;

        return new TodayTimeRecordStatusResponse(
                today,
                hasCheckout ? "COMPLETED" : "READY_TO_CHECKOUT",
                hasCheckout ? "VIEW_REPORT" : "CHECK_OUT",
                lastEvent.recordedAt(),
                lastEvent.actionType(),
                recordEvents,
                "PERSISTED",
                SAO_PAULO.getId()
        );
    }

    @Override
    public RecentTimeRecordsResponse listMyRecentRecords(int limit) {
        var employeeOpt = getAuthenticatedEmployeeIfPresent();
        if (employeeOpt.isEmpty()) {
            return new RecentTimeRecordsResponse(List.of(), "PERSISTED");
        }

        int normalizedLimit = normalizeLimit(limit);
        var employee = employeeOpt.get();
        var companyName = companyProvider.findById(employee.companyId())
                .map(Company::name)
                .orElse("Empresa principal");
        var recentRecords = recordRepository.findRecentByEmployeeId(employee.employeeId(), normalizedLimit);
        Map<Long, String> latestDocumentIdByTimeRecordId = buildLatestDocumentIdMap(
                recentRecords.stream()
                        .map(TimeRecord::timeRecordId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );

        var items = recentRecords.stream()
                .flatMap(record -> buildRecentRecordEvents(record, companyName, latestDocumentIdByTimeRecordId).stream())
                .sorted(Comparator.comparing(RecentTimeRecordItemResponse::dateTime).reversed())
                .limit(normalizedLimit)
                .toList();

        return new RecentTimeRecordsResponse(items, "PERSISTED");
    }

    @Override
    public MyRequestsResponse listMyRequests(int limit) {
        var employeeOpt = getAuthenticatedEmployeeIfPresent();
        if (employeeOpt.isEmpty()) {
            return new MyRequestsResponse(List.of(), "PERSISTED");
        }

        int normalizedLimit = normalizeLimit(limit);
        var employeeId = employeeOpt.get().employeeId();
        var allRecords = recordRepository.findByEmployeeId(employeeId);

        var requests = new ArrayList<MyRequestItemResponse>();
        requests.addAll(buildVacationRequestItems(allRecords));
        requests.addAll(buildTimeOffRequestItems(allRecords));
        requests.addAll(buildManualAdjustmentItems(approvalProvider.findByRequestingEmployeeId(employeeId, normalizedLimit)));

        var items = requests.stream()
                .sorted(Comparator.comparing(MyRequestItemResponse::createdAt).reversed())
                .limit(normalizedLimit)
                .toList();

        return new MyRequestsResponse(items, "PERSISTED");
    }

    private List<Long> parseTimeRecordIdsCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .toList();
    }

    private Map<Long, String> buildLatestDocumentIdMap(Collection<Long> timeRecordIds) {
        if (timeRecordIds == null || timeRecordIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Document> latestDocumentsByTimeRecordId = documentProvider.findByTimeRecordIds(timeRecordIds).stream()
                .filter(doc -> doc.timeRecordId() != null)
                .collect(Collectors.toMap(
                        Document::timeRecordId,
                        doc -> doc,
                        (current, candidate) -> {
                            if (current.uploadeAt() == null) {
                                return candidate;
                            }
                            if (candidate.uploadeAt() == null) {
                                return current;
                            }
                            return current.uploadeAt().isAfter(candidate.uploadeAt()) ? current : candidate;
                        }
                ));

        return latestDocumentsByTimeRecordId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().documentId().toString()
                ));
    }

    private TodayTimeRecordStatusResponse neutralTodayStatus(LocalDate today) {
        return new TodayTimeRecordStatusResponse(
                today,
                "UNKNOWN",
                "NONE",
                null,
                null,
                List.of(),
                "PERSISTED",
                SAO_PAULO.getId()
        );
    }

    private Optional<Employee> getAuthenticatedEmployeeIfPresent() {
        try {
            return employeeProvider.findById(jwtAuthenticatedUser.getEmployeeId());
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    private List<TodayTimeRecordItemResponse> buildTodayRecordEvents(List<TimeRecord> records) {
        var items = new ArrayList<TodayTimeRecordItemResponse>();

        for (var record : records) {
            if (record.timeRecordId() == null || record.startWork() == null) {
                continue;
            }

            items.add(new TodayTimeRecordItemResponse(
                    record.timeRecordId(),
                    "CHECK_IN",
                    toOffsetDateTime(record.startWork()),
                    record.statusRecord().name(),
                    "BIOMETRIC"
            ));

            if (record.endWork() != null) {
                items.add(new TodayTimeRecordItemResponse(
                        record.timeRecordId(),
                        "CHECK_OUT",
                        toOffsetDateTime(record.endWork()),
                        record.statusRecord().name(),
                        "BIOMETRIC"
                ));
            }
        }

        return items.stream()
                .sorted(Comparator.comparing(TodayTimeRecordItemResponse::recordedAt))
                .toList();
    }

    private List<RecentTimeRecordItemResponse> buildRecentRecordEvents(
            TimeRecord record,
            String companyName,
            Map<Long, String> latestDocumentIdByTimeRecordId
    ) {
        if (record.timeRecordId() == null || record.startWork() == null) {
            return List.of();
        }

        String documentId = latestDocumentIdByTimeRecordId.get(record.timeRecordId());
        String receiptUrl = documentId == null ? null : "/documents/" + documentId;
        boolean hasReceipt = receiptUrl != null;
        var items = new ArrayList<RecentTimeRecordItemResponse>();

        items.add(new RecentTimeRecordItemResponse(
                record.timeRecordId(),
                "CHECK_IN",
                toOffsetDateTime(record.startWork()),
                "REGISTERED",
                "BIOMETRIC",
                companyName,
                hasReceipt,
                receiptUrl
        ));

        if (record.endWork() != null) {
            items.add(new RecentTimeRecordItemResponse(
                    record.timeRecordId(),
                    "CHECK_OUT",
                    toOffsetDateTime(record.endWork()),
                    "REGISTERED",
                    "BIOMETRIC",
                    companyName,
                    hasReceipt,
                    receiptUrl
            ));
        }

        return items;
    }

    private List<MyRequestItemResponse> buildVacationRequestItems(List<TimeRecord> allRecords) {
        var vacationStatuses = Set.of(StatusRecord.REQUEST_VACATION, StatusRecord.VACATION, StatusRecord.VACATION_REJECTED);
        var records = allRecords.stream()
                .filter(record -> vacationStatuses.contains(record.statusRecord()))
                .filter(record -> record.startWork() != null)
                .sorted(Comparator.comparing(TimeRecord::startWork))
                .toList();

        if (records.isEmpty()) {
            return List.of();
        }

        var groups = new ArrayList<List<TimeRecord>>();
        var currentGroup = new ArrayList<TimeRecord>();

        for (var record : records) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(record);
                continue;
            }

            var previous = currentGroup.getLast();
            boolean sameStatus = previous.statusRecord() == record.statusRecord();
            boolean contiguous = !previous.startWork().toLocalDate().plusDays(1).isBefore(record.startWork().toLocalDate())
                    && !record.startWork().toLocalDate().isBefore(previous.startWork().toLocalDate());

            if (sameStatus && contiguous) {
                currentGroup.add(record);
            } else {
                groups.add(List.copyOf(currentGroup));
                currentGroup = new ArrayList<>();
                currentGroup.add(record);
            }
        }

        if (!currentGroup.isEmpty()) {
            groups.add(List.copyOf(currentGroup));
        }

        return groups.stream()
                .map(group -> {
                    var first = group.getFirst();
                    var last = group.getLast();
                    String status = switch (first.statusRecord()) {
                        case REQUEST_VACATION -> "PENDING";
                        case VACATION -> "APPROVED";
                        case VACATION_REJECTED -> "REJECTED";
                        default -> "UNKNOWN";
                    };
                    String description = first.startWork().toLocalDate() + " a " + last.startWork().toLocalDate();
                    return new MyRequestItemResponse(
                            "vacation-" + first.timeRecordId(),
                            "VACATION",
                            "Solicitação de férias",
                            toOffsetDateTime(first.startWork()),
                            status,
                            description
                    );
                })
                .toList();
    }

    private List<MyRequestItemResponse> buildTimeOffRequestItems(List<TimeRecord> allRecords) {
        var timeOffStatuses = Set.of(
                StatusRecord.TIME_OFF_REQUEST,
                StatusRecord.TIME_OFF,
                StatusRecord.TIME_OFF_REJECTED,
                StatusRecord.WORK_TIME_REQUEST,
                StatusRecord.WORK_TIME_REJECTED,
                StatusRecord.UPDATED
        );

        return allRecords.stream()
                .filter(record -> timeOffStatuses.contains(record.statusRecord()))
                .filter(record -> record.startWork() != null)
                .map(record -> {
                    boolean manualAdjustment = record.statusRecord() == StatusRecord.WORK_TIME_REQUEST
                            || record.statusRecord() == StatusRecord.WORK_TIME_REJECTED
                            || record.statusRecord() == StatusRecord.UPDATED;
                    String status = switch (record.statusRecord()) {
                        case TIME_OFF_REQUEST, WORK_TIME_REQUEST -> "PENDING";
                        case TIME_OFF, UPDATED -> "APPROVED";
                        case TIME_OFF_REJECTED, WORK_TIME_REJECTED -> "REJECTED";
                        default -> "UNKNOWN";
                    };
                    String title = manualAdjustment ? "Solicitação de ajuste manual" : "Solicitação de abono";
                    String description = record.startWork().toLocalDate()
                            + (record.endWork() != null && !record.endWork().toLocalDate().equals(record.startWork().toLocalDate())
                            ? " a " + record.endWork().toLocalDate()
                            : "");
                    return new MyRequestItemResponse(
                            (manualAdjustment ? "manual-adjustment-" : "time-off-") + record.timeRecordId(),
                            manualAdjustment ? "MANUAL_ADJUSTMENT" : "TIME_OFF",
                            title,
                            toOffsetDateTime(record.startWork()),
                            status,
                            description
                    );
                })
                .toList();
    }

    private List<MyRequestItemResponse> buildManualAdjustmentItems(List<TimeRecordApprovalRequest> approvals) {
        return approvals.stream()
                .map(approval -> new MyRequestItemResponse(
                        "manual-adjustment-approval-" + approval.timeRecordId(),
                        "MANUAL_ADJUSTMENT",
                        "Solicitação de ajuste manual",
                        toOffsetDateTime(approval.createdAt()),
                        "IN_REVIEW",
                        approval.newStartWork().toLocalDate() + " · "
                                + approval.newStartWork().toLocalTime().format(TIME_FORMATTER)
                                + " até "
                                + approval.newEndWork().toLocalTime().format(TIME_FORMATTER)
                ))
                .toList();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value.atZone(SAO_PAULO).toOffsetDateTime();
    }

    private TimeRecord findRecordAndCheckStatus(Long timeRecordId) {
        var record = recordRepository.findById(timeRecordId).orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + timeRecordId));
        domainAuthorizationService.authorizeEmployeeAccess(record.employeeId());

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
        try {
            String[] parts = reference.split(":");
            if (parts.length != 2) {
                throw new BadRequestException(INVALID_FORMAT);
            }
            return Duration.ofHours(Long.parseLong(parts[0])).plusMinutes(Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            throw new BadRequestException(INVALID_FORMAT);
        }
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
        return recordRepository.findById(timeRecordId).orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + timeRecordId));
    }

    private TimeRecord getRecord(UUID employeeId, Long timeRecordId) {
        var employee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);
        var record = getTimeRecord(timeRecordId);

        isRecordBelongsEmployee(employee.employeeId(), record);
        return record;
    }

    private User getManagerApprover(UUID managerUserId, UUID companyId, String invalidRoleMessage, boolean forbiddenWhenInvalidRole) {
        var managerUser = userProvider.findById(managerUserId)
                .orElseThrow(() -> new ResourceNotFoundException(MANAGER_NOT_FOUND));

        if (managerUser.role() != Role.MANAGER) {
            if (forbiddenWhenInvalidRole) {
                throw new ForbiddenException(invalidRoleMessage);
            }
            throw new BadRequestException(invalidRoleMessage);
        }

        try {
            domainAuthorizationService.requireEmployeeFromCompany(
                    managerUser.employeeId(),
                    companyId,
                    EMPLOYEE_NOT_FOUND,
                    MANAGER_DIFFERENT_COMPANY
            );
        } catch (ForbiddenException e) {
            throw new BadRequestException(MANAGER_DIFFERENT_COMPANY);
        }

        return managerUser;
    }

    private void checkGeolocation(UUID employeeId, double requestLatitude, double requestLongitude) {
        kronosTracing.observe("kronos.time_record.validate_geolocation", () -> {
            var employee = getEmployee(employeeId);

            var company = companyProvider.findById(employee.companyId())
                    .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND_FOR_THE_EMPLOYEE));

            final double allowedDistanceMeters = 80.0;
            var companyLocation = company.location();

            if (companyLocation == null) {
                throw new BadRequestException(ADDRESS_COMPANY_IS_NOT_REGISTERED);
            }

            double distance = calculateDistanceInMeters(
                    companyLocation.latitude(),
                    companyLocation.longitude(),
                    requestLatitude,
                    requestLongitude
            );

            if (distance > allowedDistanceMeters) {
                throw new BadRequestException(GEOLOCATION_OUT_OF_RANGE);
            }
        });
    }

    /**
     * Ajusta os registros de Pausa Implícita (IMPLICIT_BREAK) vizinhos ao registro de trabalho
     * sendo atualizado.
     * * Esta lógica DEVE ser chamada apenas na aplicação final da mudança (MANAGER/CTO ou Approve).
     */
    private void adjustAdjacentRecordsOnUpdate(UUID employeeId, TimeRecord recordToUpdate, LocalDateTime newStart, LocalDateTime newEnd) {
        // 1. Obter todos os registros (incluindo breaks) do dia, ordenados.
        LocalDate day = newStart.toLocalDate();
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.atTime(23, 59, 59);
        List<TimeRecord> allDayRecords = new ArrayList<>(
            recordRepository.findByRange(employeeId, dayStart, dayEnd).stream()
                .filter(tr -> tr.startWork() != null)
                .sorted(Comparator.comparing(TimeRecord::startWork))
                .toList()
        );

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
                    recordRepository.deleteTimeRecord(preceding);
                    log.info("Pausa {} consumida pela edição e deletada.", preceding.timeRecordId());
                } else {
                    // Caso contrário, ajusta o fim da pausa
                    TimeRecord updatedBreak = preceding.withCheckout(newEndBreak).withStatus(StatusRecord.IMPLICIT_BREAK);
                    recordRepository.save(updatedBreak);
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
                    recordRepository.deleteTimeRecord(succeeding);
                    log.info("Pausa {} consumida pela edição e deletada.", succeeding.timeRecordId());
                } else {
                    // Caso contrário, ajusta o início da pausa
                    TimeRecord updatedBreak = new TimeRecord(succeeding.timeRecordId(), newStartBreak, // Novo start
                            succeeding.endWork(), // Fim original
                            StatusRecord.IMPLICIT_BREAK, succeeding.edited(), succeeding.active(), succeeding.employeeId(), null, null, null, null, null, null, null, null);
                    recordRepository.save(updatedBreak);
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
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.atTime(23, 59, 59);
        Set<StatusRecord> nonBreakStatuses = EnumSet.complementOf(EnumSet.of(StatusRecord.IMPLICIT_BREAK, StatusRecord.DAY_OFF, StatusRecord.TIME_OFF, StatusRecord.ABSENCE));

        // 1. Buscar todos os registros de trabalho (non-breaks) do dia, exceto o que está sendo editado
        List<TimeRecord> workSegments = recordRepository.findByRange(employeeId, dayStart, dayEnd).stream()
            .filter(tr -> !tr.timeRecordId().equals(currentRecordId))
            .filter(tr -> nonBreakStatuses.contains(tr.statusRecord()))
            .filter(tr -> tr.endWork() != null)
            .sorted(Comparator.comparing(TimeRecord::startWork)).toList();

        for (TimeRecord segment : workSegments) {
            // Verifica se o novo registro começa antes do fim de outro segmento
            if (newStart.isBefore(segment.endWork()) && newEnd.isAfter(segment.startWork())) {
                throw new BadRequestException(NEW_REGISTER_OVERRIDES_AN_EXISTING_WORK_RECORD + segment.startWork().format(GENERATION_DATE_FMT) + " - " + segment.endWork().format(GENERATION_DATE_FMT) + ").");
            }
        }
    }

    private Map<Long, String> getLatestDocumentPathByTimeRecordId(List<TimeRecord> records) {
        var timeRecordIds = records.stream()
                .map(TimeRecord::timeRecordId)
                .filter(Objects::nonNull)
                .toList();

        if (timeRecordIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return documentProvider.findByTimeRecordIds(timeRecordIds).stream()
                .filter(doc -> doc.timeRecordId() != null)
                .collect(Collectors.groupingBy(
                        Document::timeRecordId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(
                                        Comparator.comparing(
                                                Document::uploadeAt,
                                                Comparator.nullsLast(Comparator.naturalOrder())
                                        )
                                ),
                                opt -> opt.map(doc -> doc.documentId().toString()).orElse(null)
                        )
                ));
    }

    private List<TimeRecord> getRecords(UUID employeeId, Boolean active) {
        // Encontra todos os registros (segmentos)
        List<TimeRecord> immutableRecords = active == null ? recordRepository.findByEmployeeId(employeeId) : recordRepository.findByEmployeeIdAndActive(employeeId, active);

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
            kronosTracing.observe("kronos.time_record.validate_face", () -> {
                byte[] imageBytes = Base64.getDecoder().decode(faceImageBase64);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
                UUID recognizedEmployeeId = faceRecognitionProvider.searchFaceByImage(inputStream);

                if (recognizedEmployeeId == null) {
                    throw new BadRequestException(FACE_NOT_RECOGNIZED);
                }

                if (!expectedEmployeeId.equals(recognizedEmployeeId)) {
                    throw new BadRequestException(FACE_MISMATCH);
                }
            });
            log.info("event=time_record_face_validation result=success");
        } catch (IllegalArgumentException e) {
            log.warn("event=time_record_face_validation result=failure reason=invalid_image");
            throw new BadRequestException(INVALID_BASE64_IMAGE);
        } catch (BadRequestException e) {
            log.warn("event=time_record_face_validation result=failure reason=face");
            throw e;
        } catch (RuntimeException e) {
            log.error("event=time_record_face_validation result=failure reason=provider exception_type={}",
                    e.getClass().getSimpleName());
            throw new BadRequestException(INVALID_BASE64_IMAGE);
        }
    }

    private void isHomeOffice(GeolocationRequest request, Employee employee, UUID employeeId) {
        if (!employee.homeOffice()) {
            checkGeolocation(employeeId, request.latitude(), request.longitude());
        } else {
            log.info("event=time_record_geolocation result=skipped reason=home_office");
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

        } catch (RuntimeException e) {
            log.error("event=time_record_receipt result=failure reason=receipt_generation exception_type={}",
                    e.getClass().getSimpleName());
        }
    }

    private String resolveTimeRecordFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (INTERNAL_CLOCK_OUT_OF_SYNC.equals(message)) {
            return "ntp";
        }
        if (INVALID_BASE64_IMAGE.equals(message) || FACE_NOT_RECOGNIZED.equals(message) || FACE_MISMATCH.equals(message)) {
            return "face";
        }
        if (GEOLOCATION_OUT_OF_RANGE.equals(message) || ADDRESS_COMPANY_IS_NOT_REGISTERED.equals(message)) {
            return "geolocation";
        }
        if (message != null && message.startsWith(STATUS_CHECKOUT)) {
            return "status";
        }
        return "unknown";
    }
}
