package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CompanyRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private CompanyRepository repository;

    @Test
    @DisplayName("findByActiveTrue/findByActiveFalse: deve separar ativos e inativos")
    void shouldFindCompaniesByActiveFlag() {
        repository.save(company("11111111000191", true));
        repository.save(company("22222222000191", false));

        assertEquals(1, repository.findByActiveTrue().size());
        assertEquals(1, repository.findByActiveFalse().size());
    }

    @Test
    @DisplayName("existsByCnpj/findByCnpj: deve localizar por CNPJ")
    void shouldFindAndCheckExistenceByCnpj() {
        repository.save(company("12345678000199", true));

        assertTrue(repository.existsByCnpj("12345678000199"));
        assertTrue(repository.findByCnpj("12345678000199").isPresent());
        assertFalse(repository.findByCnpj("99999999000199").isPresent());
    }

    @Test
    @DisplayName("deleteByCnpj: deve remover apenas a empresa do CNPJ informado")
    void shouldDeleteByCnpj() {
        repository.save(company("12345678000199", true));
        repository.save(company("99999999000199", true));
        repository.flush();

        repository.deleteByCnpj("12345678000199");
        repository.flush();

        assertFalse(repository.existsByCnpj("12345678000199"));
        assertTrue(repository.existsByCnpj("99999999000199"));
    }

    private CompanyEntity company(String cnpj, boolean active) {
        return CompanyEntity.builder()
                .id(UUID.randomUUID())
                .name("Kronos Tech")
                .cnpj(cnpj)
                .email("contato@kronos.com")
                .active(active)
                .address(AddressEmbeddable.builder()
                        .street("Rua A")
                        .number("10")
                        .postalCode("12345678")
                        .city("Rio")
                        .state("RJ")
                        .build())
                .latitude(-22.90)
                .longitude(-43.20)
                .build();
    }
}
