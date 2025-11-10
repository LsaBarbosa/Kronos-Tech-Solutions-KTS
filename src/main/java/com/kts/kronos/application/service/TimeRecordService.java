package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationRequestResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.domain.model.enuns.StatusRecord.PENDING_APPROVAL;

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


    @Override
    public ActionResponse registerTime(GeolocationRequest request) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);
        isHomeOffice(request, employee, employeeId);

        var openRecordOpt = recordRepository.findOpenByEmployeeId(employee.employeeId());
        var currentTime = LocalDateTime.now(SAO_PAULO);
        var currentDateParsed = currentTime.format(DATE_FORMATTER);
        var currentTimeParsed = currentTime.format(TIME_FORMATTER);

        if (openRecordOpt.isPresent()) {
            // É um CHECKOUT (Finaliza o segmento de trabalho atual)
            var open = openRecordOpt.get();

            log.debug("Tentativa de Checkout. Registro ID: {}, Status Atual: {}", open.timeRecordId(), open.statusRecord());

            if (open.statusRecord() != PENDING) {
                log.error("Tentativa de Checkout falhou. Status do registro ID {} é: {} (Esperado: PENDING)", open.timeRecordId(), open.statusRecord());
                throw new BadRequestException(STATUS_CHECKOUT + open.statusRecord() + ")");
            }

            // Se for PENDING, realiza a transição e fecha o registro
            var updated = open.withCheckout(currentTime).withStatus(open.statusRecord().onCheckout());
            recordRepository.save(updated);
            log.info("Checkout registrado para o segmento de trabalho {}.", open.timeRecordId());

            return new ActionResponse("Saída às " + currentTimeParsed + "!", "CHECKOUT");
        } else {
            // É um CHECKIN (Inicia um novo segmento de trabalho)
            var latestRecordOpt = recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employee.employeeId());

            if (latestRecordOpt.isPresent()) {
                var latest = latestRecordOpt.get();
                var latestEndWork = latest.endWork();
                var currentStartDay = currentTime.atZone(SAO_PAULO).toLocalDate();
                var latestEndDay = latestEndWork != null ? latestEndWork.atZone(SAO_PAULO).toLocalDate() : null;

                // Verifica se o último registro foi *encerrado* no MESMO DIA.
                if (latestEndWork != null && currentStartDay.equals(latestEndDay)) {

                    // 1. CRIA O REGISTRO DE PAUSA IMPLÍCITA (agora explícita)
                    var breakRecord = new TimeRecord(null, // timeRecordId será gerado
                            latestEndWork, // Início da pausa é o fim do último trabalho
                            currentTime,   // Fim da pausa é o início do novo trabalho
                            StatusRecord.IMPLICIT_BREAK, // Novo status de pausa
                            false, true, employee.employeeId());
                    recordRepository.save(breakRecord);
                    log.info("Registro de Pausa Implícita criado entre {} e {}.", latestEndWork, currentTime);


                    // 2. CRIA O NOVO REGISTRO DE PONTO (Segmento de trabalho)
                    var record = new TimeRecord(null, currentTime, null, PENDING, false, true, employee.employeeId());
                    recordRepository.save(record);
                    log.info("Novo Checkin (após pausa) registrado para o funcionário {}.", employee.employeeId());
                    return new ActionResponse("Entrada após pausa às " + currentTimeParsed + "!", "CHECKIN_AFTER_BREAK");
                }
            }

            // 2. Se for o primeiro ponto do dia/primeiro ponto geral
            var record = new TimeRecord(null, currentTime, null, PENDING, false, true, employee.employeeId());
            recordRepository.save(record);
            log.info("Primeiro Checkin do dia registrado para o funcionário {}.", employee.employeeId());
            return new ActionResponse("Entrada às " + currentTimeParsed + "!", "CHECKIN");
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

    @Override
    public void updateTimeRecord(Long timeRecordId, UpdateTimeRecordRequest req) {
        var userRole = jwtAuthenticatedUser.getRoleFromToken();
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

        if ("PARTNER".equals(userRole)) {
            // Apenas valida sobreposição contra segmentos de trabalho adjacentes
            validateNonBreakOverlap(employeeId, record.timeRecordId(), newStart, newEnd);

            if (req.managerId() == null) {
                throw new BadRequestException("O ID do manager é obrigatório para parceiros.");
            }
            var managerUser = userProvider.findById(req.managerId()).orElseThrow(() -> new ResourceNotFoundException("Manager não encontrado."));

            if (managerUser.role() != Role.MANAGER) {
                throw new BadRequestException("O usuário informado não é um manager.");
            }

            var managerEmployee = employeeProvider.findById(managerUser.employeeId()).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

            if (!managerEmployee.companyId().equals(employee.companyId())) {
                throw new BadRequestException("O manager não pertence à mesma empresa.");
            }

            // 1. Cria o payload simplificado
            var approvalRequest = new TimeRecordApprovalRequest(timeRecordId, employeeId, req.managerId(), newStart, newEnd, TIME_ZONE_BRAZIL);

            // 2. Persiste a solicitação
            approvalProvider.save(approvalRequest);

            var updatedRecord = record.withStatus(PENDING_APPROVAL).withEdited(true);
            recordRepository.save(updatedRecord);

        } else if ("MANAGER".equals(userRole) || "CTO".equals(userRole)) {
            // Lógica para o MANAGER/CTO (aprovação direta)

            // NOVO: Executa o ajuste dos registros de Pausa vizinhos
            adjustAdjacentRecordsOnUpdate(employeeId, record, newStart, newEnd);

            var statusUpdate = record.statusRecord().onUpdate();
            var updated = record.withCheckin(newStart).withCheckout(newEnd).withEdited(true).withStatus(statusUpdate);
            recordRepository.save(updated);
        } else {
            throw new BadRequestException("Role não autorizada para esta operação.");
        }
    }

    @Override
    public void approveTimeRecordChange(Long timeRecordId) {
        var record = findRecordAndCheckStatus(timeRecordId);

        // Busca a solicitação
        TimeRecordApprovalRequest approvalData = approvalProvider.findByTimeRecordId(timeRecordId).orElseThrow(() -> new ResourceNotFoundException("Solicitação de aprovação não encontrada ou expirada para o registro: " + timeRecordId));

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
        var rejectedRecord = record.withStatus(StatusRecord.UPDATE_REJECTED).withEdited(false);
        recordRepository.save(rejectedRecord);

        // Limpa o registro de aprovação
        approvalProvider.deleteByTimeRecordId(timeRecordId);

        log.info("Solicitação para o registro {} foi REJEITADA.", timeRecordId);
    }

    @Override
    public void deleteTimeRecord(UUID employeeId, Long recordId) {
        var employee = getEmployee(employeeId);
        var record = getTimeRecord(recordId);

        isRecordBelongsEmployee(employee.employeeId(), record);

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
            throw new BadRequestException("O status do registro não pode ser alterado, pois está aguardando aprovação.");
        }
        if (currentStatus == StatusRecord.UPDATED) {
            throw new BadRequestException("O status do registro não pode ser alterado, pois o registro foi atualizado após uma solicitção.");
        }
        var updateStatus = record.withStatus(req.statusRecord());
        recordRepository.save(updateStatus);
    }

    @Override
    public SimpleReportResponse simpleReport(UUID employeeId, SimpleReportRequest req) {
        var targetEmployeeId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        var employeeData = getEmployeeData(targetEmployeeId);

        String[] parts = req.reference().split(":");

        var reference = Duration.ofHours(Long.parseLong(parts[0])).plusMinutes(Long.parseLong(parts[1]));

        // Statuses de trabalho
        var workStatuses = Set.of(CREATED, UPDATED, PENDING_APPROVAL, PENDING);
        var specialStatuses = Set.of(DAY_OFF, DOCTOR_APPOINTMENT, ABSENCE);
        var allRecordsStatuses = new HashSet<>(workStatuses);
        allRecordsStatuses.addAll(specialStatuses);
        allRecordsStatuses.add(StatusRecord.IMPLICIT_BREAK); // Inclui o novo status de pausa

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

            LocalDateTime firstStartWork = dailyRecords.getFirst().startWork();
            String firstStartHour = firstStartWork.atZone(SAO_PAULO).toLocalTime().format(TIME_FORMATTER);
            LocalDateTime lastEndWork = dailyRecords.stream().map(TimeRecord::endWork).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(startDate.atStartOfDay());

            String lastEndHour = "";
            // Se houver um segmento PENDING, a saída é indefinida (string vazia)
            if (dailyRecords.stream().noneMatch(tr -> tr.statusRecord() == PENDING)) { //
                lastEndHour = lastEndWork.atZone(SAO_PAULO).toLocalTime().format(TIME_FORMATTER);
            }
            // Ordena os registros pela hora de início (essencial para definir a última saída e primeiro trabalho)
            dailyRecords.sort(Comparator.comparing(TimeRecord::startWork, Comparator.nullsLast(Comparator.naturalOrder())));

            // 1. Soma Duração Total das Pausas (Filtra pelo novo status IMPLICIT_BREAK)
            Duration dailyBreakDuration = dailyRecords.stream().filter(tr -> tr.statusRecord() == StatusRecord.IMPLICIT_BREAK).filter(tr -> tr.endWork() != null).map(tr -> Duration.between(tr.startWork(), tr.endWork())).reduce(Duration.ZERO, Duration::plus);

            // 2. Calcula Duração de Trabalho Líquida (Soma do tempo de todos os segmentos de TRABALHO)
            Duration dailyWorkedLiquid = dailyRecords.stream().filter(tr -> workStatuses.contains(tr.statusRecord()) || specialStatuses.contains(tr.statusRecord())) // Apenas segmentos de trabalho (e abonos)
                    .filter(tr -> tr.endWork() != null) // Ignora segmentos de trabalho não finalizados (PENDING)
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork())).reduce(Duration.ZERO, Duration::plus);

            LocalDate endDate = lastEndWork.atZone(SAO_PAULO).toLocalDate();

            // 4. Determina Status e Balanço
            // O status do dia será o status do primeiro segmento de TRABALHO/ABONO do dia
            StatusRecord dailyStatus = dailyRecords.stream().filter(tr -> workStatuses.contains(tr.statusRecord()) || specialStatuses.contains(tr.statusRecord())).min(Comparator.comparing(TimeRecord::startWork)).map(TimeRecord::statusRecord).orElse(StatusRecord.DAY_OFF); // Default para DAY_OFF se não houver registros de trabalho/abono.

            Duration dailyBalance = Duration.ZERO;
            boolean isSpecialStatus = specialStatuses.contains(dailyStatus);

            if (!isSpecialStatus) {
                dailyBalance = dailyWorkedLiquid.minus(reference);

                // Se houver um segmento PENDING (ainda trabalhando)
                if (dailyRecords.stream().anyMatch(tr -> tr.statusRecord() == PENDING)) {
                    dailyStatus = PENDING;
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
        var targetEmployeeId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);

        var employeeData = getEmployeeData(targetEmployeeId);
        var duration = getDuration(req.reference());

        // 1. Define o conjunto de datas a serem consideradas.
        final Set<LocalDate> finalDatesSet;
        if (req.dates() != null && req.dates().length > 0) {
            finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        } else {
            return Collections.emptyList();
        }

        // 2. Busca TODOS os registros ATIVOS.
        List<TimeRecord> allRecordsForEmployee = getRecords(targetEmployeeId, req.active());

        // NOVO: Define todos os status que devem ser incluídos no relatório detalhado (trabalho + pausa)
        var includedStatuses = new HashSet<>(Set.of(CREATED, PENDING, UPDATED, PENDING_APPROVAL, DAY_OFF, StatusRecord.TIME_OFF, ABSENCE, StatusRecord.IMPLICIT_BREAK // Inclui a pausa explícita no relatório
        ));

        // 3. Filtra os registros de TRABALHO (segmentos) e PAUSAS
        List<TimeRecord> workRecords = allRecordsForEmployee.stream()
                // Filtra por datas selecionadas (usa a data do startWork)
                .filter(tr -> tr.startWork() != null && finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate()))
                // Aplica o filtro de status (se houver) - Inclui o IMPLICIT_BREAK se o status não for especificado.
                .filter(tr -> req.status() == null ? includedStatuses.contains(tr.statusRecord()) : tr.statusRecord() == req.status())
                // Garante que segmentos PENDING sem endWork sejam incluídos
                .filter(tr -> tr.endWork() != null || tr.statusRecord() == PENDING || tr.statusRecord() == StatusRecord.IMPLICIT_BREAK || tr.statusRecord() == StatusRecord.DAY_OFF || tr.statusRecord() == StatusRecord.ABSENCE || tr.statusRecord() == StatusRecord.TIME_OFF).collect(Collectors.toCollection(ArrayList::new));


        List<TimeRecordResponse> finalResponse = workRecords.stream()
                .map(timeRecord -> {
                    // Busca o path do documento se houver um timeRecordId associado.
                    String documentPath = documentProvider.findByTimeRecordId(timeRecord.timeRecordId())
                            .map(doc -> doc.documentId().toString()) // Assumindo que DOCUMENTS é a constante "/documents" de ApiPaths
                            .orElse(null);

                    // Passa o path do documento para o DTO
                    return TimeRecordResponse.fromDomain(timeRecord, duration, employeeData, documentPath);
                }).sorted(Comparator.comparing(TimeRecordResponse::startWork)).collect(Collectors.toCollection(ArrayList::new));

        // Ordenar por horário de início para melhor visualização

        return finalResponse;
    }

    @Override
    public TimeRecordApprovalPageResponse listPendingApprovals(int page, int size, String employeeName) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TimeRecordApprovalRequest> approvalsPage = approvalProvider.findAll(pageable, employeeName);

        // Mapeamento e lógica de preenchimento dos detalhes da aprovação
        List<TimeRecordApprovalResponse> responses = new ArrayList<>();

        for (TimeRecordApprovalRequest approvalData : approvalsPage.getContent()) {
            var timeRecord = recordRepository.findById(approvalData.timeRecordId()).orElse(null);

            var partnerEmployee = employeeProvider.findById(approvalData.requestingEmployeeId()).orElse(null);
            var managerUser = userProvider.findById(approvalData.managerId()).orElse(null);

            var document = documentProvider.findByTimeRecordId(approvalData.timeRecordId()).orElse(null);
            String documentPath = null;
            if (document != null) {
                 documentPath = "/documents/" + document.documentId();
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

        // Retorna o DTO de paginação
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
                .orElseThrow(() -> new ResourceNotFoundException("Manager não encontrado."));

        if (managerUser.role() != Role.MANAGER) {
            throw new BadRequestException("O usuário informado não é um manager.");
        }

        if (!employeeProvider.findById(managerUser.employeeId()).map(e -> e.companyId().equals(employee.companyId())).orElse(false)) {
            throw new BadRequestException("O manager não pertence à mesma empresa.");
        }

        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        List<Long> createdRecordIds = new ArrayList<>();

        // Validação básica: data de início não pode ser após a data de fim
        if (start.isAfter(end)) {
            throw new BadRequestException("A data de início das férias não pode ser posterior à data de fim.");
        }


        long daysBetween = ChronoUnit.DAYS.between(start, end) + 1;
        for (int i = 0; i < daysBetween; i++) {
            LocalDate currentDay = start.plusDays(i);
            LocalDateTime midnight = currentDay.atStartOfDay();

            // 2. Cria um registro para cada dia com status REQUEST_VACATION e 00:00 como hora
            var vacationRequestRecord = new TimeRecord(
                    null,
                    midnight,
                    midnight, // Saída também às 00:00 para garantir horas trabalhadas = 0
                    StatusRecord.REQUEST_VACATION,
                    false,
                    true,
                    employeeId
            );

            recordRepository.save(vacationRequestRecord);
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
            throw new ForbiddenException("Apenas Managers ou CTO podem aprovar solicitações de férias.");
        }

        // Aprova (muda o status) todos os registros na lista
        for (Long recordId : request.timeRecordIds()) {
            var record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + recordId));

            if (record.statusRecord() == StatusRecord.REQUEST_VACATION) {
                var approvedRecord = record.withStatus(StatusRecord.VACATION); // 4. Manager aprova -> VACATION
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
        // Validação da Role: Apenas MANAGER ou CTO podem rejeitar
        var userRole = jwtAuthenticatedUser.getRoleFromToken();
        if (!("MANAGER".equals(userRole) || "CTO".equals(userRole))) {
            throw new ForbiddenException("Apenas Managers ou CTO podem rejeitar solicitações de férias.");
        }

        // Rejeita (muda o status) todos os registros na lista
        for (Long recordId : request.timeRecordIds()) {
            var record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + recordId));

            if (record.statusRecord() == StatusRecord.REQUEST_VACATION) {
                var rejectedRecord = record.withStatus(StatusRecord.VACATION_REJECTED); // 4. Manager rejeita -> VACATION_REJECTED
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
            case "PENDING" -> Set.of(StatusRecord.REQUEST_VACATION);
            case "APPROVED" -> Set.of(StatusRecord.VACATION);
            case "REJECTED" -> Set.of(StatusRecord.VACATION_REJECTED);
            default -> EnumSet.of(StatusRecord.REQUEST_VACATION, StatusRecord.VACATION, StatusRecord.VACATION_REJECTED);
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
            throw new BadRequestException("A data de início não pode ser posterior à data de fim.");
        }
        if (request.startDate().equals(request.endDate()) && parseStartTime.isAfter(parseEndTime)) {
            throw new BadRequestException(HOURS_EXCEPTIONS);
        }

        // Validação do Manager
        var managerUser = userProvider.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager não encontrado."));
        if (managerUser.role() != Role.MANAGER) {
            throw new BadRequestException("O usuário informado não é um manager.");
        }
        if (!employeeProvider.findById(managerUser.employeeId()).map(e -> e.companyId().equals(employee.companyId())).orElse(false)) {
            throw new BadRequestException("O manager não pertence à mesma empresa.");
        }

        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        long daysBetween = ChronoUnit.DAYS.between(start, end) + 1;

        // Variáveis para reutilizar os metadados e o caminho do arquivo físico (storagePath)
        String uploadedStoragePath = null;
        String documentFileName = null;
        String documentContentType = null;
        Long firstRecordId = null; // Para retornar o primeiro ID criado


        for (int i = 0; i < daysBetween; i++) {
            LocalDate currentDay = start.plusDays(i);
            LocalDateTime currentStart = currentDay.atTime(parseStartTime);
            LocalDateTime currentEnd = currentDay.atTime(parseEndTime);

            // 1. Cria o registro de ponto (TimeRecord)
            var dailyTimeOffRecord = new TimeRecord(
                    null,
                    currentStart,
                    currentEnd,
                    StatusRecord.TIME_OFF_REQUEST,
                    true,
                    true,
                    employeeId
            );

            // Validação: evita duplicidade no dia
            if (recordRepository.existsByEmployeeIdAndDate(employeeId, currentDay)) {
                throw new BadRequestException("Já existe um registro de ponto ou solicitação para o dia: " + currentDay.format(DATE_FORMATTER));
            }

            // 2. Salva o registro e obtém o ID gerado pelo banco de dados
            // Nota: Assumimos que recordRepository.save agora retorna o TimeRecord com o ID populado.
            var savedRecord = recordRepository.save(dailyTimeOffRecord);
            if (i == 0) {
                firstRecordId = savedRecord.timeRecordId();
            }

            // 3. Lógica de Upload Físico e Associação de Documento
            if (document != null && !document.isEmpty()) {
                if (i == 0) {
                    // Primeiro dia (i=0): Faz o upload físico (Bucket) e salva a primeira Document Entity.
                    try {
                        documentService.uploadDocumentForTimeRecord(
                                DocumentType.TIME_OFF,
                                employeeId,
                                savedRecord.timeRecordId(), // Associa ao 1º registro
                                document
                        );

                        // Busca a Document Entity recém-criada para capturar o storagePath e metadados.
                        var uploadedDoc = documentProvider.findByTimeRecordId(savedRecord.timeRecordId())
                                .orElseThrow(() -> new IllegalStateException("Documento não encontrado após upload para o 1º registro."));

                        uploadedStoragePath = uploadedDoc.storagePath();
                        documentFileName = uploadedDoc.fileName();
                        documentContentType = uploadedDoc.contentType();

                    } catch (IOException e) {
                        log.error("Falha ao salvar o documento de abono para o registro {}: {}", savedRecord.timeRecordId(), e.getMessage());
                        throw new BadRequestException(NOT_ABLE_TO_READ_FILE + e.getMessage());
                    }
                }
                else if (uploadedStoragePath != null) {
                     var docToLink = new Document(
                            employeeId,
                            DocumentType.TIME_OFF,
                            documentFileName,
                            documentContentType,
                            uploadedStoragePath,
                            TIME_ZONE_BRAZIL,
                            savedRecord.timeRecordId()
                    );
                    documentProvider.save(docToLink);
                }
            }
        }


        if (firstRecordId == null) {
            throw new BadRequestException("Falha ao criar o primeiro registro de abono.");
        }
        return firstRecordId;
    }

    @Override
    public void approveTimeOff(Long timeRecordId) {
        var record = getTimeRecord(timeRecordId);

        if (record.statusRecord() != StatusRecord.TIME_OFF_REQUEST) {
            throw new BadRequestException("O registro não é uma solicitação de abono pendente (status atual: " + record.statusRecord() + ").");
        }

        // 1. Manager/CTO aprova: Status muda para TIME_OFF
        var approvedRecord = record.withStatus(StatusRecord.TIME_OFF);
        recordRepository.save(approvedRecord);
    }

    @Override
    public void rejectTimeOff(Long timeRecordId) {
        var record = getTimeRecord(timeRecordId);

        if (record.statusRecord() != StatusRecord.TIME_OFF_REQUEST) {
            throw new BadRequestException("O registro não é uma solicitação de abono pendente (status atual: " + record.statusRecord() + ").");
        }

        // 1. Manager/CTO rejeita: Status muda para TIME_OFF_REJECTED
        var rejectedRecord = record.withStatus(StatusRecord.TIME_OFF_REJECTED);
        recordRepository.save(rejectedRecord);
    }

    @Override
    public TimeRecordPageResponse listTimeOffRequests(String statusFilter, String employeeName, int page, int size) {

        // 1. Obtém o ID da empresa do Manager autenticado
        var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var companyId = getEmployee(managerEmployeeId).companyId();

        // 2. Define os Status a serem buscados com base no filtro
        Set<StatusRecord> targetStatuses = switch (statusFilter.toUpperCase()) {
            case "PENDING" -> Set.of(StatusRecord.TIME_OFF_REQUEST);
            case "APPROVED" -> Set.of(StatusRecord.TIME_OFF);
            case "REJECTED" -> Set.of(StatusRecord.TIME_OFF_REJECTED);
            default -> EnumSet.of(StatusRecord.TIME_OFF_REQUEST, StatusRecord.TIME_OFF, StatusRecord.TIME_OFF_REJECTED);
        };

        // 3. Busca todos os funcionários da empresa (para filtro e mapeamento)
        List<Employee> allEmployeesInCompany = employeeProvider.findByCompanyId(companyId);
        Map<UUID, Employee> employeeCache = allEmployeesInCompany.stream()
                .collect(Collectors.toMap(Employee::employeeId, emp -> emp));

        // 4. Aplica filtro de nome no Employee Cache e coleta os IDs relevantes
        Set<UUID> filteredEmployeeIds = employeeName != null && !employeeName.isBlank()
                ? employeeCache.values().stream()
                .filter(emp -> emp.fullName().toLowerCase().contains(employeeName.toLowerCase())) // FILTRO POR NOME
                .map(Employee::employeeId)
                .collect(Collectors.toSet())
                : employeeCache.keySet();

        // 5. Busca e filtra os registros de ponto com os status alvo para os IDs filtrados
        List<TimeRecord> timeOffRecords = filteredEmployeeIds.stream()
                .flatMap(empId -> recordRepository.findByEmployeeId(empId).stream())
                .filter(tr -> tr.startWork() != null)
                .filter(tr -> targetStatuses.contains(tr.statusRecord()))
                .toList();

        // 6. Mapeia para TimeRecordResponse e ordena
        var reference = Duration.ofHours(8);
        var companyName = companyUseCase.getCompanyNameById(companyId);

        List<TimeRecordResponse> mappedResponses = timeOffRecords.stream()
                .map(tr -> {
                    var emp = employeeCache.get(tr.employeeId());
                    var recordEmployeeData = new EmployeeData(emp.fullName(), companyName);

                    // --- INÍCIO DA NOVA LÓGICA: INCLUIR O PATH DO DOCUMENTO ---
                    String documentPath = documentProvider.findByTimeRecordId(tr.timeRecordId()) //
                            .map(doc ->  doc.documentId().toString()) //
                            .orElse(null);
                    // --- FIM DA NOVA LÓGICA ---

                    return TimeRecordResponse.fromDomain(tr, reference, recordEmployeeData, documentPath); //
                })
                .sorted(Comparator.comparing(TimeRecordResponse::startWork).reversed())
                .collect(Collectors.toList());

        // 7. APLICAÇÃO DA PAGINAÇÃO MANUAL
        long totalElements = mappedResponses.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, (int) totalElements);
        int end = Math.min(start + size, (int) totalElements);

        List<TimeRecordResponse> pageContent = mappedResponses.subList(start, end);

        // 8. Retorna o DTO de paginação
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
            throw new BadRequestException("O registro não está aguardando aprovação.");
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
        var employee = getEmployee(employeeId);
        var record = getTimeRecord(timeRecordId);

        isRecordBelongsEmployee(employee.employeeId(), record);
        return record;
    }

    private void checkGeolocation(UUID employeeId, double requestLatitude, double requestLongitude) {
        var employee = getEmployee(employeeId);

        var company = companyProvider.findById(employee.companyId()).orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada para o funcionário."));

        final double ALLOWED_DISTANCE_METERS = 80.0;
        var companyLocation = company.location();

        if (companyLocation == null) {
            throw new BadRequestException("A localização da empresa não está cadastrada.");
        }

        // Você precisará de uma função para calcular a distância entre os pontos
        double distance = calculateDistanceInMeters(companyLocation.latitude(), companyLocation.longitude(), requestLatitude, requestLongitude);

        if (distance > ALLOWED_DISTANCE_METERS) {
            throw new BadRequestException("Você está fora da área de trabalho permitida.");
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
                            StatusRecord.IMPLICIT_BREAK, succeeding.edited(), succeeding.active(), succeeding.employeeId());
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
                throw new BadRequestException("O novo horário se sobrepõe a um registro de trabalho existente (" + segment.startWork().format(DATE_TIME_FORMATTER) + " - " + segment.endWork().format(DATE_TIME_FORMATTER) + ").");
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


}