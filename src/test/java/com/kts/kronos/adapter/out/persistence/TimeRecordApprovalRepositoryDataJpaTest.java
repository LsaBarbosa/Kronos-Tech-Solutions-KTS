package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordApprovalEntity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TimeRecordApprovalRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private TimeRecordApprovalRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void deveBuscarAprovacoesPorCompanyIdEEmployeeName() {
        UUID companyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();

        UUID anaId = persistEmployee(companyId, "Ana Paula", "12345678901");
        UUID brunoId = persistEmployee(companyId, "Bruno Silva", "22345678901");
        UUID outraEmpresaId = persistEmployee(otherCompanyId, "Ana Outra", "32345678901");

        repository.save(approval(1L, anaId, LocalDateTime.now().minusMinutes(5)));
        repository.save(approval(2L, brunoId, LocalDateTime.now().minusMinutes(3)));
        repository.save(approval(3L, outraEmpresaId, LocalDateTime.now().minusMinutes(1)));

        var page = repository.findAllByCompanyId(
                PageRequest.of(0, 10),
                companyId,
                "%ana%"
        );

        assertEquals(1, page.getContent().size());
        assertEquals(1L, page.getContent().getFirst().getTimeRecordId());
    }

    @Test
    void deveBuscarAprovacoesPorCompanyIdSemFiltroOrdenandoPorCriacaoDesc() {
        UUID companyId = UUID.randomUUID();
        UUID anaId = persistEmployee(companyId, "Ana Paula", "52345678901");
        UUID brunoId = persistEmployee(companyId, "Bruno Silva", "62345678901");

        repository.save(approval(20L, anaId, LocalDateTime.now().minusMinutes(10)));
        repository.save(approval(21L, brunoId, LocalDateTime.now().minusMinutes(1)));

        var page = repository.findAllByCompanyId(PageRequest.of(0, 1), companyId, null);

        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals(21L, page.getContent().getFirst().getTimeRecordId());
    }

    @Test
    void deveRemoverAprovacoesAntigas() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = persistEmployee(companyId, "Carlos", "42345678901");

        repository.save(approval(10L, employeeId, LocalDateTime.now()));
        repository.save(approval(11L, employeeId, LocalDateTime.now()));
        repository.flush();

        em.getEntityManager()
                .createQuery("""
                    UPDATE TimeRecordApprovalEntity t
                       SET t.createdAt = :createdAt
                     WHERE t.timeRecordId = :timeRecordId
                """)
                .setParameter("createdAt", LocalDateTime.now().minusDays(40))
                .setParameter("timeRecordId", 10L)
                .executeUpdate();

        em.flush();
        em.clear();

        repository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(30));
        repository.flush();

        var remaining = repository.findAll();

        assertEquals(1, remaining.size());
        assertEquals(11L, remaining.getFirst().getTimeRecordId());
    }

    private UUID persistEmployee(UUID companyId, String fullName, String cpf) {
        UUID employeeId = UUID.randomUUID();

        em.persistAndFlush(
                EmployeeEntity.builder()
                        .employeeId(employeeId)
                        .fullName(fullName)
                        .cpf(cpf)
                        .jobPosition("Analista")
                        .email(cpf + "@kts.com")
                        .salary(1000.0)
                        .companyId(companyId)
                        .address(AddressEmbeddable.builder()
                                .street("Rua A")
                                .number("10")
                                .postalCode("65000000")
                                .city("São Luís")
                                .state("MA")
                                .build())
                        .build()
        );

        return employeeId;
    }

    private static TimeRecordApprovalEntity approval(Long timeRecordId, UUID employeeId, LocalDateTime createdAt) {
        return TimeRecordApprovalEntity.builder()
                .timeRecordId(timeRecordId)
                .requestingEmployeeId(employeeId)
                .managerId(UUID.randomUUID())
                .newStartWork(LocalDateTime.now().minusHours(8))
                .newEndWork(LocalDateTime.now().minusHours(1))
                .createdAt(createdAt)
                .build();
    }
}
