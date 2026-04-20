package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyNsrRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private CompanyNsrRepository repository;

    @Test
    @DisplayName("incrementAndGetNsr: deve iniciar em 1 e incrementar atomicamente")
    void shouldIncrementNsrUsingPostgresUpsert() {
        UUID companyId = UUID.randomUUID();

        Long first = repository.incrementAndGetNsr(companyId);
        Long second = repository.incrementAndGetNsr(companyId);
        Long third = repository.incrementAndGetNsr(companyId);

        assertEquals(1L, first);
        assertEquals(2L, second);
        assertEquals(3L, third);
    }
}