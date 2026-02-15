package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.*;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(EMPLOYEE)
@RequiredArgsConstructor
@Tag(name = SWAGGER_EMPLOYEE_TAG, description = SWAGGER_EMPLOYEE_DESC)
public class EmployeeController {

    private final EmployeeUseCase useCase;
    private final CompanyUseCase companyUseCase;

    @Operation(summary = REG_EMPLOYEE_SUMMARY, description = REG_EMPLOYEE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = REG_EMPLOYEE_201),
            @ApiResponse(responseCode = "400", description = REG_EMPLOYEE_400),
            @ApiResponse(responseCode = "403", description = REG_EMPLOYEE_403)
    })
    @PostMapping
    @PreAuthorize(ADMINISTRATOR)
    public ResponseEntity<EmployeeResponse> registerEmployee(@Valid @RequestBody CreateEmployeeRequest dto) {
        var create = useCase.createEmployee(dto);
        var companyName = companyUseCase.getCompanyNameById(create.companyId());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(create.employeeId()).toUri();
        return ResponseEntity.created(location).body(EmployeeResponse.fromDomain(create, companyName, null));
    }

    @Operation(summary = LIST_EMPLOYEE_SUMMARY, description = LIST_EMPLOYEE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = LIST_SUCCESS),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    @GetMapping
    @PreAuthorize(MANAGER)
    public ResponseEntity<EmployeeListResponse> allEmployees(@RequestParam(value = "active", required = false) Boolean active) {
        var employees = useCase.listEmployees(active);

        var employeeResponses = employees.stream().map(employee -> {
            var companyName = companyUseCase.getCompanyNameById(employee.companyId());

            return EmployeeResponse.fromDomain(employee, companyName, null);
        }).toList();
        return ResponseEntity.ok(new EmployeeListResponse(employeeResponses));
    }

    @Operation(summary = GET_EMPLOYEE_SUMMARY, description = GET_EMPLOYEE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = GET_EMPLOYEE_SUCCESS),
            @ApiResponse(responseCode = "404", description = GET_EMPLOYEE_404),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    @PreAuthorize(MANAGER)
    @GetMapping(EMPLOYEE_ID)
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID employeeId) {
        var employee = useCase.getEmployee(employeeId);
        var companyName = companyUseCase.getCompanyNameById(employee.companyId());
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee, companyName, null));
    }

    @Operation(summary = UPDATE_EMPLOYEE_SUMMARY, description = UPDATE_EMPLOYEE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = UPDATE_EMPLOYEE_SUCCESS),
            @ApiResponse(responseCode = "400", description = UPDATE_EMPLOYEE_400),
            @ApiResponse(responseCode = "404", description = SWAGGER_EMP_NOT_FOUND),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    @PreAuthorize(MANAGER)
    @PatchMapping(UPDATE_EMPLOYEE)
    public void updateEmployee(@PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeManagerRequest dto) {
        useCase.updateEmployee(employeeId, dto);
    }

    @Operation(summary = OWN_PROFILE_SUMMARY, description = OWN_PROFILE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = OWN_PROFILE_SUCCESS),
            @ApiResponse(responseCode = "404", description = OWN_PROFILE_404)
    })
    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(OWN_PROFILE)
    public ResponseEntity<EmployeeResponse> getOwnProfile() {
        var profile = useCase.getOwnProfile();
        var employee = profile.employee();
        var role = profile.role();
        var companyName = companyUseCase.getCompanyNameById(employee.companyId());
        return ResponseEntity.ok(EmployeeResponse.fromDomain(employee, companyName, role));
    }

    @Operation(summary = UPDATE_OWN_PROFILE_SUMMARY, description = UPDATE_OWN_PROFILE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = UPDATE_OWN_PROFILE_SUCCESS),
            @ApiResponse(responseCode = "404", description = SWAGGER_EMP_NOT_FOUND)
    })
    @PreAuthorize(ANY_EMPLOYEE)
    @PatchMapping(UPDATE_OWN_PROFILE)
    public void updateOwnProfile(@Valid @RequestBody UpdateEmployeePartnerRequest dto) {
        useCase.updateOwnProfile(dto);
    }

    @Operation(summary = DEL_EMPLOYEE_SUMMARY, description = DEL_EMPLOYEE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = DEL_EMPLOYEE_SUCCESS),
            @ApiResponse(responseCode = "404", description = SWAGGER_EMP_NOT_FOUND),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    @PreAuthorize(MANAGER)
    @DeleteMapping(EMPLOYEE_ID)
    public void deleteEmployee(@PathVariable UUID employeeId) {
        useCase.deleteEmployee(employeeId);
    }

    @Operation(summary = MARK_MSG_SEEN_SUMMARY, description = MARK_MSG_SEEN_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = MARK_MSG_SEEN_SUCCESS),
            @ApiResponse(responseCode = "404", description = SWAGGER_EMP_NOT_FOUND)
    })
    @PostMapping(MESSAGES_SEEN)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize(ANY_EMPLOYEE)
    public void markMessagesAsSeen() {
        useCase.markMessagesAsSeen();
    }

    @Operation(summary = CHECK_CPF_SUMMARY, description = CHECK_CPF_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = CHECK_CPF_200),
            @ApiResponse(responseCode = "404", description = CHECK_CPF_404)
    })
    @GetMapping(CHECK_CPF)
    public ResponseEntity<Void> checkCpfAvailability(@RequestParam String cpf) {
        if (useCase.cpfExists(cpf)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
