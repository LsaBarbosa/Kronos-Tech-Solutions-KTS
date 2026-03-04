package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.company.CompanyListResponse;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(COMPANIES)
@RequiredArgsConstructor
@Tag(name = SWAGGER_COMPANY_TAG, description = SWAGGER_COMPANY_DESC)
public class CompanyController {

    private final CompanyUseCase useCase;

    @Operation(summary = REG_COMPANY_SUMMARY, description = REG_COMPANY_DESC)
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = REG_COMPANY_SUCCESS), @ApiResponse(responseCode = "400", description = REG_COMPANY_400), @ApiResponse(responseCode = "403", description = REG_COMPANY_403)})
    @PostMapping
    @PreAuthorize(KRONOS)
    public ResponseEntity<Void> registerCompany(@Valid @RequestBody CreateCompanyRequest dto) {
        useCase.createCompany(dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = GET_COMPANY_SUMMARY, description = GET_COMPANY_DESC)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = GET_COMPANY_SUCCESS), @ApiResponse(responseCode = "404", description = GET_COMPANY_404), @ApiResponse(responseCode = "403", description = ACCESS_DENIED)})
    @PreAuthorize(KRONOS)
    @GetMapping(BY_CNPJ)
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable String cnpj) {
        var company = useCase.getCompany(cnpj);
        return ResponseEntity.ok(CompanyResponse.fromDomain(company));
    }

    @Operation(summary = LIST_COMPANY_SUMMARY, description = LIST_COMPANY_DESC)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = LIST_SUCCESS), @ApiResponse(responseCode = "403", description = ACCESS_DENIED)})
    @PreAuthorize(KRONOS)
    @GetMapping
    public ResponseEntity<CompanyListResponse> allCompanies(@RequestParam(value = "active", required = false) Boolean active) {
        var companies = useCase.listCompanies(active);
        return ResponseEntity.ok(new CompanyListResponse(companies.stream().map(CompanyResponse::fromDomain).toList()));
    }

    @Operation(summary = UPDATE_COMPANY_SUMMARY, description = UPDATE_COMPANY_DESC)
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = UPDATE_COMPANY_SUCCESS), @ApiResponse(responseCode = "400", description = UPDATE_COMPANY_400), @ApiResponse(responseCode = "404", description = UPDATE_COMPANY_404), @ApiResponse(responseCode = "403", description = ACCESS_DENIED)})
    @PreAuthorize(KRONOS)
    @PatchMapping(BY_CNPJ)
    public ResponseEntity<Void> updateCompany(@PathVariable String cnpj, @Valid @RequestBody UpdateCompanyRequest dto) {
        useCase.updateCompany(cnpj, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = TOGGLE_COMPANY_SUMMARY, description = TOGGLE_COMPANY_DESC)
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = TOGGLE_COMPANY_SUCCESS), @ApiResponse(responseCode = "404", description = UPDATE_COMPANY_404), @ApiResponse(responseCode = "403", description = ACCESS_DENIED)})
    @PreAuthorize(KRONOS)
    @PatchMapping(TOGGLE_ACTIVATE)
    public ResponseEntity<Void> deactivateCompany(@PathVariable String cnpj) {
        useCase.toggleActivate(cnpj);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = DEL_COMPANY_SUMMARY, description = DEL_COMPANY_DESC)
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = DEL_COMPANY_SUCCESS), @ApiResponse(responseCode = "404", description = UPDATE_COMPANY_404), @ApiResponse(responseCode = "403", description = ACCESS_DENIED)})
    @PreAuthorize(KRONOS)
    @DeleteMapping(BY_CNPJ)
    public ResponseEntity<Void> deleteCompany(@PathVariable String cnpj) {
        useCase.deleteByCnpj(cnpj);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = CHECK_CNPJ_SUMMARY, description = CHECK_CNPJ_DESC)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = CHECK_CNPJ_200), @ApiResponse(responseCode = "404", description = CHECK_CNPJ_404)})
    @GetMapping(CHECK_CNPJ)
    public ResponseEntity<Void> checkCnpjAvailability(@RequestParam String cnpj) {
        if (useCase.cnpjExists(cnpj)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
