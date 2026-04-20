package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.projection.CompanyEmployeeCountsProjection;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class EmployeeRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private EmployeeRepository repository;

    @Test
    @DisplayName("countByCompanyIds: deve retornar ativos e inativos por empresa")
    void shouldCountEmployeesByCompanyIds() {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();

        repository.save(employee(companyA, "12345678909", true));
        repository.save(employee(companyA, "98765432100", false));
        repository.save(employee(companyB, "11144477735", true));

        List<CompanyEmployeeCountsProjection> result = repository.countByCompanyIds(List.of(companyA, companyB));

        assertEquals(2, result.size());

        CompanyEmployeeCountsProjection projectionA = result.stream()
                .filter(p -> p.getCompanyId().equals(companyA))
                .findFirst()
                .orElseThrow();

        CompanyEmployeeCountsProjection projectionB = result.stream()
                .filter(p -> p.getCompanyId().equals(companyB))
                .findFirst()
                .orElseThrow();

        assertEquals(1L, projectionA.getActiveCount());
        assertEquals(1L, projectionA.getInactiveCount());

        assertEquals(1L, projectionB.getActiveCount());
        assertEquals(0L, projectionB.getInactiveCount());
    }

    @Test
    @DisplayName("findByCompanyIdAndActive: deve filtrar ativos por empresa")
    void shouldFindByCompanyIdAndActive() {
        UUID companyId = UUID.randomUUID();

        repository.save(employee(companyId, "12345678909", true));
        repository.save(employee(companyId, "98765432100", false));

        assertEquals(1, repository.findByCompanyIdAndActive(companyId, true).size());
        assertEquals(1, repository.findByCompanyIdAndActive(companyId, false).size());
    }

    @Test
    @DisplayName("existsByCpf/findByCpf: deve localizar colaborador por CPF")
    void shouldFindAndCheckExistenceByCpf() {
        UUID companyId = UUID.randomUUID();
        repository.save(employee(companyId, "12345678909", true));

        assertTrue(repository.existsByCpf("12345678909"));
        assertTrue(repository.findByCpf("12345678909").isPresent());
        assertTrue(repository.findByCpf("00000000000").isEmpty());
    }

    @Test
    @DisplayName("findByCompanyId/countByCompanyIdAndActive: deve filtrar e contar por empresa")
    void shouldFindAndCountByCompanyId() {
        UUID companyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();

        repository.save(employee(companyId, "12345678909", true));
        repository.save(employee(companyId, "98765432100", false));
        repository.save(employee(otherCompanyId, "11144477735", true));

        assertEquals(2, repository.findByCompanyId(companyId).size());
        assertEquals(1, repository.countByCompanyIdAndActive(companyId, true));
        assertEquals(1, repository.countByCompanyIdAndActive(companyId, false));
    }

    @Test
    @DisplayName("deleteById: deve remover colaborador por id")
    void shouldDeleteById() {
        UUID companyId = UUID.randomUUID();
        EmployeeEntity saved = repository.save(employee(companyId, "12345678909", true));
        repository.flush();

        repository.deleteById(saved.getEmployeeId());
        repository.flush();

        assertTrue(repository.findById(saved.getEmployeeId()).isEmpty());
    }

    private EmployeeEntity employee(UUID companyId, String cpf, boolean active) {
        return EmployeeEntity.builder()
                .employeeId(UUID.randomUUID())
                .fullName("Employee " + cpf)
                .cpf(cpf)
                .pis(null)
                .jobPosition("Developer")
                .email(cpf + "@kronos.com")
                .salary(3000.0)
                .phone("21999999999")
                .active(active)
                .address(AddressEmbeddable.builder()
                        .street("Rua A")
                        .number("10")
                        .postalCode("12345678")
                        .city("Rio")
                        .state("RJ")
                        .build())
                .companyId(companyId)
                .homeOffice(false)
                .build();
    }
}
