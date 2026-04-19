package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AfdEntryEntity;
import com.kts.kronos.support.jpa.AbstractPostgresContainerTest;
import com.kts.kronos.support.jpa.PostgresDataJpaTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@PostgresDataJpaTest
class AfdEntryRepositoryDataJpaTest extends AbstractPostgresContainerTest {

    @Autowired
    private AfdEntryRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findLastHashByCompanyId: deve retornar o hash do registro com maior NSR da empresa")
    void shouldReturnLastHashByCompanyIdWhenRecordsExist() {
        UUID targetCompanyId = UUID.randomUUID();
        UUID anotherCompanyId = UUID.randomUUID();

        repository.save(buildEntry(targetCompanyId, 1L, "hash-001"));
        repository.save(buildEntry(targetCompanyId, 3L, "hash-003"));
        repository.save(buildEntry(targetCompanyId, 2L, "hash-002"));
        repository.save(buildEntry(anotherCompanyId, 99L, "hash-other"));

        entityManager.flush();
        entityManager.clear();

        Optional<String> result = repository.findLastHashByCompanyId(targetCompanyId);

        assertTrue(result.isPresent());
        assertEquals("hash-003", result.get());
    }

    @Test
    @DisplayName("findLastHashByCompanyId: deve retornar vazio quando a empresa nao possui registros")
    void shouldReturnEmptyWhenCompanyHasNoRecords() {
        Optional<String> result = repository.findLastHashByCompanyId(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("streamAllByCompanyIdOrderByNsrAsc: deve retornar stream ordenado por NSR ascendente")
    void shouldStreamEntriesOrderedByNsrAsc() {
        UUID targetCompanyId = UUID.randomUUID();
        UUID anotherCompanyId = UUID.randomUUID();

        repository.save(buildEntry(targetCompanyId, 30L, "hash-030"));
        repository.save(buildEntry(targetCompanyId, 10L, "hash-010"));
        repository.save(buildEntry(targetCompanyId, 20L, "hash-020"));
        repository.save(buildEntry(anotherCompanyId, 1L, "hash-other"));

        entityManager.flush();
        entityManager.clear();

        try (Stream<AfdEntryEntity> stream = repository.streamAllByCompanyIdOrderByNsrAsc(targetCompanyId)) {
            List<Long> nsrs = stream.map(AfdEntryEntity::getNsr).toList();

            assertEquals(List.of(10L, 20L, 30L), nsrs);
        }
    }

    @Test
    @DisplayName("streamAllByCompanyIdOrderByNsrAsc: deve permitir leitura incremental em cenario minimo")
    void shouldAllowIncrementalReadingFromStream() {
        UUID companyId = UUID.randomUUID();

        repository.save(buildEntry(companyId, 1L, "hash-001"));
        repository.save(buildEntry(companyId, 2L, "hash-002"));
        repository.save(buildEntry(companyId, 3L, "hash-003"));

        entityManager.flush();
        entityManager.clear();

        try (Stream<AfdEntryEntity> stream = repository.streamAllByCompanyIdOrderByNsrAsc(companyId)) {
            var iterator = stream.iterator();

            assertTrue(iterator.hasNext());
            assertEquals(1L, iterator.next().getNsr());

            assertTrue(iterator.hasNext());
            assertEquals(2L, iterator.next().getNsr());
        }
    }

    private AfdEntryEntity buildEntry(UUID companyId, Long nsr, String currentHash) {
        return AfdEntryEntity.builder()
                .nsr(nsr)
                .recordType("7")
                .recordDate(LocalDateTime.of(2026, 1, 1, 8, 0).plusMinutes(nsr))
                .employeeCpf(String.format("%011d", nsr))
                .employeePis("12345678901")
                .companyId(companyId)
                .employeeId(UUID.randomUUID())
                .previousHash("prev-" + nsr)
                .currentHash(currentHash)
                .build();
    }
}