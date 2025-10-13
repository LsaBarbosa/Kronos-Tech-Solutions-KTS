package com.kts.kronos.application.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.kts.kronos.adapter.in.messaging.dto.TimeRecordChangeRequestMessage;
import com.kts.kronos.adapter.in.web.dto.message.publisher.TimeRecordChangePublisher;
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

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TimeRecordService implements TimeRecordUseCase {
    private final TimeRecordProvider recordRepository;
    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final TimeRecordChangePublisher publisher;
    private final UserProvider userProvider;
    private final TimeRecordApprovalProvider approvalProvider;


    @Override
    public void registerTime(GeolocationRequest request) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        checkGeolocation(employeeId, request.latitude(), request.longitude());
        var employee = getEmployee(employeeId);

        var openRecordOpt = recordRepository.findOpenByEmployeeId(employee.employeeId());
        var currentTime = LocalDateTime.now(SAO_PAULO);

        if (openRecordOpt.isPresent()) {
            // É um CHECKOUT
            var open = openRecordOpt.get();

            // Adicionar log para DEBUG
            log.debug("Tentativa de Checkout. Registro ID: {}, Status Atual: {}", open.timeRecordId(), open.statusRecord());

            // Verifica se o status é PENDING. Se não for, loga um ERRO específico.
            if (open.statusRecord() != PENDING) {
                log.error("Tentativa de Checkout falhou. Status do registro ID {} é: {} (Esperado: PENDING)", open.timeRecordId(), open.statusRecord());
                // Se o front-end não espera uma exceção detalhada, você pode lançar uma BadRequest:
                throw new BadRequestException(STATUS_CHECKOUT + open.statusRecord() + ")");
            }

            // Se for PENDING, realiza a transição
            var updated = open.withCheckout(currentTime).withStatus(open.statusRecord().onCheckout());
            recordRepository.save(updated);
            log.info("Checkout registrado para o funcionário {}.", employee.employeeId());
        } else {
            // É um CHECKIN: Não encontrou registro aberto
            // Cria um novo registro com status PENDING
            var record = new TimeRecord(null, currentTime, null, PENDING, false, true, employee.employeeId());
            recordRepository.save(record);
            log.info("Checkin registrado para o funcionário {}.", employee.employeeId());
        }
    }

    public void registerBreak(GeolocationRequest request) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        checkGeolocation(employeeId, request.latitude(), request.longitude());
        var employee = getEmployee(employeeId);
        var currentTime = LocalDateTime.now(SAO_PAULO);

        // 1. Verifica se há um Check-in principal ativo (Deve haver para pausar)
        if (recordRepository.findOpenByEmployeeId(employee.employeeId()).isEmpty()) {
            throw new BadRequestException("Não é possível iniciar ou encerrar uma pausa sem um Check-in principal ativo.");
        }

        // 2. Verifica se há uma pausa aberta
        var openBreakOpt = recordRepository.findOpenBreakByEmployeeId(employee.employeeId());

        if (openBreakOpt.isPresent()) {
            // É um FIM DA PAUSA (Break End)
            var openBreak = openBreakOpt.get();
            var updated = openBreak.withCheckout(currentTime).withStatus(openBreak.statusRecord().onBreakEnd());
            recordRepository.save(updated);
            log.info("Fim da Pausa registrado para o funcionário {}.", employee.employeeId());

        } else {
            // É um INÍCIO DA PAUSA (Break Start)
            var breakRecord = new TimeRecord(null, currentTime, null, StatusRecord.BREAK_IN_PROGRESS, false, true, employee.employeeId());
            recordRepository.save(breakRecord);
            log.info("Início da Pausa registrado para o funcionário {}.", employee.employeeId());
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

            var messageData = new TimeRecordChangeRequestMessage(timeRecordId, employeeId, req.managerId(), start, end, TIME_ZONE_BRAZIL);

            // SUBSTITUIÇÃO DO REDIS: Salva a solicitação de aprovação no JPA via Provider
            approvalProvider.save(messageData.toDomain());

            var updatedRecord = record.withStatus(StatusRecord.PENDING_APPROVAL).withEdited(true);
            recordRepository.save(updatedRecord);

            // O publish continua usando o PubSub para notificação
            publisher.publishApprovalRequest(messageData);

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
        if (currentStatus == StatusRecord.PENDING_APPROVAL) {
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

        // Statuses de trabalho e pausa para inclusão na lista inicial
        var allWorkStatuses = Set.of(CREATED, UPDATED, DAY_OFF, DOCTOR_APPOINTMENT, ABSENCE);
        var breakStatuses = Set.of(StatusRecord.BREAK, StatusRecord.BREAK_IN_PROGRESS);
        var allStatusesToProcess = new HashSet<StatusRecord>();
        allStatusesToProcess.addAll(allWorkStatuses);
        allStatusesToProcess.addAll(breakStatuses);


        var recordsForEmployee = recordRepository.findByEmployeeIdAndActive(targetEmployeeId, true)
                .stream()
                // Garante que só peguemos registros relevantes (finalizados ou pausas em andamento)
                .filter(tr -> tr.endWork() != null || breakStatuses.contains(tr.statusRecord()))
                .filter(tr -> allStatusesToProcess.contains(tr.statusRecord()))
                .toList();

        // Uso de finalDatesSet para ser efetivamente final para o lambda
        final Set<LocalDate> finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        recordsForEmployee = recordsForEmployee.stream().filter(tr -> {
            var day = tr.startWork().atZone(SAO_PAULO).toLocalDate();
            return finalDatesSet.contains(day);
        }).toList();

        Map<LocalDate, List<TimeRecord>> recordByDay = recordsForEmployee.stream().collect(Collectors.groupingBy(tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate(), TreeMap::new, Collectors.toList()));

        List<SimpleReportDay> days = new ArrayList<>();
        var totalWorkedDuration = Duration.ZERO;
        var totalBreakDuration = Duration.ZERO; // NOVO: Acumulador de pausas totais
        var totalBalance = Duration.ZERO;


        for (var entry : recordByDay.entrySet()) {
            var startDate = entry.getKey();
            List<TimeRecord> entryValueRecords = entry.getValue();

            // 1. Separa registros de Trabalho (incluindo abonos) e Pausas
            List<TimeRecord> workRecords = entryValueRecords.stream()
                    .filter(tr -> !breakStatuses.contains(tr.statusRecord()))
                    .toList();

            List<TimeRecord> breakRecords = entryValueRecords.stream()
                    .filter(tr -> breakStatuses.contains(tr.statusRecord()))
                    .toList();

            // 2. Calcula Duração Total das Pausas (apenas concluídas)
            Duration dailyBreakDuration = breakRecords.stream()
                    .filter(tr -> tr.endWork() != null)
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                    .reduce(Duration.ZERO, Duration::plus);

            // 3. Calcula Duração de Trabalho Bruta (checkin/out, abonos)
            Duration dailyWorkGross = workRecords.stream()
                    .filter(tr -> tr.endWork() != null)
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                    .reduce(Duration.ZERO, Duration::plus);

            // 4. Calcula Duração de Trabalho Líquida (Descontando Pausa)
            Duration dailyWorkedLiquid = dailyWorkGross.minus(dailyBreakDuration);


            Duration dailyBalance;

            boolean onlySpecialNonWork = workRecords.stream().allMatch(tr -> tr.statusRecord() == StatusRecord.DAY_OFF || tr.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT || tr.statusRecord() == StatusRecord.ABSENCE);

            var endDate = entryValueRecords.stream().map(tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate()).max(LocalDate::compareTo).orElse(startDate);

            if (onlySpecialNonWork) {
                dailyBalance = Duration.ZERO;
                dailyWorkedLiquid = dailyWorkGross; // Para abonos/folgas, a duração total é a bruta.

            } else {
                // O cálculo do saldo deve usar o tempo líquido
                dailyBalance = dailyWorkedLiquid.minus(reference);
            }

            // --- INÍCIO DA LÓGICA DO STATUS DOMINANTE ---
            StatusRecord dailyStatus;
            if (workRecords.stream().anyMatch(tr -> tr.statusRecord() == StatusRecord.ABSENCE)) {
                dailyStatus = StatusRecord.ABSENCE;
            } else if (workRecords.stream().anyMatch(tr -> tr.statusRecord() == StatusRecord.PENDING_APPROVAL)) {
                dailyStatus = StatusRecord.PENDING_APPROVAL;
            } else if (workRecords.stream().anyMatch(tr -> tr.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT)) {
                dailyStatus = StatusRecord.DOCTOR_APPOINTMENT;
            } else if (workRecords.stream().allMatch(tr -> tr.statusRecord() == StatusRecord.DAY_OFF)) {
                dailyStatus = StatusRecord.DAY_OFF;
            } else if (workRecords.stream().anyMatch(tr -> tr.statusRecord() == StatusRecord.CREATED || tr.statusRecord() == StatusRecord.UPDATED)) {
                dailyStatus = StatusRecord.CREATED; // Indica um dia de trabalho normal
            } else {
                dailyStatus = StatusRecord.DAY_OFF; // Se não houver registros de trabalho (apenas pausas ou nada), é uma Folga
            }
            // --- FIM DA LÓGICA DO STATUS DOMINANTE ---

            // Formatação
            var totalHours = String.format("%02d:%02d", dailyWorkedLiquid.toHours(), dailyWorkedLiquid.toMinutesPart());
            var totalBreak = String.format("%02d:%02d", dailyBreakDuration.toHours(), dailyBreakDuration.toMinutesPart()); // NOVO: Formatação da pausa
            var sign = dailyBalance.isNegative() ? "-" : "+";
            var balance = sign + String.format("%02d:%02d", Math.abs(dailyBalance.toHours()), Math.abs(dailyBalance.toMinutesPart()));

            totalWorkedDuration = totalWorkedDuration.plus(dailyWorkedLiquid);
            totalBreakDuration = totalBreakDuration.plus(dailyBreakDuration); // NOVO: Soma total de pausas
            totalBalance = totalBalance.plus(dailyBalance);

            days.add(new SimpleReportDay(startDate, endDate, totalHours, totalBreak, balance, dailyStatus)); // NOVO: Passa dailyStatus
        }

        var finalWorked = String.format("%02d:%02d", totalWorkedDuration.toHours(), totalWorkedDuration.toMinutesPart());
        var finalBreak = String.format("%02d:%02d", totalBreakDuration.toHours(), totalBreakDuration.toMinutesPart()); // NOVO: Formatação da pausa total
        var signAll = totalBalance.isNegative() ? "-" : "+";
        var finalBalance = signAll + String.format("%02d:%02d", Math.abs(totalBalance.toHours()), Math.abs(totalBalance.toMinutesPart()));

        return new SimpleReportResponse(employeeData.employeeName(), employeeData.companyName(), days, finalWorked, finalBreak, finalBalance); // NOVO: Retorna finalBreak
    }
    @Override
    public byte[] simpleReportPDF(UUID employeeId, SimpleReportResponse report) {
        var baos = new ByteArrayOutputStream();
        var writer = new PdfWriter(baos);
        var pdfDoc = new PdfDocument(writer);
        var document = new Document(pdfDoc);
        var title = new Paragraph("Relatório de Horas");
        var employee = new Paragraph("Colaborador: " + report.enployeeName());
        var company = new Paragraph("Empresa: " + report.companyName());

        document.add(title.setFontSize(36).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
        document.add(company.setFontSize(28).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
        document.add(employee.setFontSize(26).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        float[] columnWidths = {1, 1, 1};
        var table = new Table(columnWidths).setPadding(5).setHorizontalAlignment(HorizontalAlignment.CENTER).setFontSize(24).setTextAlignment(TextAlignment.CENTER).setWidth(UnitValue.createPercentValue(100)).setHorizontalAlignment(HorizontalAlignment.CENTER);

        table.addHeaderCell(new Cell().add(new Paragraph("Data")));
        table.addHeaderCell(new Cell().add(new Paragraph("Horas Trabalhadas")));
        table.addHeaderCell(new Cell().add(new Paragraph("Saldo")));

        for (SimpleReportDay day : report.days()) {
            var startDate = day.startDate().format(DATE_FORMATTER);
            var endDate = day.endDate() != null ? day.endDate().format(DATE_FORMATTER) : "";
            table.addCell(startDate + "\n" + endDate);
            table.addCell(day.totalHours());
            var balance = day.balance();
            var balancePara = new Paragraph(balance);
            if (balance.startsWith("+")) {
                balancePara.setFontColor(ColorConstants.GREEN);
            } else if (balance.startsWith("-")) {
                balancePara.setFontColor(ColorConstants.RED);
            }
            table.addCell(new Cell().add(balancePara));
        }

        var finalBalance = report.totalBalance();
        var finalBalancePara = new Paragraph(finalBalance).setBold();
        if (finalBalance.startsWith("+")) {
            finalBalancePara.setFontColor(ColorConstants.GREEN);
        } else if (finalBalance.startsWith("-")) {
            finalBalancePara.setFontColor(ColorConstants.RED);
        }

        table.addCell(new Cell().add(new Paragraph("Total")).setBold());
        table.addCell(new Cell().add(new Paragraph(report.totalHoursWorked())).setBold());
        table.addCell(new Cell().add(finalBalancePara));

        document.add(table);
        document.close();
        return baos.toByteArray();
    }

    @Override
    public List<TimeRecordResponse> listReport(UUID employeeId, ListReportRequest req) {
        var targetEmployeeId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);

        var employeeData = getEmployeeData(targetEmployeeId);
        var duration = getDuration(req.reference());

        // 1. Define o conjunto de datas a serem consideradas (CORRIGIDO: usa Set final).
        final Set<LocalDate> finalDatesSet;
        if (req.dates() != null && req.dates().length > 0) {
            finalDatesSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        } else {
            // Se nenhuma data for fornecida, retorna lista vazia
            return Collections.emptyList();
        }

        // 2. Busca TODOS os registros ATIVOS (se houver filtro) do funcionário.
        List<TimeRecord> allRecordsForEmployee = getRecords(targetEmployeeId, req.active());

        // 3. Busca TODOS os registros de PAUSA para as datas filtradas.
        Map<LocalDate, List<TimeRecord>> breaksByDay = new HashMap<>();
        for (LocalDate date : finalDatesSet) { // Itera sobre o Set final
            // Busca as pausas de forma robusta por dia (método implementado anteriormente)
            List<TimeRecord> breaksForDay = recordRepository.findBreaksByEmployeeIdAndDate(targetEmployeeId, date);
            if (!breaksForDay.isEmpty()) {
                breaksByDay.put(date, breaksForDay);
            }
        }

        // 4. Filtra os registros de TRABALHO por data e status.
        List<TimeRecord> workRecords = allRecordsForEmployee.stream()
                // Exclui registros de pausa da lista principal de trabalho
                .filter(tr -> tr.statusRecord() != StatusRecord.BREAK && tr.statusRecord() != StatusRecord.BREAK_IN_PROGRESS)
                // Filtra por datas selecionadas (USANDO finalDatesSet)
                .filter(tr -> finalDatesSet.contains(tr.startWork().atZone(SAO_PAULO).toLocalDate()))
                // Aplica o filtro de status (se houver)
                .filter(tr -> req.status() == null || tr.statusRecord() == req.status())
                .toList();


        List<TimeRecordResponse> finalResponse = new ArrayList<>();

        // 5. Mapeia e Agrupa: Itera sobre os registros de trabalho filtrados e anexa as pausas.
        workRecords.stream()
                .forEach(timeRecord -> {
                    LocalDate workDay = timeRecord.startWork().atZone(SAO_PAULO).toLocalDate();

                    // Pausas que ocorreram no mesmo dia
                    List<TimeRecord> relatedBreaks = breaksByDay.getOrDefault(workDay, Collections.emptyList());

                    finalResponse.add(TimeRecordResponse.fromDomainWithBreaks(
                            timeRecord,
                            duration,
                            employeeData,
                            relatedBreaks // Passa a lista de pausas para o cálculo e exibição
                    ));
                });

        // 6. Adiciona registros que NÃO SÃO DE TRABALHO (pausas, abonos) se foram o alvo principal da busca.
        if (req.status() != null && (req.status() == StatusRecord.BREAK || req.status() == StatusRecord.BREAK_IN_PROGRESS || req.status() == StatusRecord.DAY_OFF || req.status() == StatusRecord.ABSENCE || req.status() == StatusRecord.DOCTOR_APPOINTMENT)) {
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
    public byte[] listReportPDF(List<TimeRecordResponse> records) {
        var baos = new ByteArrayOutputStream();
        var writer = new PdfWriter(baos);
        var pdfDoc = new PdfDocument(writer);
        var document = new Document(pdfDoc);
        var title = new Paragraph("Relatório de Horas detalhado");
        var employeeData = getEmployeeData(records.getLast().employeeId());
        var employee = new Paragraph("Colaborador: " + employeeData.employeeName());
        var company = new Paragraph("Empresa: " + employeeData.companyName());

        document.add(title.setFontSize(36).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
        document.add(company.setFontSize(28).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
        document.add(employee.setFontSize(28).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        float[] columnWidths = {1, 1, 1, 1, 1};
        Table table = new Table(columnWidths).setPadding(5).setHorizontalAlignment(HorizontalAlignment.CENTER).setFontSize(24).setTextAlignment(TextAlignment.CENTER).setWidth(UnitValue.createPercentValue(100)).setHorizontalAlignment(HorizontalAlignment.CENTER);
        table.addHeaderCell(new Cell().add(new Paragraph("Entrada")));
        table.addHeaderCell(new Cell().add(new Paragraph("Saída")));
        table.addHeaderCell(new Cell().add(new Paragraph("Status")));
        table.addHeaderCell(new Cell().add(new Paragraph("Diária")));
        table.addHeaderCell(new Cell().add(new Paragraph("Saldo")));

        for (TimeRecordResponse tr : records) {
            table.addCell(tr.startWork().format(DATE_FORMATTER) + "\n" + tr.startWork().format(TIME_FORMATTER)).setFontSize(16);
            table.addCell(tr.endWork().format(DATE_FORMATTER) + "\n" + tr.endWork().format(TIME_FORMATTER)).setFontSize(16);
            table.addCell(tr.statusRecord().name()).setFontSize(16);
            table.addCell(tr.hoursWork()).setFontSize(16);

            var balance = new Paragraph(tr.balance());
            if (tr.balance().startsWith("+")) {
                balance.setFontColor(ColorConstants.GREEN);
            } else if (tr.balance().startsWith("-")) {
                balance.setFontColor(ColorConstants.RED);
            }
            table.addCell(new Cell().add(balance)).setFontSize(16);
        }
        document.add(table);
        document.close();
        return baos.toByteArray();

    }

    @Override
    public List<TimeRecordApprovalResponse> listPendingApprovals() {
        // SUBSTITUIÇÃO DO REDIS: Busca todas as solicitações pendentes do JPA
        List<TimeRecordApprovalRequest> approvals = approvalProvider.findAll();

        if (approvals.isEmpty()) {
            return Collections.emptyList();
        }

        List<TimeRecordApprovalResponse> responses = new ArrayList<>();

        for (TimeRecordApprovalRequest approvalData : approvals) {
            var timeRecord = recordRepository.findById(approvalData.timeRecordId())
                    .orElse(null);
            // Busca os dados do colaborador e manager
            var partnerEmployee = employeeProvider.findById(approvalData.partnerEmployeeId())
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

    @Override
    public void approveTimeRecordChange(Long timeRecordId) {
        var record = findRecordAndCheckStatus(timeRecordId);

        // SUBSTITUIÇÃO DO REDIS: Busca a solicitação no novo Provider
        TimeRecordApprovalRequest approvalData = approvalProvider.findByTimeRecordId(timeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de aprovação não encontrada ou expirada para o registro: " + timeRecordId));

        // Aplica as alterações e atualiza o status
        var approvedRecord = record
                .withCheckin(approvalData.newStartWork())
                .withCheckout(approvalData.newEndWork())
                .withStatus(StatusRecord.UPDATED); // Status final: UPDATED

        recordRepository.save(approvedRecord);

        // SUBSTITUIÇÃO DO REDIS: Limpa o registro da tabela de aprovação (JPA)
        approvalProvider.deleteByTimeRecordId(timeRecordId);

        log.info("Solicitação para o registro {} foi APROVADA.", timeRecordId);
    }

    @Override
    public void rejectTimeRecordChange(Long timeRecordId) {
        var record = findRecordAndCheckStatus(timeRecordId);

        // Reverte o status do registro.
        var rejectedRecord = record.withStatus(StatusRecord.UPDATE_REJECTED).withEdited(false);
        recordRepository.save(rejectedRecord);

        // SUBSTITUIÇÃO DO REDIS: Limpa o registro da tabela de aprovação (JPA)
        approvalProvider.deleteByTimeRecordId(timeRecordId);

        log.info("Solicitação para o registro {} foi REJEITADA.", timeRecordId);
    }

    private TimeRecord findRecordAndCheckStatus(Long timeRecordId) {
        var record = recordRepository.findById(timeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException(RECORD_NOT_FOUND + timeRecordId));

        if (record.statusRecord() != StatusRecord.PENDING_APPROVAL) {
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

