package com.kts.kronos.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Role;
import com.kts.kronos.domain.model.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TimeRecordService implements TimeRecordUseCase {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TimeRecordProvider recordRepository;
    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final RabbitTemplate rabbitTemplate;
    private final UserProvider userProvider;

    @Override
    public void checkin() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);

        if (recordRepository.findOpenByEmployeeId(employee.employeeId()).isPresent()) {
            throw new BadRequestException(CHECKIN_EXCEPTION);
        }

        var record = new TimeRecord(null, TIME_ZONE_BRAZIL, null, PENDING, false, true, employee.employeeId());
        recordRepository.save(record);
    }

    @Override
    public void checkout() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = getEmployee(employeeId);
        var open = recordRepository.findOpenByEmployeeId(employee.employeeId()).orElseThrow(() -> new BadRequestException(CHECKOUT_EXCEPTION));

        var updated = open.withCheckout(TIME_ZONE_BRAZIL).withStatus(open.statusRecord().onCheckout());

        recordRepository.save(updated);
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

        // Lógica condicional baseada na Role
        if ("PARTNER" .equals(userRole)) {
            if (req.managerId() == null) {
                throw new BadRequestException("O ID do manager é obrigatório para parceiros.");
            }

            // Validar o manager
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

            // Salva a proposta de alteração no Redis com validade de 7 dias
            var approvalData = new TimeRecordChangeRequestMessage(timeRecordId, employeeId, req.managerId(), start, end);
            String redisKey = APPROVAL_KEY_PREFIX + timeRecordId;
            redisTemplate.opsForValue().set(redisKey, approvalData, 7, TimeUnit.DAYS);

            // Atualiza o status do registro para PENDING_APPROVAL
            var updatedRecord = record.withStatus(StatusRecord.PENDING_APPROVAL).withEdited(true);
            recordRepository.save(updatedRecord);

            // Envia a mensagem para a fila de forma assíncrona
            rabbitTemplate.convertAndSend(TIME_RECORD_EXCHANGE, ROUTING_KEY, approvalData);

        } else if ("MANAGER" .equals(userRole)) {
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
        var updateStatus = record.withStatus(req.statusRecord());
        recordRepository.save(updateStatus);
    }

    @Override
    public SimpleReportResponse simpleReport(UUID employeeId, SimpleReportRequest req) {
        var targetEmployeeId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        var employeeData = getEmployeeData(targetEmployeeId);

        String[] parts = req.reference().split(":");

        var reference = Duration.ofHours(Long.parseLong(parts[0])).plusMinutes(Long.parseLong(parts[1]));

        var allStatuses = Set.of(CREATED, UPDATED, DAY_OFF, DOCTOR_APPOINTMENT, ABSENCE);

        var recordsById = recordRepository.findByEmployeeIdAndActive(targetEmployeeId, true).stream().filter(tr -> allStatuses.contains(tr.statusRecord())).toList();

        var dateSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        recordsById = recordsById.stream().filter(tr -> {
            var day = tr.startWork().atZone(SAO_PAULO).toLocalDate();
            return dateSet.contains(day);
        }).toList();

        Map<LocalDate, List<TimeRecord>> recordByDay = recordsById.stream().collect(Collectors.groupingBy(tr -> tr.startWork().atZone(SAO_PAULO).toLocalDate(), TreeMap::new, Collectors.toList()));

        List<SimpleReportDay> days = new ArrayList<>();
        var totalWorked = Duration.ZERO;
        var totalBalance = Duration.ZERO;


        for (var entry : recordByDay.entrySet()) {
            var startDate = entry.getKey();
            List<TimeRecord> entryValueRecords = entry.getValue();

            var dailyWorked = entryValueRecords.stream().map(tr -> Duration.between(tr.startWork(), tr.endWork())).reduce(Duration.ZERO, Duration::plus);

            Duration dailyBalance;

            boolean onlyDayOff = entryValueRecords.stream().allMatch(tr -> tr.statusRecord() == StatusRecord.DAY_OFF || tr.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT);

            var endDate = entryValueRecords.stream().map(tr -> tr.endWork().atZone(SAO_PAULO).toLocalDate()).max(LocalDate::compareTo).orElse(startDate);

            if (onlyDayOff) {
                dailyBalance = Duration.ZERO;

            } else {
                Duration balanceInput = entryValueRecords.stream().filter(tr -> tr.statusRecord() != StatusRecord.DAY_OFF && tr.statusRecord() != StatusRecord.DOCTOR_APPOINTMENT)

                        .map(tr -> Duration.between(tr.startWork(), tr.endWork())).reduce(Duration.ZERO, Duration::plus);

                dailyBalance = balanceInput.minus(reference);
            }

            var totalHours = String.format("%02d:%02d", dailyWorked.toHours(), dailyWorked.toMinutesPart());
            var sign = dailyBalance.isNegative() ? "-" : "+";
            var balance = sign + String.format("%02d:%02d", Math.abs(dailyBalance.toHours()), Math.abs(dailyBalance.toMinutesPart()));

            totalWorked = totalWorked.plus(dailyWorked);
            totalBalance = totalBalance.plus(dailyBalance);

            days.add(new SimpleReportDay(startDate, endDate, totalHours, balance));
        }

        var finalWorked = String.format("%02d:%02d", totalWorked.toHours(), totalWorked.toMinutesPart());
        var signAll = totalBalance.isNegative() ? "-" : "+";
        var finalBalance = signAll + String.format("%02d:%02d", Math.abs(totalBalance.toHours()), Math.abs(totalBalance.toMinutesPart()));

        return new SimpleReportResponse(employeeData.employeeName(), employeeData.companyName(), days, finalWorked, finalBalance);
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
        var records = getRecords(targetEmployeeId, req.active());

        if (req.status() != null) {
            records = records.stream().filter(record -> record.statusRecord() == req.status()).toList();
        }
        if (req.dates() != null && req.dates().length > 0) {
            var dateList = Arrays.asList(req.dates());
            var brasiliaTime = ZoneId.of("America/Sao_Paulo");
            records = records.stream().filter(record -> dateList.contains(record.startWork().atZone(brasiliaTime).toLocalDate())).toList();
        }
        return records.stream().map(timeRecord -> TimeRecordResponse.fromDomain(timeRecord, duration, employeeData)).toList();
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
    public void approveTimeRecordChange(Long timeRecordId) {
        var record = findRecordAndCheckStatus(timeRecordId);
        String redisKey = APPROVAL_KEY_PREFIX + timeRecordId;

        // Busca a solicitação no Redis como um objeto genérico
        Object approvalDataObject = redisTemplate.opsForValue().get(redisKey);

        if (approvalDataObject == null) {
            throw new ResourceNotFoundException("Solicitação de aprovação não encontrada ou expirada para o registro: " + timeRecordId);
        }

        // Usa o ObjectMapper para converter o objeto (que é um Map) para a sua classe específica
        TimeRecordChangeRequestMessage approvalData = objectMapper.convertValue(
                approvalDataObject,
                TimeRecordChangeRequestMessage.class
        );
        // Aplica as alterações e atualiza o status
        var approvedRecord = record
                .withCheckin(approvalData.newStartWork())
                .withCheckout(approvalData.newEndWork())
                .withStatus(StatusRecord.UPDATED); // Status final: UPDATED

        recordRepository.save(approvedRecord);

        // Limpa a chave do Redis
        redisTemplate.delete(redisKey);

        log.info("Solicitação para o registro {} foi APROVADA.", timeRecordId);
        // Opcional: Enviar notificação de volta para o PARTNER
    }

    @Override
    public void rejectTimeRecordChange(Long timeRecordId) {
        var record = findRecordAndCheckStatus(timeRecordId);
        String redisKey = APPROVAL_KEY_PREFIX + timeRecordId;

        // Reverte o status do registro.
        // Aqui, revertemos para CREATED e `edited` para false, mas poderia ser outra lógica.
        var rejectedRecord = record.withStatus(StatusRecord.UPDATE_REJECTED).withEdited(false);
        recordRepository.save(rejectedRecord);

        // Limpa a chave do Redis
        redisTemplate.delete(redisKey);

        log.info("Solicitação para o registro {} foi REJEITADA.", timeRecordId);
        // Opcional: Enviar notificação de volta para o PARTNER
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
}
