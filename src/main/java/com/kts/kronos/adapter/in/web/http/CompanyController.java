package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.company.CompanyListResponse;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import static com.kts.kronos.constants.ApiPaths.COMPANIES;
import static com.kts.kronos.constants.ApiPaths.BY_CNPJ;
import static com.kts.kronos.constants.ApiPaths.TOGGLE_ACTIVATE_EMPLOYEE;
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(COMPANIES)
@RequiredArgsConstructor
@Tag(name = COMPANY_API, description = COMPANY_DESCRIPTION_API)
public class CompanyController {


    private final CompanyUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = CREATE, description = CREATE_DESCRIPTION)
    public void registerCompany(@Valid @RequestBody CreateCompanyRequest dto) {
        useCase.createCompany(dto);
    }

    @GetMapping(BY_CNPJ)
    @Operation(summary = GET_BY_PARAMETER, description = GET_COMPANY_DESCRIPTION)
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable String cnpj) {
        var company = useCase.getCompany(cnpj);
        return ResponseEntity.ok(CompanyResponse.fromDomain(company));
    }

    @GetMapping
    @Operation(summary = GET_ALL, description = ALL_OBJECTS_DESCRIPTION)
    public ResponseEntity<CompanyListResponse> allCompanies(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var companies = useCase.listCompanies(active);
        return ResponseEntity.ok(new CompanyListResponse(
                companies.stream().map(CompanyResponse::fromDomain).toList()
        ));
    }

    @PatchMapping(BY_CNPJ)
    @Operation(summary = UPDATE, description = UPDATE_DESCRIPTION)
    @ResponseStatus(HttpStatus.OK)
    public void updateCompany(@PathVariable String cnpj, @Valid @RequestBody UpdateCompanyRequest dto) {
        useCase.updateCompany(cnpj, dto);
    }

    @PatchMapping(TOGGLE_ACTIVATE_EMPLOYEE)
    @Operation(summary = TOGGLE, description = TOGGLE_DESCRIPTION)
    @ResponseStatus(HttpStatus.OK)
    public void toogleActivateCompany(@PathVariable String cnpj) {
        useCase.toggleActivate(cnpj);
    }

    @DeleteMapping(BY_CNPJ)
    @Operation(summary = DELETE, description = DELETE_COMPANY_DESCRIPTION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(@PathVariable String cnpj) {
        useCase.deleteByCnpj(cnpj);
    }

}
