package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
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

    private final UserProvider userProvider;
    private final TimeRecordApprovalProvider approvalProvider;


    @Override
    public ActionResponse registerTime(GeolocationRequest request) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        checkGeolocation(employeeId, request.latitude(), request.longitude());
        var employee = getEmployee(employeeId);

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

            return new ActionResponse(
                    "Saída registrada " + currentDateParsed +" às "+ currentTimeParsed,
                    "CHECKOUT"
            );
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
                    var breakRecord = new TimeRecord(
                            null, // timeRecordId será gerado
                            latestEndWork, // Início da pausa é o fim do último trabalho
                            currentTime,   // Fim da pausa é o início do novo trabalho
                            StatusRecord.IMPLICIT_BREAK, // Novo status de pausa
                            false,
                            true,
                            employee.employeeId()
                    );
                    recordRepository.save(breakRecord);
                    log.info("Registro de Pausa Implícita criado entre {} e {}.", latestEndWork, currentTime);


                    // 2. CRIA O NOVO REGISTRO DE PONTO (Segmento de trabalho)
                    var record = new TimeRecord(null, currentTime, null, PENDING, false, true, employee.employeeId());
                    recordRepository.save(record);
                    log.info("Novo Checkin (após pausa) registrado para o funcionário {}.", employee.employeeId());
                    return new ActionResponse(
                            "Nova entrada de trabalho iniciado após pausa. " + currentDateParsed +" às "+ currentTimeParsed+".",
                            "CHECKIN_AFTER_BREAK"
                    );
                }
            }

            // 2. Se for o primeiro ponto do dia/primeiro ponto geral
            var record = new TimeRecord(null, currentTime, null, PENDING, false, true, employee.employeeId());
            recordRepository.save(record);
            log.info("Primeiro Checkin do dia registrado para o funcionário {}.", employee.employeeId());
            return new ActionResponse(
                    "Entrada registrada " + currentDateParsed +" às "+ currentTimeParsed+"! Seja bem-vindo(a).",
                    "CHECKIN"
            );
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
            var managerUser = userProvider.findById(req.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager não encontrado."));

            if (managerUser.role() != Role.MANAGER) {
                throw new BadRequestException("O usuário informado não é um manager.");
            }

            var managerEmployee = employeeProvider.findById(managerUser.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

            if (!managerEmployee.companyId().equals(employee.companyId())) {
                throw new BadRequestException("O manager não pertence à mesma empresa.");
            }

            // 1. Cria o payload simplificado
            var approvalRequest = new TimeRecordApprovalRequest(
                    timeRecordId,
                    employeeId,
                    req.managerId(),
                    newStart,
                    newEnd,
                    TIME_ZONE_BRAZIL
            );

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
        TimeRecordApprovalRequest approvalData = approvalProvider.findByTimeRecordId(timeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de aprovação não encontrada ou expirada para o registro: " + timeRecordId));

        // --- NOVO: Executa o ajuste dos registros de Pausa vizinhos ANTES de aplicar o ponto ---
        adjustAdjacentRecordsOnUpdate(
                record.employeeId(),
                record,
                approvalData.newStartWork(),
                approvalData.newEndWork()
        );
        // ---------------------------------------------------------------------------------------

        // 1. Aplica as alterações no registro principal
        var approvedRecord = record
                .withCheckin(approvalData.newStartWork())
                .withCheckout(approvalData.newEndWork())
                .withStatus(StatusRecord.UPDATED);

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
        var allRecords = recordRepository.findByEmployeeIdAndActive(targetEmployeeId, true)
                .stream()
                .filter(tr -> tr.startWork() != null) // Deve ter startWork para ser válido
                .filter(tr -> allRecordsStatuses.contains(tr.statusRecord()))
                .toList();

        final Set<LocalDate> finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        allRecords = allRecords.stream().filter(tr ->
                finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate())
        ).toList();


        // 2. Agrupamento por dia (usando o Map para coletar todos os registros do dia)
        Map<LocalDate, List<TimeRecord>> recordsByDay = allRecords.stream()
                .collect(Collectors.groupingBy(
                        tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate(),
                        TreeMap::new,
                        Collectors.toCollection(ArrayList::new)
                ));


        List<SimpleReportDay> days = new ArrayList<>();
        var totalWorkedDuration = Duration.ZERO;
        var totalBreakDuration = Duration.ZERO;
        var totalBalance = Duration.ZERO;


        for (var entry : recordsByDay.entrySet()) {
            var startDate = entry.getKey();
            List<TimeRecord> dailyRecords = entry.getValue();

            // Ordena os registros pela hora de início (essencial para definir a última saída e primeiro trabalho)
            dailyRecords.sort(Comparator.comparing(TimeRecord::startWork, Comparator.nullsLast(Comparator.naturalOrder())));

            // 1. Soma Duração Total das Pausas (Filtra pelo novo status IMPLICIT_BREAK)
            Duration dailyBreakDuration = dailyRecords.stream()
                    .filter(tr -> tr.statusRecord() == StatusRecord.IMPLICIT_BREAK)
                    .filter(tr -> tr.endWork() != null)
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                    .reduce(Duration.ZERO, Duration::plus);

            // 2. Calcula Duração de Trabalho Líquida (Soma do tempo de todos os segmentos de TRABALHO)
            Duration dailyWorkedLiquid = dailyRecords.stream()
                    .filter(tr -> workStatuses.contains(tr.statusRecord()) || specialStatuses.contains(tr.statusRecord())) // Apenas segmentos de trabalho (e abonos)
                    .filter(tr -> tr.endWork() != null) // Ignora segmentos de trabalho não finalizados (PENDING)
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                    .reduce(Duration.ZERO, Duration::plus);

            // 3. Define a última data de saída do dia
            LocalDateTime lastEndWork = dailyRecords.stream()
                    .map(TimeRecord::endWork)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(startDate.atStartOfDay());

            LocalDate endDate = lastEndWork.atZone(SAO_PAULO).toLocalDate();

            // 4. Determina Status e Balanço
            // O status do dia será o status do primeiro segmento de TRABALHO/ABONO do dia
            StatusRecord dailyStatus = dailyRecords.stream()
                    .filter(tr -> workStatuses.contains(tr.statusRecord()) || specialStatuses.contains(tr.statusRecord()))
                    .min(Comparator.comparing(TimeRecord::startWork))
                    .map(TimeRecord::statusRecord)
                    .orElse(StatusRecord.DAY_OFF); // Default para DAY_OFF se não houver registros de trabalho/abono.

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

            days.add(new SimpleReportDay(startDate, endDate, totalHours, totalBreak, balance, dailyStatus));
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
        var includedStatuses = new HashSet<>(Set.of(
                CREATED, PENDING, UPDATED, PENDING_APPROVAL,
                DAY_OFF, DOCTOR_APPOINTMENT, ABSENCE,
                StatusRecord.IMPLICIT_BREAK // Inclui a pausa explícita no relatório
        ));

        // 3. Filtra os registros de TRABALHO (segmentos) e PAUSAS
        List<TimeRecord> workRecords = allRecordsForEmployee.stream()
                // Filtra por datas selecionadas (usa a data do startWork)
                .filter(tr -> tr.startWork() != null && finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate()))
                // Aplica o filtro de status (se houver) - Inclui o IMPLICIT_BREAK se o status não for especificado.
                .filter(tr -> req.status() == null ? includedStatuses.contains(tr.statusRecord()) : tr.statusRecord() == req.status())
                // Garante que segmentos PENDING sem endWork sejam incluídos
                .filter(tr -> tr.endWork() != null || tr.statusRecord() == PENDING || tr.statusRecord() == StatusRecord.IMPLICIT_BREAK || tr.statusRecord() == StatusRecord.DAY_OFF || tr.statusRecord() == StatusRecord.ABSENCE || tr.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT)
                .collect(Collectors.toCollection(ArrayList::new));


        List<TimeRecordResponse> finalResponse = workRecords.stream()
                .map(timeRecord -> TimeRecordResponse.fromDomain(timeRecord, duration, employeeData))
                .collect(Collectors.toCollection(ArrayList::new));


        // Ordenar por horário de início para melhor visualização
        finalResponse.sort(Comparator.comparing(TimeRecordResponse::startWork));

        return finalResponse;
    }

    @Override
    public List<TimeRecordApprovalResponse> listPendingApprovals() {
        List<TimeRecordApprovalRequest> approvals = approvalProvider.findAll();

        if (approvals.isEmpty()) {
            return Collections.emptyList();
        }

        List<TimeRecordApprovalResponse> responses = new ArrayList<>();

        for (TimeRecordApprovalRequest approvalData : approvals) {
            var timeRecord = recordRepository.findById(approvalData.timeRecordId())
                    .orElse(null);

            var partnerEmployee = employeeProvider.findById(approvalData.requestingEmployeeId())
                    .orElse(null);
            var managerUser = userProvider.findById(approvalData.managerId())
                    .orElse(null);


            if (partnerEmployee != null && managerUser != null && timeRecord != null) {
                responses.add(new TimeRecordApprovalResponse(
                        approvalData.timeRecordId(),
                        partnerEmployee.fullName(),
                        managerUser.username(),
                        approvalData.newStartWork(),
                        approvalData.newEndWork(),
                        timeRecord.startWork(),
                        timeRecord.endWork()
                ));
            }
        }
        return responses;
    }

    private TimeRecord findRecordAndCheckStatus(Long timeRecordId) {
        var record = recordRepository.findById(timeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + timeRecordId));

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

        var company = companyProvider.findById(employee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada para o funcionário."));

        final double ALLOWED_DISTANCE_METERS = 80.0;
        var companyLocation = company.location();

        if (companyLocation == null) {
            throw new BadRequestException("A localização da empresa não está cadastrada.");
        }

        // Você precisará de uma função para calcular a distância entre os pontos
        double distance = calculateDistanceInMeters(
                companyLocation.latitude(), companyLocation.longitude(),
                requestLatitude, requestLongitude
        );

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
        List<TimeRecord> allDayRecords = recordRepository.findByEmployeeId(employeeId).stream()
                .filter(tr -> tr.startWork() != null && tr.startWork().toLocalDate().equals(day))
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
                    TimeRecord updatedBreak = new TimeRecord(
                            succeeding.timeRecordId(),
                            newStartBreak, // Novo start
                            succeeding.endWork(), // Fim original
                            StatusRecord.IMPLICIT_BREAK,
                            succeeding.edited(),
                            succeeding.active(),
                            succeeding.employeeId()
                    );
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
        Set<StatusRecord> nonBreakStatuses = EnumSet.complementOf(EnumSet.of(
                StatusRecord.IMPLICIT_BREAK,
                StatusRecord.DAY_OFF,
                StatusRecord.DOCTOR_APPOINTMENT,
                StatusRecord.ABSENCE
        ));

        // 1. Buscar todos os registros de trabalho (non-breaks) do dia, exceto o que está sendo editado
        List<TimeRecord> workSegments = recordRepository.findByEmployeeId(employeeId).stream()
                .filter(tr -> !tr.timeRecordId().equals(currentRecordId))
                .filter(tr -> tr.startWork() != null && tr.startWork().toLocalDate().equals(day))
                .filter(tr -> nonBreakStatuses.contains(tr.statusRecord()))
                .filter(tr -> tr.endWork() != null) // Só checa segmentos fechados
                .sorted(Comparator.comparing(TimeRecord::startWork))
                .toList();

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
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // Retorna a distância em metros
    }
}