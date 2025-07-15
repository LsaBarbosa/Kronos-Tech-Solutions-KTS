package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.*;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeUseCase useCase;

    @PostMapping
    public ResponseEntity<Void> registerEmployee(@Valid @RequestBody CreateEmployeeRequest dto) {
        useCase.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
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

    @GetMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID employeeId) {
        var employee = useCase.getEmployee(employeeId);
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee));
    }

    @PatchMapping("/manager/update-employee/{employeeId}")
    public ResponseEntity<Void> updateEmployee(@PathVariable UUID employeeId,
                                               @Valid @RequestBody UpdateEmployeeManagerRequest dto
    ) {
        useCase.updateEmployee(employeeId, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/manager/update-own-profile/{employeeId}")
    public ResponseEntity<Void> updateOwnProfile(@PathVariable UUID employeeId,
                                                 @Valid @RequestBody UpdateEmployeePartnerRequest dto
    ) {
        useCase.updateOwnProfile(employeeId, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cnpj}")
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID id) {
        useCase.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}