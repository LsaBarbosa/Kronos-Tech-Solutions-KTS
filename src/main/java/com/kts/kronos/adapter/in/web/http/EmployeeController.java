package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.*;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.*;

@RestController
@RequestMapping(EMPLOYEE)
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeUseCase useCase;
    private final CompanyUseCase companyUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATOR)
    public ResponseEntity<EmployeeResponse> registerEmployee(@Valid @RequestBody CreateEmployeeRequest dto) {
        var create = useCase.createEmployee(dto);
        var companyName = companyUseCase.getCompanyNameById(create.companyId());
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(create.employeeId())
                .toUri();
        return ResponseEntity.created(location).body(EmployeeResponse.fromDomain(create, companyName));
    }

    @GetMapping
    @PreAuthorize(MANAGER)
    public ResponseEntity<EmployeeListResponse> allEmployees(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var employees = useCase.listEmployees(active);

        var employeeResponses = employees.stream().map(employee -> {
            // 1. Busca o nome da empresa usando o CompanyService
            String companyName = companyUseCase.getCompanyNameById(employee.companyId());

            // 2. Mapeia para o DTO, passando o nome da empresa
            return EmployeeResponse.fromDomain(employee, companyName);
        }).toList();
        return ResponseEntity.ok(new EmployeeListResponse(employeeResponses));
    }

    @PreAuthorize(MANAGER)
    @GetMapping(EMPLOYEE_ID)
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID employeeId) {
        var employee = useCase.getEmployee(employeeId);
        var companyName = companyUseCase.getCompanyNameById(employee.companyId());
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee, companyName));
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
        var companyName = companyUseCase.getCompanyNameById(employee.companyId());
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee,companyName));
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
