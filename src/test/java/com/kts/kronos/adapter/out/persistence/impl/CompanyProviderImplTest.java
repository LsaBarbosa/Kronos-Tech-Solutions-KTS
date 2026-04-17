package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.out.persistence.CompanyRepository;
import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProviderImplTest {

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private CompanyProviderImpl provider;

    @Test
    void deveBuscarPorCnpjMapeandoParaDominio() {
        when(repository.findByCnpj("12345678000199")).thenReturn(Optional.of(companyEntity(true)));

        var result = provider.findByCnpj("12345678000199");

        assertEquals("KTS", result.orElseThrow().name());
    }

    @Test
    void deveEscolherRepositorioCorretoNoFindByActive() {
        when(repository.findByActiveTrue()).thenReturn(List.of(companyEntity(true)));
        when(repository.findByActiveFalse()).thenReturn(List.of(companyEntity(false)));

        assertEquals(1, provider.findByActive(true).size());
        assertEquals(1, provider.findByActive(false).size());

        verify(repository).findByActiveTrue();
        verify(repository).findByActiveFalse();
    }

    @Test
    void deveSalvarEmpresaConvertendoEntidade() {
        var company = new Company(
                UUID.randomUUID(),
                "KTS",
                "12345678000199",
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "São Luís", "MA"),
                new Location(-2.53, -44.30),
                0,
                0
        );

        when(repository.save(org.mockito.ArgumentMatchers.any(CompanyEntity.class)))
                .thenReturn(companyEntity(true));

        provider.save(company);

        verify(repository).save(org.mockito.ArgumentMatchers.any(CompanyEntity.class));
    }

    private static CompanyEntity companyEntity(boolean active) {
        return CompanyEntity.builder()
                .id(UUID.randomUUID())
                .name("KTS")
                .cnpj("12345678000199")
                .email("contato@kts.com")
                .active(active)
                .address(AddressEmbeddable.builder()
                        .street("Rua A")
                        .number("10")
                        .postalCode("65000000")
                        .city("São Luís")
                        .state("MA")
                        .build())
                .latitude(-2.53)
                .longitude(-44.30)
                .build();
    }
}