package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.company.CompanyListResponse;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.KRONOS;

@RestController
@RequestMapping(COMPANIES)
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyUseCase useCase;

    @PostMapping
    @PreAuthorize(KRONOS)
    public void registerCompany(@Valid @RequestBody CreateCompanyRequest dto) {

        useCase.createCompany(dto);
    }

    @PreAuthorize(KRONOS)
    @GetMapping(BY_CNPJ)
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable String cnpj) {
        var company = useCase.getCompany(cnpj);
        return ResponseEntity.ok(CompanyResponse.fromDomain(company));
    }

    @PreAuthorize(KRONOS)
    @GetMapping
    public ResponseEntity<CompanyListResponse> allCompanies(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var companies = useCase.listCompanies(active);
        return ResponseEntity.ok(new CompanyListResponse(
                companies.stream().map(CompanyResponse::fromDomain).toList()
        ));
    }

    @PreAuthorize(KRONOS)
    @PatchMapping(BY_CNPJ)
    public void updateCompany(
            @PathVariable String cnpj,
            @Valid @RequestBody UpdateCompanyRequest dto
    ) {
        useCase.updateCompany(cnpj, dto);
    }

    @PreAuthorize(KRONOS)
    @PatchMapping(TOGGLE_ACTIVATE)
    public void deactivateCompany(@PathVariable String cnpj) {
        useCase.toggleActivate(cnpj);
    }

    @PreAuthorize(KRONOS)
    @DeleteMapping(BY_CNPJ)
    public void deleteCompany(@PathVariable String cnpj) {
        useCase.deleteByCnpj(cnpj);
    }

    @GetMapping(CHECK_CNPJ)
    public ResponseEntity<Void> checkCnpjAvailability(@RequestParam String cnpj) {
        if (useCase.cnpjExists(cnpj)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
