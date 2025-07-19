package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeListResponse;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeResponse;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.kts.kronos.constants.ApiPaths.EMPLOYEE;
import static com.kts.kronos.constants.ApiPaths.EMPLOYEE_ID;
import static com.kts.kronos.constants.ApiPaths.UPDATE_EMPLOYEE;
import static com.kts.kronos.constants.ApiPaths.UPDATE_OWN_PROFILE;

import java.util.UUID;

@RestController
@RequestMapping(EMPLOYEE)
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void registerEmployee(@Valid @RequestBody CreateEmployeeRequest dto) {
        useCase.createEmployee(dto);
    }

    @GetMapping
    public ResponseEntity<EmployeeListResponse> allEmployees(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var employees = useCase.listEmployees(active);
        return ResponseEntity.ok(new EmployeeListResponse(
                employees.stream().map(EmployeeResponse::fromDomain).toList()
        ));
    }

    @GetMapping(EMPLOYEE_ID)
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID employeeId) {
        var employee = useCase.getEmployee(employeeId);
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee));
    }

    @PatchMapping(UPDATE_EMPLOYEE)
    @ResponseStatus(HttpStatus.OK)
    public void updateEmployee(@PathVariable UUID employeeId,
                               @Valid @RequestBody UpdateEmployeeManagerRequest dto
    ) {
        useCase.updateEmployee(employeeId, dto);
    }

    @PatchMapping(UPDATE_OWN_PROFILE)
    @ResponseStatus(HttpStatus.OK)
    public void updateOwnProfile(@PathVariable UUID employeeId,
                                                 @Valid @RequestBody UpdateEmployeePartnerRequest dto
    ) {
        useCase.updateOwnProfile(employeeId, dto);
    }

    @DeleteMapping(EMPLOYEE_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable UUID id) {
        useCase.deleteEmployee(id);
    }
}