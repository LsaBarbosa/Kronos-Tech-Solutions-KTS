package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.CreateScheduleExceptionRequest;
import com.kts.kronos.adapter.in.web.dto.employee.ScheduleExceptionResponse;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.ScheduleExceptionProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.ScheduleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleExceptionService {

    private final ScheduleExceptionProvider exceptionProvider;
    private final EmployeeProvider employeeProvider;
    private final DomainAuthorizationService domainAuthorizationService;

    public ScheduleExceptionResponse create(UUID employeeId, CreateScheduleExceptionRequest req) {
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        domainAuthorizationService.authorizeCompanyAccess(employee.companyId());

        if (!req.isDayOff() && (req.workStartTime() == null || req.workEndTime() == null)) {
            throw new BadRequestException("Exceção de trabalho requer horário de início e fim.");
        }

        if (exceptionProvider.existsByEmployeeAndDate(employeeId, req.exceptionDate())) {
            throw new ConflictException("Já existe uma exceção para esta data.");
        }

        var exception = new ScheduleException(
                UUID.randomUUID(),
                employeeId,
                req.exceptionDate(),
                req.workStartTime(),
                req.workEndTime(),
                req.breakStartTime(),
                req.breakEndTime(),
                req.isDayOff(),
                req.description()
        );

        return ScheduleExceptionResponse.fromDomain(exceptionProvider.save(exception));
    }

    @Transactional(readOnly = true)
    public List<ScheduleExceptionResponse> listByMonth(UUID employeeId, int year, int month) {
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        domainAuthorizationService.authorizeCompanyAccess(employee.companyId());

        var ym = YearMonth.of(year, month);
        return exceptionProvider.findByEmployeeAndMonth(employeeId, ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .map(ScheduleExceptionResponse::fromDomain)
                .toList();
    }

    public void delete(UUID employeeId, LocalDate date) {
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        domainAuthorizationService.authorizeCompanyAccess(employee.companyId());

        if (!exceptionProvider.existsByEmployeeAndDate(employeeId, date)) {
            throw new ResourceNotFoundException("Exceção não encontrada para esta data.");
        }

        exceptionProvider.deleteByEmployeeAndDate(employeeId, date);
    }
}
