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
import com.kts.kronos.application.security.DomainAuthorizationService;
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

    @Override
    public ActionResponse registerTime(GeolocationRequest request) {

        // 0. BLINDAGEM CONTRA FRAUDE DE RELÓGIO (NTP)
        ntpTimeService.validateSystemTime(10); //

        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);

        // 1. Validações Prévias (Biometria e Geolocalização)
        validateFaceRecognition(employeeId, request.faceImageBase64());
        isHomeOffice(request, employee, employeeId);

        // 2. Preparação de Dados
        var openRecordOpt = recordRepository.findOpenByEmployeeId(employee.employeeId());
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

            // Só permite checkout se o registro aberto for do MESMO DIA
            if (openRecordDate.isEqual(todayDate)) {
                if (open.statusRecord() != PENDING) {
                    throw new BadRequestException(STATUS_CHECKOUT + open.statusRecord() + ")");
                }

                // A. GERA NSR ATÔMICO (Sequencial Fiscal Único para Saída)
                var nsrCheckout = nsrProvider.generateNextNsr(employee.companyId());

                // B. Cria o registro atualizado (Fechamento)
                var updated = new TimeRecord(
                        open.timeRecordId(),
                        open.startWork(),
                        currentTime, // endWork
                        open.statusRecord().onCheckout(),
                        open.edited(),
                        open.active(),
                        open.employeeId(),
                        open.latitude(),
                        open.longitude(),
                        request.latitude(),  // endLatitude
                        request.longitude(), // endLongitude
                        open.nsrCheckin(),   // Mantém NSR da Entrada
                        nsrCheckout,         // Novo NSR da Saída
                        open.originalStartWork(),
                        currentTime          // Define o original da saída
                );

                recordRepository.save(updated);

                // C. Auditoria Fiscal (AFD) - Grava linha tipo 7
                adfUseCase.logMarking(company, employee, currentTime, nsrCheckout);

                // D. Comprovante (PDF) - Gera e salva no S3
                generateAndSaveReceipt(employee, updated.timeRecordId(), currentTime, nsrCheckout, EXIT);

                log.info("Checkout realizado com sucesso. NSR: {}", nsrCheckout);
                return new ActionResponse("Saída às " + currentTimeParsed + "! (NSR: " + nsrCheckout + ")", CHECKOUT);

            } else {
                log.info("Registro anterior (ID: {}) ignorado pois pertence a data passada.", open.timeRecordId());
                // Continua para criar um novo Check-in
            }
        }

        // ---------------------------------------------------------------------
        // CENÁRIO B: CHECKIN (Entrada) - Com lógica de suporte a DIA DE FOLGA
        // ---------------------------------------------------------------------

        // A. GERA NSR ATÔMICO (Sequencial Fiscal Único para Entrada)
        var nsrCheckin = nsrProvider.generateNextNsr(employee.companyId());
        var actionType = CHECKIN; // Default

        // >>> NOVA LÓGICA: Verifica se já existe um registro de FOLGA ou FALTA para hoje <<<
        // Isso permite que o funcionário trabalhe no dia que o sistema achava que era folga.
        // Necessário buscar qualquer registro do dia, independente de estar "open"
        var startOfDay = todayDate.atStartOfDay();
        var endOfDay = todayDate.atTime(23, 59, 59);

        // Estamos usando o método findByEmployeeIdAndStartWorkBetween que retorna uma lista.
        // Pegamos o primeiro se existir.
        List<TimeRecord> recordsToday = recordRepository.findByRange(
                employee.employeeId(), startOfDay, endOfDay); //

        Optional<TimeRecord> dayOffOrAbsenceRecord = recordsToday.stream()
                .filter(r -> r.statusRecord() == StatusRecord.DAY_OFF || r.statusRecord() == StatusRecord.ABSENCE)
                .findFirst();

        TimeRecord recordToSave;

        if (dayOffOrAbsenceRecord.isPresent()) {
            // CENÁRIO: Transformar FOLGA/FALTA em TRABALHO
            var existing = dayOffOrAbsenceRecord.get();
            log.info("Convertendo registro {} (Status: {}) para PENDING (Trabalho) devido a Check-in manual.",
                    existing.timeRecordId(), existing.statusRecord());

            // Reaproveita o ID e atualiza os dados para um check-in válido
            recordToSave = new TimeRecord(
                    existing.timeRecordId(), // Mantém o ID
                    currentTime,             // Novo StartWork (agora)
                    null,                    // EndWork nulo (está trabalhando)
                    PENDING,    // Novo Status
                    false,                   // Não é editado (é um registro original de ponto)
                    true,
                    employee.employeeId(),
                    request.latitude(),
                    request.longitude(),
                    null, null,
                    nsrCheckin,              // Atribui o NSR gerado
                    null,
                    currentTime,             // Original Start
                    null
            );

            actionType = CHECKIN_ON_DAY_OFF;

        } else {
            // CENÁRIO PADRÃO: Criar novo registro

            // Lógica de Pausa Implícita (Gap)
            var latestRecordOpt = recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employee.employeeId());

            if (latestRecordOpt.isPresent()) {
                var latest = latestRecordOpt.get();
                var latestEndWork = latest.endWork();
                var currentStartDay = currentTime.toLocalDate();
                var latestEndDay = latestEndWork != null ? latestEndWork.atZone(SAO_PAULO).toLocalDate() : null;

                // Se o último registro fechado foi HOJE, cria o registro de intervalo (gap)
                if (latestEndWork != null && currentStartDay.equals(latestEndDay)) {
                    var breakRecord = new TimeRecord(
                            null,
                            latestEndWork, // Início da Pausa
                            currentTime,   // Fim da Pausa
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
                    log.info("Pausa implícita registrada entre {} e {}", latestEndWork, currentTime);
                }
            }

            // Cria o objeto novo
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

        // C. Salva (Create ou Update)
        var savedRecord = recordRepository.save(recordToSave);

        // D. Auditoria Fiscal (AFD)
        adfUseCase.logMarking(company, employee, currentTime, nsrCheckin);

        // E. Comprovante (PDF)
        generateAndSaveReceipt(employee, savedRecord.timeRecordId(), currentTime, nsrCheckin, "ENTRADA");

        log.info("Checkin realizado com sucesso. NSR: {}", nsrCheckin);

        var message = switch (actionType) {
            case CHECKIN_AFTER_BREAK -> "Entrada após pausa às " + currentTimeParsed + "! (NSR: " + nsrCheckin + ")";
            case CHECKIN_ON_DAY_OFF ->
                    "Registro de folga convertido para trabalho às " + currentTimeParsed + "! (NSR: " + nsrCheckin + ")";
            default -> "Entrada às " + currentTimeParsed + "! (NSR: " + nsrCheckin + ")";
        };

        return new ActionResponse(message, actionType);
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
            var managerUser = userProvider.findById(req.managerId()).orElseThrow(() -> new ResourceNotFoundException(MANAGER_NOT_FOUND));

            if (managerUser.role() != Role.MANAGER) {
                throw new BadRequestException(USER_IS_NOT_MANAGER);
            }

            var managerEmployee = employeeProvider.findById(managerUser.employeeId()).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

            if (!managerEmployee.companyId().equals(employee.companyId())) {
                throw new BadRequestException(MANAGER_DIFFERENT_COMPANY);
            }

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
    public SimpleReportResponse simpleReport(UUID employeeId, SimpleReportRequest req) {
        var targetEmployeeId = domainAuthorizationService.authorizeEmployeeAccess(employeeId).employeeId();
        var employeeData = getEmployeeData(targetEmployeeId);

        String[] parts = req.reference().split(":");

        var reference = Duration.ofHours(Long.parseLong(parts[0])).plusMinutes(Long.parseLong(parts[1]));

        // Statuses de trabalho
        var workStatuses = Set.of(CREATED, UPDATED, PENDING_APPROVAL, PENDING);
        var specialStatuses = Set.of(DAY_OFF, TIME_OFF, ABSENCE, REQUEST_VACATION, VACATION, VACATION_REJECTED);
        var allRecordsStatuses = new HashSet<>(workStatuses);
        allRecordsStatuses.addAll(specialStatuses);
        allRecordsStatuses.add(IMPLICIT_BREAK); // Inclui o novo status de pausa

        // 1. Busca todos os registros ativos e filtra pelas datas
        var allRecords = recordRepository.findByEmployeeIdAndActive(targetEmployeeId, true).stream().filter(tr -> tr.startWork() != null) // Deve ter startWork para ser válido
                .filter(tr -> allRecordsStatuses.contains(tr.statusRecord())).toList();

        final Set<LocalDate> finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        allRecords = allRecords.stream().filter(tr -> finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate())).toList();


        // 2. Agrupamento por dia (usando o Map para coletar todos os registros do dia)
        Map<LocalDate, List<TimeRecord>> recordsByDay = allRecords.stream().collect(Collectors.groupingBy(tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate(), TreeMap::new, Collectors.toCollection(ArrayList::new)));


        List<SimpleReportDay> days = new ArrayList<>();
        var totalWorkedDuration = Duration.ZERO;
        var totalBreakDuration = Duration.ZERO;
        var totalBalance = Duration.ZERO;


        for (var entry : recordsByDay.entrySet()) {
            var startDate = entry.getKey();
            List<TimeRecord> dailyRecords = entry.getValue();

            var firstStartWork = dailyRecords.getFirst().startWork();
            var firstStartHour = firstStartWork.atZone(SAO_PAULO).toLocalTime().format(TIME_FORMATTER);
            var lastEndWork = dailyRecords.stream().map(TimeRecord::endWork).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(startDate.atStartOfDay());

            var lastEndHour = "";
            // Se houver um segmento PENDING, a saída é indefinida (string vazia)
            if (dailyRecords.stream().noneMatch(tr -> tr.statusRecord() == PENDING)) { //
                lastEndHour = lastEndWork.atZone(SAO_PAULO).toLocalTime().format(TIME_FORMATTER);
            }
            // Ordena os registros pela hora de início (essencial para definir a última saída e primeiro trabalho)
            dailyRecords.sort(Comparator.comparing(TimeRecord::startWork, Comparator.nullsLast(Comparator.naturalOrder())));

            // 1. Soma Duração Total das Pausas (Filtra pelo novo status IMPLICIT_BREAK)
            var dailyBreakDuration = dailyRecords.stream().filter(tr -> tr.statusRecord() == StatusRecord.IMPLICIT_BREAK).filter(tr -> tr.endWork() != null).map(tr -> Duration.between(tr.startWork(), tr.endWork())).reduce(Duration.ZERO, Duration::plus);

            // 2. Calcula Duração de Trabalho Líquida (Soma do tempo de todos os segmentos de TRABALHO)
            var dailyWorkedLiquid = dailyRecords.stream().filter(tr -> workStatuses.contains(tr.statusRecord()) || specialStatuses.contains(tr.statusRecord())) // Apenas segmentos de trabalho (e abonos)
                    .filter(tr -> tr.endWork() != null) // Ignora segmentos de trabalho não finalizados (PENDING)
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork())).reduce(Duration.ZERO, Duration::plus);

            var endDate = lastEndWork.atZone(SAO_PAULO).toLocalDate();

            // 4. Determina Status e Balanço
            // O status do dia será o status do primeiro segmento de TRABALHO/TIME_OFF_REQUEST do dia
            var dailyStatus = dailyRecords.stream().filter(tr -> workStatuses.contains(tr.statusRecord()) || specialStatuses.contains(tr.statusRecord())).min(Comparator.comparing(TimeRecord::startWork)).map(TimeRecord::statusRecord).orElse(StatusRecord.DAY_OFF); // Default para DAY_OFF se não houver registros de trabalho/abono.

            var dailyBalance = Duration.ZERO;
            boolean isSpecialStatus = specialStatuses.contains(dailyStatus);

            if (!isSpecialStatus) {
                dailyBalance = dailyWorkedLiquid.minus(reference);

                // Se houver um segmento PENDING (ainda trabalhando)
                if (dailyRecords.stream().anyMatch(tr -> tr.statusRecord() == PENDING)) {
                    dailyBalance = Duration.ZERO;
                }
            }


            // Formatação
            var totalHours = String.format("%02d:%02d", dailyWorkedLiquid.toHours(), dailyWorkedLiquid.toMinutesPart());
            var totalBreak = String.format("%02d:%02d", dailyBreakDuration.toHours(), dailyBreakDuration.toMinutesPart());
            var sign = dailyBalance.isNegative() ? "-" : "+";
            var balance = sign + String.format("%02d:%02d", Math.abs(dailyBalance.toHours()), Math.abs(dailyBalance.toMinutesPart()));

            totalWorkedDuration = totalWorkedDuration.plus(dailyWorkedLiquid);
            totalBreakDuration = totalBreakDuration.plus(dailyBreakDuration);
            totalBalance = totalBalance.plus(dailyBalance);

            days.add(new SimpleReportDay(startDate, endDate, firstStartHour, // Novo argumento
                    lastEndHour, totalHours, totalBreak, balance));
        }

        var finalWorked = String.format("%02d:%02d", totalWorkedDuration.toHours(), totalWorkedDuration.toMinutesPart());
        var finalBreak = String.format("%02d:%02d", totalBreakDuration.toHours(), totalBreakDuration.toMinutesPart());
        var signAll = totalBalance.isNegative() ? "-" : "+";
        var finalBalance = signAll + String.format("%02d:%02d", Math.abs(totalBalance.toHours()), Math.abs(totalBalance.toMinutesPart()));

        return new SimpleReportResponse(employeeData.employeeName(), employeeData.companyName(), days, finalWorked, finalBreak, finalBalance);
    }

    @Override
    public List<TimeRecordResponse> listReport(UUID employeeId, ListReportRequest req) {
        var targetEmployeeId = domainAuthorizationService.authorizeEmployeeAccess(employeeId).employeeId();
        var employeeData = getEmployeeData(targetEmployeeId);
        var reference = getDuration(req.reference());

        // 1. Definição de datas
        final Set<LocalDate> finalDatesSet;
        if (req.dates() != null && req.dates().length > 0) {
            finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        } else {
            return Collections.emptyList();
        }

        // 2. Busca registros
        List<TimeRecord> allRecordsForEmployee = getRecords(targetEmployeeId, req.active());

        // 3. Definição de status válidos
        var allPossibleReportStatuses = EnumSet.of(
                CREATED, PENDING, UPDATED, UPDATE_REJECTED, DAY_OFF, ABSENCE,
                PENDING_APPROVAL, TIME_OFF, TIME_OFF_REQUEST, TIME_OFF_REJECTED,
                IMPLICIT_BREAK, REQUEST_VACATION, VACATION, VACATION_REJECTED,
                WORK_TIME_REQUEST,
                WORK_TIME_REJECTED
        );

        Set<StatusRecord> finalFilterStatuses;
        if (req.statuses() == null || req.statuses().isEmpty()) {
            finalFilterStatuses = allPossibleReportStatuses;
        } else {
            finalFilterStatuses = req.statuses().stream()
                    .filter(allPossibleReportStatuses::contains)
                    .collect(Collectors.toSet());
        }

        // 4. Filtra registros relevantes para as datas solicitadas
        List<TimeRecord> recordsInRange = allRecordsForEmployee.stream()
                .filter(tr -> tr.startWork() != null && finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate()))
                .filter(tr -> finalFilterStatuses.contains(tr.statusRecord()))
                .collect(Collectors.toCollection(ArrayList::new));

        // =================================================================================
        // LÓGICA DE CÁLCULO DE SALDO ÚNICO POR DIA
        // =================================================================================

        // Agrupa por dia
        Map<LocalDate, List<TimeRecord>> recordsByDay = recordsInRange.stream()
                .collect(Collectors.groupingBy(tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate()));

        // Mapa para armazenar o saldo calculado de cada dia
        Map<LocalDate, String> dailyBalanceMap = new HashMap<>();

        recordsByDay.forEach((date, dailyRecords) -> {
            // Verifica status especiais que anulam o cálculo (Faltas, Folgas, Férias)
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
                // Filtra apenas registros com horário de fim definido
                List<TimeRecord> closedRecords = dailyRecords.stream()
                        .filter(tr -> tr.endWork() != null)
                        .sorted(Comparator.comparing(TimeRecord::startWork))
                        .toList();

                if (closedRecords.isEmpty()) {
                    balanceStr = "+00:00"; // Ou lógica para dia sem registros fechados
                } else {
                    // 1. Primeira Hora e Última Hora
                    var firstStart = closedRecords.getFirst().startWork();
                    var lastEnd = closedRecords.getLast().endWork();

                    // 2. Duração Total Bruta (Primeira -> Última)
                    var grossDuration = Duration.between(firstStart, lastEnd);

                    // 3. Soma das Pausas (Registros de IMPLICIT_BREAK)
                    // Nota: Isso assume que a pausa está registrada como IMPLICIT_BREAK, conforme seu payload.
                    var totalBreakDuration = closedRecords.stream()
                            .filter(tr -> tr.statusRecord() == StatusRecord.IMPLICIT_BREAK)
                            .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                            .reduce(Duration.ZERO, Duration::plus);

                    // Alternativa: Se quiser somar APENAS o tempo trabalhado (CREATED/UPDATED),
                    // o resultado matemático é o mesmo que (Bruto - Pausas) se os intervalos forem contíguos.
                    // Duration netWorkDuration = grossDuration.minus(totalBreakDuration);

                    // Cálculo Robusto: Soma dos segmentos de TRABALHO efetivo (exclui pausas)
                    // Isso evita problemas se houver "buracos" não registrados como pausa.
                    var netWorkDuration = closedRecords.stream()
                            .filter(tr -> tr.statusRecord() != StatusRecord.IMPLICIT_BREAK)
                            .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                            .reduce(Duration.ZERO, Duration::plus);

                    // 4. Saldo = Líquido - Referência
                    var balance = netWorkDuration.minus(reference);

                    var sign = balance.isNegative() ? "-" : "+";
                    balanceStr = sign + String.format("%02d:%02d", Math.abs(balance.toHours()), Math.abs(balance.toMinutesPart()));
                }
            }
            dailyBalanceMap.put(date, balanceStr);
        });

        // =================================================================================

        // Monta a resposta final
        return recordsInRange.stream()
                .sorted(Comparator.comparing(TimeRecord::startWork))
                .map(tr -> {

                    // ALTERADO: Agora recebemos uma lista e pegamos o primeiro (ou o mais recente)
                    var docs = documentProvider.findByTimeRecordId(tr.timeRecordId());
                    String documentPath = null;

                    if (!docs.isEmpty()) {
                        // Pega o último documento enviado (ex: comprovante de saída é mais completo)
                        // ou simplesmente o primeiro da lista.
                        // Aqui ordenamos para pegar o mais recente se houver mais de um.
                        documentPath = docs.stream()
                                .max(Comparator.comparing(Document::uploadeAt)) // Pega o mais recente
                                .map(doc -> doc.documentId().toString())
                                .orElse(docs.getFirst().documentId().toString());
                    }

                    var date = tr.startWork().atZone(SAO_PAULO).toLocalDate();
                    var dailyBalance = dailyBalanceMap.getOrDefault(date, "+00:00");

                    return TimeRecordResponse.fromDomain(tr, reference, employeeData, documentPath, dailyBalance);
                })
                .collect(Collectors.toCollection(ArrayList::new));
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
            var timeRecord = recordRepository.findById(approvalData.timeRecordId()).orElse(null);
            var partnerEmployee = employeeProvider.findById(approvalData.requestingEmployeeId()).orElse(null);
            var managerUser = userProvider.findById(approvalData.managerId()).orElse(null);

            var docs = documentProvider.findByTimeRecordId(approvalData.timeRecordId());
            String documentPath = null;
            if (!docs.isEmpty()) {
                // Pega o ID do primeiro documento encontrado
                documentPath = "/documents/" + docs.get(0).documentId();
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
            if (recordRepository.existsByEmployeeIdAndDate(employeeId, currentDay)) {
                throw new BadRequestException(ALREADY_REQUESTED + currentDay.format(DATE_FORMATTER));
            }

            recordRepository.save(vacationRequestRecord);
            createdRecordIds.add(vacationRequestRecord.timeRecordId());
            log.info("Solicitação de férias (REQUEST_VACATION) criada para o dia {} para o funcionário {}", currentDay.format(DATE_FORMATTER), employeeId);
        }

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

            if (record.statusRecord() == REQUEST_VACATION) {
                var approvedRecord = record.withStatus(VACATION); // 4. Manager aprova -> VACATION
                recordRepository.save(approvedRecord);
                log.info("Solicitação de férias (ID: {}) APROVADA. Status mudou para VACATION.", recordId);
            } else {
                // Ignore ou lance exceção se tentar aprovar algo que não está em REQUEST_VACATION
                log.warn("Tentativa de aprovar registro de férias (ID: {}) com status inválido: {}", recordId, record.statusRecord());
            }
        }
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

            if (record.statusRecord() == REQUEST_VACATION) {
                var rejectedRecord = record.withStatus(VACATION_REJECTED); // 4. Manager rejeita -> VACATION_REJECTED
                recordRepository.save(rejectedRecord);
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
                .flatMap(empId -> recordRepository.findByEmployeeId(empId).stream())
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
            recordRepository.save(approvedRecord);
            log.info("Abono aprovado para registro {}", timeRecordId);
        } else if (record.statusRecord() == StatusRecord.WORK_TIME_REQUEST) {
            var approvedRecord = record.withStatus(StatusRecord.UPDATED);
            recordRepository.save(approvedRecord);
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
            recordRepository.save(rejectedRecord);
            log.info("Abono negada para registro {}", timeRecordId);
        } else if (record.statusRecord() == StatusRecord.WORK_TIME_REQUEST) {
            var rejectedRecord = record.withStatus(StatusRecord.WORK_TIME_REJECTED);
            recordRepository.save(rejectedRecord);
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
                .flatMap(empId -> recordRepository.findByEmployeeId(empId).stream())
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
                    String documentPath = docs.isEmpty() ? null : docs.get(0).documentId().toString();
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

    private TimeRecord findRecordAndCheckStatus(Long timeRecordId) {
        var record = recordRepository.findById(timeRecordId).orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + timeRecordId));

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
        return recordRepository.findById(timeRecordId).orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + timeRecordId));
    }


    private TimeRecord getRecord(UUID employeeId, Long timeRecordId) {
        var employee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);
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

    /**
     * Ajusta os registros de Pausa Implícita (IMPLICIT_BREAK) vizinhos ao registro de trabalho
     * sendo atualizado.
     * * Esta lógica DEVE ser chamada apenas na aplicação final da mudança (MANAGER/CTO ou Approve).
     */
    private void adjustAdjacentRecordsOnUpdate(UUID employeeId, TimeRecord recordToUpdate, LocalDateTime newStart, LocalDateTime newEnd) {
        // 1. Obter todos os registros (incluindo breaks) do dia, ordenados.
        LocalDate day = newStart.toLocalDate();
        List<TimeRecord> allDayRecords = recordRepository.findByEmployeeId(employeeId).stream().filter(tr -> tr.startWork() != null && tr.startWork().toLocalDate().equals(day)).sorted(Comparator.comparing(TimeRecord::startWork)).collect(Collectors.toCollection(ArrayList::new));

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
        Set<StatusRecord> nonBreakStatuses = EnumSet.complementOf(EnumSet.of(StatusRecord.IMPLICIT_BREAK, StatusRecord.DAY_OFF, StatusRecord.TIME_OFF, StatusRecord.ABSENCE));

        // 1. Buscar todos os registros de trabalho (non-breaks) do dia, exceto o que está sendo editado
        List<TimeRecord> workSegments = recordRepository.findByEmployeeId(employeeId).stream().filter(tr -> !tr.timeRecordId().equals(currentRecordId)).filter(tr -> tr.startWork() != null && tr.startWork().toLocalDate().equals(day)).filter(tr -> nonBreakStatuses.contains(tr.statusRecord())).filter(tr -> tr.endWork() != null) // Só checa segmentos fechados
                .sorted(Comparator.comparing(TimeRecord::startWork)).toList();

        for (TimeRecord segment : workSegments) {
            // Verifica se o novo registro começa antes do fim de outro segmento
            if (newStart.isBefore(segment.endWork()) && newEnd.isAfter(segment.startWork())) {
                throw new BadRequestException(NEW_REGISTER_OVERRIDES_AN_EXISTING_WORK_RECORD + segment.startWork().format(GENERATION_DATE_FMT) + " - " + segment.endWork().format(GENERATION_DATE_FMT) + ").");
            }
        }
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

}
