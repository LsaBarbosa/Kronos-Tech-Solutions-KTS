package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.*;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Swagger.*;

@RestController
@RequestMapping(EMPLOYEE)
@RequiredArgsConstructor
@Tag(name = EMPLOYEE_API, description = EMPLOYEE_DESCRIPTION_API)
public class EmployeeController {
    private final EmployeeUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = CREATE, description = CREATE_DESCRIPTION)
    public void registerEmployee(@Valid @RequestBody CreateEmployeeRequest dto) {
        useCase.createEmployee(dto);
    }

    @GetMapping
    @Operation(summary = GET_ALL, description = ALL_OBJECTS_DESCRIPTION)
    public ResponseEntity<EmployeeListResponse> allEmployees(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var employees = useCase.listEmployees(active);
        return ResponseEntity.ok(new EmployeeListResponse(
                employees.stream().map(EmployeeResponse::fromDomain).toList()
        ));
    }

    @GetMapping(EMPLOYEE_ID)
    @Operation(summary = GET_BY_PARAMETER, description = EMPLOYEE_DESCRIPTION)
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID employeeId) {
        var employee = useCase.getEmployee(employeeId);
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee));
    }

    @PatchMapping(UPDATE_EMPLOYEE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = UPDATE, description = UPDATE_DESCRIPTION)
    public void updateEmployee(@PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeManagerRequest dto) {
        useCase.updateEmployee(employeeId, dto);
    }

    @PatchMapping(UPDATE_OWN_PROFILE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = UPDATE, description = UPDATE_DESCRIPTION)
    public void updateOwnProfile(@PathVariable UUID employeeId,
                                 @Valid @RequestBody UpdateEmployeePartnerRequest dto
    ) {
        useCase.updateOwnProfile(employeeId, dto);
    }

    @DeleteMapping(EMPLOYEE_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = DELETE, description = DELETEE_EMPLOYEE_DESCRIPTION)
    public void deleteEmployee(@PathVariable UUID id) {
        useCase.deleteEmployee(id);
    }
}