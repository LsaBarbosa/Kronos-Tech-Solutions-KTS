package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final BreakRecordProvider breakProvider;

    @Override
    public ActionResponse registerTime(GeolocationRequest request) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        checkGeolocation(employeeId, request.latitude(), request.longitude());
        var employee = getEmployee(employeeId);

        var openRecordOpt = recordRepository.findOpenByEmployeeId(employee.employeeId());
        var currentTime = LocalDateTime.now(SAO_PAULO);

        if (openRecordOpt.isPresent()) {
            // É um CHECKOUT
            var open = openRecordOpt.get();

            log.debug("Tentativa de Checkout. Registro ID: {}, Status Atual: {}", open.timeRecordId(), open.statusRecord());

            if (open.statusRecord() != PENDING) {
                log.error("Tentativa de Checkout falhou. Status do registro ID {} é: {} (Esperado: PENDING)", open.timeRecordId(), open.statusRecord());
                throw new BadRequestException(STATUS_CHECKOUT + open.statusRecord() + ")");
            }

            // NOVO: Verifica se há alguma pausa em progresso e a finaliza antes de fechar o ponto
            var openBreakOpt = breakProvider.findOpenBreakByTimeRecordId(open.timeRecordId());
            if (openBreakOpt.isPresent()) {
                var openBreak = openBreakOpt.get();
                var updatedBreak = openBreak.withEndBreak(currentTime).withActive(false);
                breakProvider.save(updatedBreak);
                log.info("Pausa aberta finalizada automaticamente no checkout do registro ID {}.", open.timeRecordId());
            }

            // Se for PENDING, realiza a transição
            var updated = open.withCheckout(currentTime).withStatus(open.statusRecord().onCheckout());
            recordRepository.save(updated);
            log.info("Checkout registrado para o funcionário {}.", employee.employeeId());

            return new ActionResponse(
                    "Saída registrada as " + currentTime +" com sucesso! Tenha um ótimo descanso.",
                    "CHECKOUT"
            );
        } else {
            // É um CHECKIN: Não encontrou registro aberto
            // Cria um novo registro com status PENDING
            var record = new TimeRecord(null, currentTime, null, PENDING, false, true, employee.employeeId());
            recordRepository.save(record);
            log.info("Checkin registrado para o funcionário {}.", employee.employeeId());
            return new ActionResponse(
                    "Entrada registrada as " + currentTime+" ! Seja bem-vindo(a) e tenha um dia produtivo.",
                    "CHECKIN"
            );
        }
    }

    @Override
    public ActionResponse registerBreak(GeolocationRequest request) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        checkGeolocation(employeeId, request.latitude(), request.longitude());
        var employee = getEmployee(employeeId);
        var currentTime = LocalDateTime.now(SAO_PAULO);
        var openRecordOpt = recordRepository.findOpenByEmployeeId(employee.employeeId());

        if (openRecordOpt.isEmpty()) {
            throw new BadRequestException("Não é possível iniciar ou encerrar uma pausa sem um Check-in principal ativo.");
        }
        var openRecord = openRecordOpt.get();

        var openBreakOpt = breakProvider.findOpenBreakByTimeRecordId(openRecord.timeRecordId());

        if (openBreakOpt.isPresent()) {
            // É um FIM DA PAUSA (Break End)
            var openBreak = openBreakOpt.get();
            var updatedBreak = openBreak.withEndBreak(currentTime).withActive(false);
            breakProvider.save(updatedBreak);
            log.info("Fim da Pausa registrado para o registro ID {}.", openRecord.timeRecordId());
            return new ActionResponse(
                    "Pausa encerrada "+ currentTime +" ! Ótimo retorno ao trabalho.",
                    "BREAK_END"
            );
        } else {
            // É um INÍCIO DA PAUSA (Break Start)
            // Cria e salva um novo BreakRecord associado ao TimeRecord principal
            var newBreak = new BreakRecord(openRecord.timeRecordId(), currentTime);
            breakProvider.save(newBreak);
            log.info("Início da Pausa registrado para o registro ID {}.", openRecord.timeRecordId());
            return new ActionResponse(
                    "Pausa iniciada " +currentTime + " ! Aproveite o descanso.",
                    "BREAK_START"
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

            // 1. Mapeia e valida as solicitações de alteração de pausa
            List<BreakRecordApprovalRequest> breakRequests = mapAndValidateBreakRequests(timeRecordId, record.breaks(), req.breakRequests());

            // 2. Cria o payload completo (Registro principal + Pausas)
            var completeApprovalRequest = new TimeRecordApprovalRequest(
                    timeRecordId,
                    employeeId,
                    req.managerId(),
                    start,
                    end,
                    TIME_ZONE_BRAZIL,
                    breakRequests // Anexa as solicitações de pausa
            );

            // 3. Persiste a solicitação completa no JPA (incluindo breaks via Cascade)
            approvalProvider.save(completeApprovalRequest);

            var updatedRecord = record.withStatus(StatusRecord.PENDING_APPROVAL).withEdited(true);
            recordRepository.save(updatedRecord);

        } else if ("MANAGER".equals(userRole)) {
            // Lógica original para o MANAGER (aprovação direta)
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

        // Busca a solicitação completa (incluindo pausas)
        TimeRecordApprovalRequest approvalData = approvalProvider.findByTimeRecordId(timeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de aprovação não encontrada ou expirada para o registro: " + timeRecordId));

        // 1. Aplica as alterações no registro principal
        var approvedRecord = record
                .withCheckin(approvalData.newStartWork())
                .withCheckout(approvalData.newEndWork())
                .withStatus(StatusRecord.UPDATED);

        // 2. Aplica as alterações nas pausas aninhadas
        for (BreakRecordApprovalRequest breakApproval : approvalData.breakApprovalRequests()) {
            var originalBreakOpt = record.breaks().stream()
                    .filter(b -> b.breakRecordId().equals(breakApproval.breakRecordId()))
                    .findFirst();

            if (originalBreakOpt.isPresent()) {
                var originalBreak = originalBreakOpt.get();

                // Cria um novo BreakRecord com as horas aprovadas (requer BreakRecordProvider.save)
                var breakToUpdate = new BreakRecord(
                        originalBreak.breakRecordId(),
                        originalBreak.timeRecordId(),
                        breakApproval.newStartBreak(),
                        breakApproval.newEndBreak(),
                        originalBreak.active()
                );

                // Salva a pausa individual atualizada
                breakProvider.save(breakToUpdate);
            }
        }

        // 3. Salva o registro principal atualizado
        recordRepository.save(approvedRecord);

        // 4. Limpa o registro de aprovação
        approvalProvider.deleteByTimeRecordId(timeRecordId);

        log.info("Solicitação para o registro {} foi APROVADA.", timeRecordId);
    }

    @Override
    public void rejectTimeRecordChange(Long timeRecordId) {
        var record = findRecordAndCheckStatus(timeRecordId);

        // Reverte o status do registro.
        var rejectedRecord = record.withStatus(StatusRecord.UPDATE_REJECTED).withEdited(false);
        recordRepository.save(rejectedRecord);

        // Limpa o registro de aprovação (O Cascade do JPA cuidará das pausas aninhadas)
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

        var recordsForEmployee = recordRepository.findByEmployeeIdAndActive(targetEmployeeId, true)
                .stream()
                // Garante que só peguemos registros relevantes (finalizados ou PENDING)
                .filter(tr -> tr.endWork() != null || tr.statusRecord() == PENDING)
                .filter(tr -> allWorkStatuses.contains(tr.statusRecord()))
                .toList();

        final Set<LocalDate> finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        recordsForEmployee = recordsForEmployee.stream().filter(tr -> finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate())).toList();

        // Agrupamento por dia (usando toMap para garantir apenas um registro principal por dia)
        Map<LocalDate, TimeRecord> workRecordByDay = recordsForEmployee.stream()
                .collect(Collectors.toMap(
                        tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate(),
                        tr -> tr,
                        (existing, replacement) -> existing, // Em caso de duplicidade, mantém o primeiro
                        TreeMap::new
                ));


        List<SimpleReportDay> days = new ArrayList<>();
        var totalWorkedDuration = Duration.ZERO;
        var totalBreakDuration = Duration.ZERO;
        var totalBalance = Duration.ZERO;


        for (var entry : workRecordByDay.entrySet()) {
            var startDate = entry.getKey();
            TimeRecord workRecord = entry.getValue();

            // 1. Calcula Duração Total das Pausas (usando a lista BreakRecord do TimeRecord)
            Duration dailyBreakDuration = workRecord.breaks().stream()
                    .filter(br -> br.endBreak() != null)
                    .map(br -> Duration.between(br.startBreak(), br.endBreak()))
                    .reduce(Duration.ZERO, Duration::plus);

            // 2. Calcula Duração de Trabalho Bruta (checkin/out, abonos)
            Duration dailyWorkGross = Duration.ZERO;
            if (workRecord.endWork() != null) {
                dailyWorkGross = Duration.between(workRecord.startWork(), workRecord.endWork());
            }

            // 3. Calcula Duração de Trabalho Líquida (Descontando Pausa)
            Duration dailyWorkedLiquid = dailyWorkGross.minus(dailyBreakDuration);

            Duration dailyBalance;

            boolean onlySpecialNonWork = workRecord.statusRecord() == StatusRecord.DAY_OFF || workRecord.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT || workRecord.statusRecord() == StatusRecord.ABSENCE;

            var endDate = workRecord.endWork() != null ? workRecord.endWork().atZone(SAO_PAULO).toLocalDate() : startDate;

            if (onlySpecialNonWork) {
                dailyBalance = Duration.ZERO;
                dailyWorkedLiquid = dailyWorkGross; // Para abonos/folgas, a duração total é a bruta.

            } else {
                dailyBalance = dailyWorkedLiquid.minus(reference);
            }

            StatusRecord dailyStatus = workRecord.statusRecord();

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

        // 2. Busca TODOS os registros ATIVOS. TimeRecordEntity agora carrega os Breaks via EAGER.
        List<TimeRecord> allRecordsForEmployee = getRecords(targetEmployeeId, req.active());

        // 3. Filtra os registros de TRABALHO (excluindo os breaks que agora são aninhados)
        List<TimeRecord> workRecords = allRecordsForEmployee.stream()
                // Mantém apenas os registros principais de ponto/abono
                .filter(tr -> tr.statusRecord() != StatusRecord.DAY_OFF && tr.statusRecord() != StatusRecord.ABSENCE && tr.statusRecord() != StatusRecord.DOCTOR_APPOINTMENT)
                // Filtra por datas selecionadas
                .filter(tr -> finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate()))
                // Aplica o filtro de status (se houver)
                .filter(tr -> req.status() == null || tr.statusRecord() == req.status())
                .toList();


        List<TimeRecordResponse> finalResponse = new ArrayList<>();

        // 4. Mapeia e Agrupa: Itera sobre os registros de trabalho filtrados e anexa as pausas.
        workRecords.stream()
                .forEach(timeRecord -> {
                    // O TimeRecord já tem os breaks carregados (devido ao FetchType.EAGER em TimeRecordEntity)
                    List<BreakRecord> relatedBreaks = timeRecord.breaks();

                    finalResponse.add(TimeRecordResponse.fromDomainWithBreaks(
                            timeRecord,
                            duration,
                            employeeData,
                            relatedBreaks // Passa a lista de pausas do TimeRecord
                    ));
                });

        // 5. Adiciona registros que SÃO DE ABONO se foram o alvo principal da busca.
        if (req.status() != null && (req.status() == StatusRecord.DAY_OFF || req.status() == StatusRecord.ABSENCE || req.status() == StatusRecord.DOCTOR_APPOINTMENT)) {
            allRecordsForEmployee.stream()
                    .filter(tr -> tr.statusRecord() == req.status())
                    .filter(tr -> finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate()))
                    .map(timeRecord -> TimeRecordResponse.fromDomain(timeRecord, duration, employeeData))
                    .forEach(finalResponse::add);
        }

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

            // Mapeia as pausas aninhadas para o DTO de resposta
            List<BreakApprovalResponse> mappedBreaks = approvalData.breakApprovalRequests().stream()
                    .map(BreakApprovalResponse::fromDomain)
                    .toList();

            if (partnerEmployee != null && managerUser != null && timeRecord != null) {
                responses.add(new TimeRecordApprovalResponse(
                        approvalData.timeRecordId(),
                        partnerEmployee.fullName(),
                        managerUser.username(),
                        approvalData.newStartWork(),
                        approvalData.newEndWork(),
                        timeRecord.startWork(),
                        timeRecord.endWork(),
                        mappedBreaks
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

    private List<TimeRecord> getRecords(UUID employeeId, Boolean active) {
        return active == null ? recordRepository.findByEmployeeId(employeeId) : recordRepository.findByEmployeeIdAndActive(employeeId, active);
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

    private List<BreakRecordApprovalRequest> mapAndValidateBreakRequests(Long timeRecordId, List<BreakRecord> currentBreaks, List<UpdateBreakRecordRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        return requests.stream().map(req -> {
            // 1. Encontra a pausa original
            BreakRecord originalBreak = currentBreaks.stream()
                    .filter(b -> b.breakRecordId().equals(req.breakRecordId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Pausa não encontrada: " + req.breakRecordId()));

            // 2. Converte as horas para LocalDateTime (usando a data do registro original como base)
            LocalDate breakDate = originalBreak.startBreak().toLocalDate();

            var newStart = LocalDateTime.of(breakDate, LocalTime.parse(req.startHour(), TIME_FORMATTER));
            var newEnd = LocalDateTime.of(breakDate, LocalTime.parse(req.endHour(), TIME_FORMATTER));

            // 3. Validação básica de horas
            if (newStart.isAfter(newEnd)) {
                throw new BadRequestException("Hora de início da pausa deve ser anterior à hora de fim da pausa.");
            }

            // 4. Cria o objeto de aprovação aninhado
            return new BreakRecordApprovalRequest(
                    timeRecordId,
                    req.breakRecordId(),
                    newStart,
                    newEnd
            );
        }).toList();
    }

    private double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        // Implementação da fórmula de Haversine ou outra mais precisa.
        // Exemplo:
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