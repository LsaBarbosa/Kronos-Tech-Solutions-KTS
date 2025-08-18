package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.company.CompanyListResponse;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import static com.kts.kronos.constants.ApiPaths.COMPANIES;
import static com.kts.kronos.constants.ApiPaths.BY_CNPJ;
import static com.kts.kronos.constants.ApiPaths.TOGGLE_ACTIVATE_EMPLOYEE;

@RestController
@RequestMapping(COMPANIES)
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CTO')")
    public void registerCompany(@Valid @RequestBody CreateCompanyRequest dto) {
        useCase.createCompany(dto);
    }

    @PreAuthorize("hasRole('CTO')")
    @GetMapping(BY_CNPJ)
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable String cnpj) {
        var company = useCase.getCompany(cnpj);
        return ResponseEntity.ok(CompanyResponse.fromDomain(company));
    }

    @PreAuthorize("hasRole('CTO')")
    @GetMapping
    public ResponseEntity<CompanyListResponse> allCompanies(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var companies = useCase.listCompanies(active);
        return ResponseEntity.ok(new CompanyListResponse(
                companies.stream().map(CompanyResponse::fromDomain).toList()
        ));
    }

    @PreAuthorize("hasRole('CTO')")
    @PatchMapping(BY_CNPJ)
    @ResponseStatus(HttpStatus.OK)
    public void updateCompany(
            @PathVariable String cnpj,
            @Valid @RequestBody UpdateCompanyRequest dto
    ) {
        useCase.updateCompany(cnpj, dto);
    }

    @PreAuthorize("hasRole('CTO')")
    @PatchMapping(TOGGLE_ACTIVATE_EMPLOYEE)
    @ResponseStatus(HttpStatus.OK)
    public void deactivateCompany(@PathVariable String cnpj) {
        useCase.toggleActivate(cnpj);
    }

    @PreAuthorize("hasRole('CTO')")
    @DeleteMapping(BY_CNPJ)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(@PathVariable String cnpj) {
        useCase.deleteByCnpj(cnpj);
    }

}
