package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.CreateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
                .withStatus(StatusRecord.CREATED);

        timeRecordRepo.save(updated);
    }

    @Override
    public TimeRecordResponse updateTimeRecord(UpdateTimeRecordRequest req) {
        getEmployee(req.employeeId());

        var existing = timeRecordRepo.findById(req.timeRecordId()).
                orElseThrow(() -> new ResourceNotFoundException(
                "TimeRecord não encontrado: " + req.timeRecordId()));

        if (!existing.employeeId().equals(req.employeeId())) {
            throw new BadRequestException("Registro não pertence ao funcionário informado");
        }

        var timeFormatt = DateTimeFormatter.ofPattern("HH:mm");
        var start = LocalDateTime.of(
                req.startDate(),
                LocalTime.parse(req.startHour(),timeFormatt));
        var end = LocalDateTime.of(
                req.endDate(),
                LocalTime.parse(req.endHour(), timeFormatt));

        var now =LocalDateTime.now(SAO_PAULO);
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
                .withStatus(StatusRecord.UPDATED);
        var saved = timeRecordRepo.save(updated);
        return TimeRecordResponse.fromDomain(saved, Duration.ZERO);
    }

    @Override
    public List<TimeRecordResponse> listReport(UUID employeeId, String reference, Boolean active, StatusRecord status, LocalDate[] dates) {
        getEmployee(employeeId);

        String[] parts = reference.split(":");
        var duration = Duration.ofHours(Long.parseLong(parts[0]))
                .plusMinutes(Long.parseLong(parts[1]));

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

    private UUID getEmployee(UUID uuid) {
        employeeRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado: " + uuid));
        return uuid;
    }

}
