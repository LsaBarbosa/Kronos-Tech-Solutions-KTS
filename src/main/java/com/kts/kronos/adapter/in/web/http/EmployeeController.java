package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.*;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;
import static com.kts.kronos.constants.Messages.MANAGER;

@RestController
@RequestMapping(EMPLOYEE)
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGER)
    public void registerEmployee(@Valid @RequestBody CreateEmployeeRequest dto) {
        useCase.createEmployee(dto);
    }

    @GetMapping
    @PreAuthorize(MANAGER)
    public ResponseEntity<EmployeeListResponse> allEmployees(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var employees = useCase.listEmployees(active);
        return ResponseEntity.ok(new EmployeeListResponse(
                employees.stream().map(EmployeeResponse::fromDomain).toList()
        ));
    }
    @PreAuthorize(MANAGER)
    @GetMapping(EMPLOYEE_ID)
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID employeeId) {
        var employee = useCase.getEmployee(employeeId);
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee));
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(UPDATE_EMPLOYEE)
    @ResponseStatus(HttpStatus.OK)
    public void updateEmployee(@PathVariable UUID employeeId,
                               @Valid @RequestBody UpdateEmployeeManagerRequest dto
    ) {
        useCase.updateEmployee(employeeId, dto);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(OWN_PROFILE)
    public ResponseEntity<EmployeeResponse> getOwnProfile() {
        var employee = useCase.getOwnProfile();
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee));
    }
    @PreAuthorize(ANY_EMPLOYEE)
    @PatchMapping(UPDATE_OWN_PROFILE)
    @ResponseStatus(HttpStatus.OK)
    public void updateOwnProfile(@Valid @RequestBody UpdateEmployeePartnerRequest dto
    ) {
        useCase.updateOwnProfile(dto);
    }

    @PreAuthorize(MANAGER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(EMPLOYEE_ID)
    public void deleteEmployee(@PathVariable UUID employeeId) {
        useCase.deleteEmployee(employeeId);
    }
}