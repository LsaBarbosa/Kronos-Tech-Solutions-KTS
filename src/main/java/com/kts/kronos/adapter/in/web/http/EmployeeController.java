package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.*;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.infrastructure.redis.RedisCacheNames;
import com.kts.kronos.infrastructure.redis.RedisScopeKeyResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.UUID;
import java.util.function.Supplier;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.*;

@RestController
@RequestMapping(EMPLOYEE)
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeUseCase useCase;
    private final CompanyUseCase companyUseCase;

    @Autowired(required = false)
    private CacheProvider cacheProvider;

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
        return ResponseEntity.ok(cache(
                RedisCacheNames.EMPLOYEE_LIST,
                RedisScopeKeyResolver.authenticatedScope("active=" + active),
                EmployeeListResponse.class,
                () -> buildEmployeeList(active)
        ));
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
        return ResponseEntity.ok(cache(
                RedisCacheNames.EMPLOYEE_OWN_PROFILE,
                RedisScopeKeyResolver.authenticatedScope(),
                EmployeeDetailResponse.class,
                () -> {
                    var profile = useCase.getOwnProfile();
                    var employee = profile.employee();
                    var role = profile.role();
                    var companyName = companyUseCase.getCompanyNameById(employee.companyId());
                    return EmployeeDetailResponse.fromDomain(employee, companyName, role);
                }
        ));
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
    public ResponseEntity<Void> checkCpfAvailability(@RequestParam String cpf) {
        if (useCase.cpfExists(cpf)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private EmployeeListResponse buildEmployeeList(Boolean active) {
        var employees = useCase.listEmployees(active);

        var employeeResponses = employees.stream().map(employee -> {
            String companyName = companyUseCase.getCompanyNameById(employee.companyId());

            return EmployeeListItemResponse.fromDomain(employee, companyName);
        }).toList();
        return new EmployeeListResponse(employeeResponses);
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, Supplier<T> loader) {
        if (cacheProvider == null) {
            return loader.get();
        }
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }


}
