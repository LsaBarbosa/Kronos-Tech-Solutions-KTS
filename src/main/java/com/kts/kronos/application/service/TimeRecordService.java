package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.CreateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
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
        var employeeId = req.employeeId();

        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee não encontrado: " + employeeId
                ));

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
        UUID empId = req.employeeId();
        employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado: " + empId));

        TimeRecord open = timeRecordRepo.findOpenByEmployeeId(empId)
                .orElseThrow(() -> new BadRequestException("Nenhum registro de check-in pendente encontrado"));

        var now = LocalDateTime.now(SAO_PAULO);
        TimeRecord updated = open
                .withCheckout(now)
                .withStatus(StatusRecord.CREATED);

        timeRecordRepo.save(updated);
    }

    @Override
    public List<TimeRecordResponse> listReport(UUID employeeId, String reference, Boolean active, StatusRecord status, LocalDate[] dates) {
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado: " + employeeId));

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

}
