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
    @PreAuthorize(ADMINISTRATOR)
    public ResponseEntity<EmployeeCreatedResponse> registerEmployee(@Valid @RequestBody CreateEmployeeRequest dto) {
        var create = useCase.createEmployee(dto);
        var companyName = companyUseCase.getCompanyNameById(create.companyId());
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(create.employeeId())
                .toUri();
        return ResponseEntity.created(location).body(EmployeeCreatedResponse.fromDomain(create, companyName));
    }

    @GetMapping
    @PreAuthorize(MANAGER)
    public ResponseEntity<EmployeeListResponse> allEmployees(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        return ResponseEntity.ok(useCase.listEmployeesResponse(active));
    }

    @GetMapping(FIND_BY_CPF)
    @PreAuthorize(ADMINISTRATOR)
    public ResponseEntity<EmployeeDetailResponse> findByCpfGlobal(@RequestParam String cpf) {
        return useCase.findByCpfGlobal(cpf)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(EMPLOYEES_BY_COMPANY)
    @PreAuthorize(KRONOS)
    public ResponseEntity<EmployeeListResponse> employeesByCompany(
            @PathVariable UUID companyId,
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        return ResponseEntity.ok(useCase.listEmployeesByCompany(companyId, active));
    }

    @PreAuthorize(MANAGER)
    @GetMapping(EMPLOYEE_ID)
    public ResponseEntity<EmployeeDetailResponse> getEmployee(@PathVariable UUID employeeId) {
        var employee = useCase.getEmployee(employeeId);
        var companyName = companyUseCase.getCompanyNameById(employee.companyId());
        return ResponseEntity.ok(EmployeeDetailResponse.fromDomain(employee, companyName, null));
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(UPDATE_EMPLOYEE)
    public ResponseEntity<Void> updateEmployee(@PathVariable UUID employeeId,
                               @Valid @RequestBody UpdateEmployeeManagerRequest dto
    ) {
        useCase.updateEmployee(employeeId, dto);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(OWN_PROFILE)
    public ResponseEntity<EmployeeDetailResponse> getOwnProfile() {
        return ResponseEntity.ok(useCase.getOwnProfileResponse());
    }
    @PreAuthorize(ANY_EMPLOYEE)
    @PatchMapping(UPDATE_OWN_PROFILE)
    public ResponseEntity<Void> updateOwnProfile(@Valid @RequestBody UpdateEmployeePartnerRequest dto
    ) {
        useCase.updateOwnProfile(dto);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(MANAGER)
    @PostMapping("/manager/{employeeId}/biometric-enrollment")
    public ResponseEntity<Void> enrollBiometricByManager(
            @PathVariable UUID employeeId,
            @Valid @RequestBody RegisterFaceRequest dto) {
        useCase.enrollBiometricByManager(employeeId, dto);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(MANAGER)
    @DeleteMapping(EMPLOYEE_ID)
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID employeeId) {
        useCase.deleteEmployee(employeeId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping(MESSAGES_SEEN)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<Void> markMessagesAsSeen() {
        useCase.markMessagesAsSeen();
        return ResponseEntity.noContent().build();
    }

    @GetMapping(CHECK_CPF)
    @PreAuthorize(ADMINISTRATOR)
    public ResponseEntity<Void> checkCpfAvailability(
            @RequestParam String cpf,
            @RequestParam(value = "companyId", required = false) UUID companyId
    ) {
        boolean exists = companyId != null
                ? useCase.cpfExistsInCompany(companyId, cpf)
                : useCase.cpfExistsInActiveCompany(cpf);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
