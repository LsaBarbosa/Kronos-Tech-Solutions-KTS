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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;


import static com.kts.kronos.constants.ApiPaths.COMPANIES;
import static com.kts.kronos.constants.ApiPaths.BY_CNPJ;
import static com.kts.kronos.constants.ApiPaths.TOGGLE_ACTIVATE_EMPLOYEE;

@RestController
@RequestMapping(COMPANIES)
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyUseCase useCase;

    @PostMapping
    public ResponseEntity<Void> registerCompany(@Valid @RequestBody CreateCompanyRequest dto) {
        useCase.createCompany(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping(BY_CNPJ)
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable String cnpj) {
        var company = useCase.getCompany(cnpj);
        return ResponseEntity.ok(CompanyResponse.fromDomain(company));
    }

    @GetMapping
    public ResponseEntity<CompanyListResponse> allCompanies(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var companies = useCase.listCompanies(active);
        return ResponseEntity.ok(new CompanyListResponse(
                companies.stream().map(CompanyResponse::fromDomain).toList()
        ));
    }

    @PatchMapping(BY_CNPJ)
    public ResponseEntity<Void> updateCompany(
            @PathVariable String cnpj,
            @Valid @RequestBody UpdateCompanyRequest dto
    ) {
        useCase.updateCompany(cnpj, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(TOGGLE_ACTIVATE_EMPLOYEE)
    public ResponseEntity<Void> deactivateCompany(@PathVariable String cnpj) {
        useCase.toggleActivate(cnpj);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(BY_CNPJ)
    public ResponseEntity<Void> deleteCompany(@PathVariable String cnpj) {
        useCase.deleteByCnpj(cnpj);
        return ResponseEntity.noContent().build();
    }

}
