package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.CompanyListResponse;
import com.kts.kronos.adapter.in.web.dto.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.UpdateCompanyRequest;
import com.kts.kronos.app.port.in.usecase.CompanyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyUseCase useCase;

    @PostMapping
    public ResponseEntity<Void> createCompany(
            @Valid @RequestBody CreateCompanyRequest dto
    ) {
        useCase.createCompany(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{cnpj}")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable String cnpj) {
        var company = useCase.getCompany(cnpj);
        return ResponseEntity.ok(CompanyResponse.fromDomain(company));
    }

    @GetMapping
    public ResponseEntity<CompanyListResponse> listCompanies(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var companies = useCase.listCompanies(active);
        return ResponseEntity.ok(new CompanyListResponse(
                companies.stream().map(CompanyResponse::fromDomain).toList()
        ));
    }

    @PatchMapping("/{cnpj}")
    public ResponseEntity<Void> updateCompany(
            @PathVariable String cnpj,
            @Valid @RequestBody UpdateCompanyRequest dto
    ) {
        var cmd = UpdateCompanyRequest.toCommand(dto);
        useCase.updateCompany(cnpj, cmd);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{cnpj}/deactivate")
    public ResponseEntity<Void> deactivateCompany(@PathVariable String cnpj) {
        useCase.deactivateCompany(cnpj);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{cnpj}")
    public ResponseEntity<Void> deleteCompany(@PathVariable String cnpj) {
        useCase.deleteByCnpj(cnpj);
        return ResponseEntity.noContent().build();
    }

}
