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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping(EMPLOYEE_ID)
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID employeeId) {
        var employee = useCase.getEmployee(employeeId);
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee));
    }

    @PatchMapping(UPDATE_EMPLOYEE)
    public ResponseEntity<Void> updateEmployee(@PathVariable UUID employeeId,
                                               @Valid @RequestBody UpdateEmployeeManagerRequest dto
    ) {
        useCase.updateEmployee(employeeId, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(UPDATE_OWN_PROFILE)
    public ResponseEntity<Void> updateOwnProfile(@PathVariable UUID employeeId,
                                                 @Valid @RequestBody UpdateEmployeePartnerRequest dto
    ) {
        useCase.updateOwnProfile(employeeId, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(EMPLOYEE_ID)
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
        useCase.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}