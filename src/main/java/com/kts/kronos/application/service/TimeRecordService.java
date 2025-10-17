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
                    "Saída registrada às " + currentDateParsed + " às " + currentTimeParsed + " com sucesso! ",
                    "CHECKOUT"
            );
        } else {
            // É um CHECKIN (Inicia um novo segmento de trabalho)
            // 1. Verifica se a última saída foi no mesmo dia para calcular a pausa
            var latestRecordOpt = recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employee.employeeId());

            if (latestRecordOpt.isPresent()) {
                var latest = latestRecordOpt.get();
                var latestEndWork = latest.endWork();
                var currentStartDay = currentTime.atZone(SAO_PAULO).toLocalDate();
                var latestEndDay = latestEndWork != null ? latestEndWork.atZone(SAO_PAULO).toLocalDate() : null;

                // Se o último registro foi *encerrado* no MESMO DIA, é uma pausa implícita (novo segmento).
                if (latestEndWork != null && currentStartDay.equals(latestEndDay)) {
                    // Crie um novo registro de ponto com status PENDING
                    var record = new TimeRecord(null, currentTime, null, PENDING, false, true, employee.employeeId());
                    recordRepository.save(record);
                    log.info("Novo Checkin (após pausa implícita) registrado para o funcionário {}.", employee.employeeId());
                    return new ActionResponse(
                            "Bem-vindo de volta. Entrada registrada " + currentDateParsed + " às " + currentTimeParsed + ".",
                            "CHECKIN_AFTER_BREAK"
                    );
                }
            }

            // 2. Se for o primeiro ponto do dia/primeiro ponto geral
            var record = new TimeRecord(null, currentTime, null, PENDING, false, true, employee.employeeId());
            recordRepository.save(record);
            log.info("Primeiro Checkin do dia registrado para o funcionário {}.", employee.employeeId());
            return new ActionResponse(
                    "Entrada registrada " + currentDateParsed + " às " + currentTimeParsed + "! Seja bem-vindo(a) e tenha um dia produtivo.",
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

        var start = LocalDateTime.of(req.startDate(), parseStartTime);
        var end = LocalDateTime.of(req.endDate(), parseEndTime);


        if (req.startDate().equals(req.endDate()) && parseStartTime.isAfter(parseEndTime)) {
            throw new BadRequestException(HOURS_EXCEPTIONS);
        }
        if ("PARTNER".equals(userRole)) {
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
                    start,
                    end,
                    TIME_ZONE_BRAZIL
            );

            // 2. Persiste a solicitação
            approvalProvider.save(approvalRequest);

            var updatedRecord = record.withStatus(PENDING_APPROVAL).withEdited(true);
            recordRepository.save(updatedRecord);

        } else if ("MANAGER".equals(userRole) || "CTO".equals(userRole)) {
            // Lógica para o MANAGER/CTO (aprovação direta)
            var statusUpdate = record.statusRecord().onUpdate();
            var updated = record.withCheckin(start).withCheckout(end).withEdited(true).withStatus(statusUpdate);
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

        // Statuses de trabalho (agora excluindo breaks)
        var allWorkStatuses = Set.of(CREATED, UPDATED, DAY_OFF, DOCTOR_APPOINTMENT, ABSENCE, PENDING_APPROVAL, PENDING);

        // 1. Busca todos os registros ativos, incluindo segmentos PENDING.
        var recordsForEmployee = recordRepository.findByEmployeeIdAndActive(targetEmployeeId, true)
                .stream()
                // Apenas registros relevantes (finalizados ou PENDING)
                .filter(tr -> tr.endWork() != null || tr.statusRecord() == PENDING)
                .filter(tr -> allWorkStatuses.contains(tr.statusRecord()))
                .toList();

        final Set<LocalDate> finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        recordsForEmployee = recordsForEmployee.stream().filter(tr -> {
            // Usa o startWork para determinar a data do registro
            if (tr.startWork() != null) {
                return finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate());
            }
            return false;
        }).toList();

        // 2. Agrupamento por dia (usando o Map para coletar todos os segmentos do dia)
        Map<LocalDate, List<TimeRecord>> segmentsByDay = recordsForEmployee.stream()
                .collect(Collectors.groupingBy(
                        tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate(),
                        TreeMap::new, // Garante que as chaves (datas) estejam ordenadas
                        Collectors.toList()
                ));


        List<SimpleReportDay> days = new ArrayList<>();
        var totalWorkedDuration = Duration.ZERO;
        var totalBreakDuration = Duration.ZERO;
        var totalBalance = Duration.ZERO;


        for (var entry : segmentsByDay.entrySet()) {
            var startDate = entry.getKey();
            List<TimeRecord> segments = entry.getValue();

            // Ordena os segmentos pela hora de início para calcular o gap
            segments.sort(Comparator.comparing(TimeRecord::startWork));

            // 1. Calcula Duração Total das Pausas (Gap entre os segmentos)
            Duration dailyBreakDuration = calculateTotalBreakDuration(segments, SAO_PAULO);

            // 2. Calcula Duração de Trabalho Líquida (Soma do tempo de cada segmento)
            Duration dailyWorkedLiquid = segments.stream()
                    .filter(tr -> tr.endWork() != null) // Ignora segmentos não finalizados (PENDING)
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                    .reduce(Duration.ZERO, Duration::plus);

            // 3. Define a última data de saída do dia (pode ser diferente da data de início)
            LocalDateTime lastEndWork = segments.stream()
                    .map(TimeRecord::endWork)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(startDate.atStartOfDay());

            LocalDate endDate = lastEndWork.atZone(SAO_PAULO).toLocalDate();

            // 4. Determina Status e Balanço
            TimeRecord firstSegment = segments.getFirst();
            StatusRecord dailyStatus = firstSegment.statusRecord(); // Usa o status do primeiro segmento como o status principal do dia (se não for PENDING)

            Duration dailyBalance;
            boolean isSpecialStatus = dailyStatus == StatusRecord.DAY_OFF || dailyStatus == StatusRecord.DOCTOR_APPOINTMENT || dailyStatus == StatusRecord.ABSENCE;

            if (isSpecialStatus) {
                dailyBalance = Duration.ZERO;
                // Para abonos/folgas, a duração de trabalho líquida é a soma do tempo dos segmentos.
            } else {
                dailyBalance = dailyWorkedLiquid.minus(reference);
                if (segments.stream().anyMatch(tr -> tr.statusRecord() == PENDING)) {
                    // Se houver um segmento PENDING, o saldo fica no zero/em aberto, mas o tempo trabalhado acumula
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

        // 3. Filtra os registros de TRABALHO (segmentos)
        List<TimeRecord> workRecords = allRecordsForEmployee.stream()
                // Filtra por datas selecionadas (usa a data do startWork)
                .filter(tr -> tr.startWork() != null && finalDatesSet.
                        contains(tr.startWork().atZone(SAO_PAULO).toLocalDate()))
                // Aplica o filtro de status (se houver)
                .filter(tr -> req.status() == null || tr.statusRecord() == req.status())
                // Garante que segmentos PENDING sem endWork sejam incluídos, a menos que o status seja DAY_OFF/ABSENCE/DOCTOR_APPOINTMENT
                .filter(tr -> tr.endWork() != null || tr.statusRecord() == PENDING || (req.status() != null && (req.status() == StatusRecord.DAY_OFF || req.status() == StatusRecord.ABSENCE || req.status() == StatusRecord.DOCTOR_APPOINTMENT)))
                .collect(Collectors.toCollection(ArrayList::new)); // Converte para lista mutável imediatamente

        return workRecords.stream()
                .map(timeRecord -> TimeRecordResponse.fromDomain(timeRecord, duration, employeeData)).
                sorted(Comparator.comparing(TimeRecordResponse::startWork)).collect(Collectors.toCollection(ArrayList::new));
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