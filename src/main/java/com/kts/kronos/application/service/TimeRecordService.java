package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.application.port.out.repository.EmployeeRepository;
import com.kts.kronos.application.port.out.repository.TimeRecordRepository;
import com.kts.kronos.domain.model.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeRecordService implements TimeRecordUseCase {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private final TimeRecordRepository timeRecordRepo;
    private final EmployeeRepository employeeRepository;

    @Override
    public void checkin(CreateTimeRecordRequest req) {
        var employeeId = getEmployee(req.employeeId());

        if (timeRecordRepo.findOpenByEmployeeId(employeeId).isPresent()) {
            throw new BadRequestException("Check-out obrigatório antes de novo check-in");
        }

        var now = LocalDateTime.now(SAO_PAULO);
        TimeRecord tr = new TimeRecord(
                null,
                now,
                null,
                StatusRecord.PENDING,
                false,
                true,
                employeeId
        );
        timeRecordRepo.save(tr);


    }

    @Override
    public void checkout(CreateTimeRecordRequest req) {
        var employeeId = getEmployee(req.employeeId());
        var open = timeRecordRepo.findOpenByEmployeeId(employeeId)
                .orElseThrow(() -> new BadRequestException("Nenhum registro de check-in pendente encontrado"));

        var now = LocalDateTime.now(SAO_PAULO);
        var updated = open
                .withCheckout(now)
                .withStatus(open.statusRecord().onCheckout());

        timeRecordRepo.save(updated);
    }

    @Override
    public TimeRecordResponse updateTimeRecord(UpdateTimeRecordRequest req) {
        getEmployee(req.employeeId());

        var existing = getTimeRecord(req.timeRecordId());

        if (!existing.employeeId().equals(req.employeeId())) {
            throw new BadRequestException("Registro não pertence ao funcionário informado");
        }

        var timeFormatt = DateTimeFormatter.ofPattern("HH:mm");
        var start = LocalDateTime.of(
                req.startDate(),
                LocalTime.parse(req.startHour(), timeFormatt));
        var end = LocalDateTime.of(
                req.endDate(),
                LocalTime.parse(req.endHour(), timeFormatt));

        var now = LocalDateTime.now(SAO_PAULO);
        if (start.isAfter(now) || end.isAfter(now)) {
            throw new BadRequestException("Não é possível usar data/hora futura");
        }

        if (req.startDate().equals(req.endDate()) &&
                LocalTime.parse(req.startHour(), timeFormatt).isAfter(LocalTime.parse(req.endHour(), timeFormatt))) {
            throw new BadRequestException(
                    "Hora de início não pode ser posterior que hora final na mesma data");
        }
        var updated = existing
                .withCheckin(start)
                .withCheckout(end)
                .withEdited(true)
                .withStatus(existing.statusRecord().onUpdate());
        var saved = timeRecordRepo.save(updated);
        return TimeRecordResponse.fromDomain(saved, Duration.ZERO);
    }

    @Override
    public void deleteTimeRecord(DeleteTimeRecordRequest req) {
        getEmployee(req.employeeId());
        var existing = getTimeRecord(req.timeRecordId());

        if (!existing.employeeId().equals(req.employeeId())) {
            throw new BadRequestException(
                    "Registro não pertence ao funcionário informado");
        }
        timeRecordRepo.deleteTimeRecord(existing);
    }

    @Override
    public List<TimeRecordResponse> listReport(UUID employeeId, String reference, Boolean active, StatusRecord status, LocalDate[] dates) {
        getEmployee(employeeId);

        var duration = getDuration(reference);

        var records = active == null
                ? timeRecordRepo.findByEmployeeId(employeeId)
                : timeRecordRepo.findByEmployeeIdAndActive(employeeId, active);

        if (status != null) {
            records = records.stream().filter(record -> record.statusRecord() == status).toList();
        }
        if (dates != null && dates.length > 0) {
            var dateList = Arrays.asList(dates);
            var brasiliaTime = ZoneId.of("America/Sao_Paulo");
            records = records.stream()
                    .filter(record -> dateList.contains(record.startWork().atZone(brasiliaTime).toLocalDate())
                    ).toList();
        }
        return records.stream()
                .map(timeRecord -> TimeRecordResponse.fromDomain(timeRecord, duration))
                .toList();
    }

    @Override
    public void toggleActivate(ToggleActivate toggleActivate) {
        getEmployee(toggleActivate.employeeId());

        var existing = timeRecordRepo.findById(toggleActivate.timeRecordId()).
                orElseThrow(() -> new ResourceNotFoundException(
                        "TimeRecord não encontrado: " + toggleActivate.timeRecordId()));

        if (!existing.employeeId().equals(toggleActivate.employeeId())) {
            throw new BadRequestException("Registro não pertence ao funcionário informado");
        }

        TimeRecord toggle = existing.withActive(!existing.active());

        timeRecordRepo.save(toggle);
    }

    @Override
    public void updateStatus(UpdateTimeRecordStatusRequest req) {
        getEmployee(req.employeeId());

        var existing = timeRecordRepo.findById(req.timeRecordId()).
                orElseThrow(() -> new ResourceNotFoundException(
                        "TimeRecord não encontrado: " + req.timeRecordId()));

        if (!existing.employeeId().equals(req.employeeId())) {
            throw new BadRequestException("Registro não pertence ao funcionário informado");
        }
        var updateStatus = existing
                .withStatus(req.statusRecord());
        timeRecordRepo.save(updateStatus);
    }

    @Override
    public SimpleReportResponse reportResumido(SimpleReportRequest req) {
        getEmployee(req.employeeId());
        // converte reference “H:mm” → Duration

        String[] parts = req.reference().split(":");
        Duration dailyRef = Duration.ofHours(Long.parseLong(parts[0]))
                .plusMinutes(Long.parseLong(parts[1]));

        Set<StatusRecord> allStatuses = Set.of(
                StatusRecord.CREATED,
                StatusRecord.UPDATED,
                StatusRecord.DAY_OFF,
                StatusRecord.DOCTOR_APPOINTMENT,
                StatusRecord.ABSENCE
        );
        List<TimeRecord> records = timeRecordRepo.findByEmployeeIdAndActive(
                        req.employeeId(), true)
                .stream()
                .filter(tr -> allStatuses.contains(tr.statusRecord()))
                .toList();

        // 4) filtra pelas datas passadas
        Set<LocalDate> dateSet = Arrays.stream(req.dates()).collect(Collectors.toSet());
        records = records.stream()
                .filter(tr -> {
                    LocalDate day = tr.startWork()
                            .atZone(SAO_PAULO)
                            .toLocalDate();
                    return dateSet.contains(day);
                })
                .toList();

        // 5) agrupa por dia
        Map<LocalDate, List<TimeRecord>> byDay = records.stream()
                .collect(Collectors.groupingBy(tr ->
                                tr.startWork().atZone(SAO_PAULO).toLocalDate(),
                        TreeMap::new, Collectors.toList()
                ));

        List<SimpleReportDay> days = new ArrayList<>();
        Duration totalWorked = Duration.ZERO;
        Duration totalBalance = Duration.ZERO;

        for (var entry : byDay.entrySet()) {
            LocalDate date = entry.getKey();
            List<TimeRecord> trs = entry.getValue();

            // soma horas de todos os registros do dia
            Duration dailyWorked = trs.stream()
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                    .reduce(Duration.ZERO, Duration::plus);

            Duration dailyBalance;

            boolean onlyDayOff = trs.stream()
                    .allMatch(tr -> tr.statusRecord() == StatusRecord.DAY_OFF || tr.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT);

            if (onlyDayOff) {
                // balance zero
                dailyBalance = Duration.ZERO;
            } else {
                // balance normal, excluindo DAY_OFF e DOCTOR_APPOINTMENT
                Duration balanceInput = trs.stream()
                        .filter(tr -> tr.statusRecord() != StatusRecord.DAY_OFF
                                && tr.statusRecord() != StatusRecord.DOCTOR_APPOINTMENT)
                        .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
                        .reduce(Duration.ZERO, Duration::plus);

                dailyBalance = balanceInput.minus(dailyRef);
            }
            // formata
            String totalH = String.format("%02d:%02d",
                    dailyWorked.toHours(),
                    dailyWorked.toMinutesPart());
            String sign = dailyBalance.isNegative() ? "-" : "+";
            String bal = sign + String.format("%02d:%02d",
                    Math.abs(dailyBalance.toHours()),
                    Math.abs(dailyBalance.toMinutesPart()));

            // atualiza totais
            totalWorked = totalWorked.plus(dailyWorked);
            totalBalance = totalBalance.plus(dailyBalance);


            days.add(new SimpleReportDay(date, totalH, bal));
        }

        // monta totais finais
        String finalWorked = String.format("%02d:%02d",
                totalWorked.toHours(),
                totalWorked.toMinutesPart());
        String signAll = totalBalance.isNegative() ? "-" : "+";
        String finalBal = signAll + String.format("%02d:%02d",
                Math.abs(totalBalance.toHours()),
                Math.abs(totalBalance.toMinutesPart()));

        return new SimpleReportResponse(days, finalWorked, finalBal);
    }


    private UUID getEmployee(UUID uuid) {
        employeeRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado: " + uuid));
        return uuid;

    }

    private TimeRecord getTimeRecord(Long timeRecordId) {
        return timeRecordRepo.findById(timeRecordId).
                orElseThrow(() -> new ResourceNotFoundException(
                        "TimeRecord não encontrado: " + timeRecordId));
    }

    private static Duration getDuration(String reference) {
        String[] parts = reference.split(":");
        return Duration.ofHours(Long.parseLong(parts[0]))
                .plusMinutes(Long.parseLong(parts[1]));
    }

}
