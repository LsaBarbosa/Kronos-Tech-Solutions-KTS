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
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeRecordService implements TimeRecordUseCase {

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

        var nowSp = getLocalDateTime();
        TimeRecord tr = new TimeRecord(
                null,
                nowSp,
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

        var now = getLocalDateTime();
        TimeRecord updated = open
                .withCheckout(now)
                .withStatus(StatusRecord.CREATED);

        timeRecordRepo.save(updated);
    }

    @Override
    public List<TimeRecordResponse> listReport(UUID employeeId, String reference, Boolean active) {
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado: " + employeeId));

        String[] parts = reference.split(":");
        Duration refDur = Duration.ofHours(Long.parseLong(parts[0]))
                .plusMinutes(Long.parseLong(parts[1]));

        var recs = active == null
                ? timeRecordRepo.findByEmployeeId(employeeId)
                : timeRecordRepo.findByEmployeeIdAndActive(employeeId, active);

        return recs.stream()
                .map(tr -> TimeRecordResponse.fromDomain(tr, refDur))
                .toList();
    }

    private static LocalDateTime getLocalDateTime() {
        return LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }
}
