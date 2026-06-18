package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.company.CompanyListResponse;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
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

import java.util.function.Supplier;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.KRONOS;

@RestController
@RequestMapping(COMPANIES)
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyUseCase useCase;

    @Autowired(required = false)
    private CacheProvider cacheProvider;

    @PostMapping
    @PreAuthorize(KRONOS)
    public ResponseEntity<Void> registerCompany(@Valid @RequestBody CreateCompanyRequest dto) {
        useCase.createCompany(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize(KRONOS)
    @GetMapping(BY_CNPJ)
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable String cnpj) {
        return ResponseEntity.ok(cache(
                RedisCacheNames.COMPANY_GET,
                RedisScopeKeyResolver.authenticatedScope("cnpj=" + cnpj),
                CompanyResponse.class,
                () -> CompanyResponse.fromDomain(useCase.getCompany(cnpj))
        ));
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
    public ResponseEntity<Void> updateCompany(
            @PathVariable String cnpj,
            @Valid @RequestBody UpdateCompanyRequest dto
    ) {
        useCase.updateCompany(cnpj, dto);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(KRONOS)
    @PatchMapping(TOGGLE_ACTIVATE)
    public ResponseEntity<Void> deactivateCompany(@PathVariable String cnpj) {
        useCase.toggleActivate(cnpj);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(KRONOS)
    @DeleteMapping(BY_CNPJ)
    public ResponseEntity<Void> deleteCompany(@PathVariable String cnpj) {
        useCase.deleteByCnpj(cnpj);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(CHECK_CNPJ)
    @PreAuthorize(KRONOS)
    public ResponseEntity<Void> checkCnpjAvailability(@RequestParam String cnpj) {
        if (useCase.cnpjExists(cnpj)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, Supplier<T> loader) {
        if (cacheProvider == null) {
            return loader.get();
        }
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }
}
