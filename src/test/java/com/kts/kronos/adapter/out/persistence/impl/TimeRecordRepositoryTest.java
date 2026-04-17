package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class TimeRecordRepositoryTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private TimeRecordRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void deveBuscarUltimoRegistroPorEmployeeId() {
        UUID employeeId = persistEmployee(UUID.randomUUID(), "Ana Paula", "52345678901");

        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 10, 8, 0), StatusRecord.CREATED));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 11, 8, 0), StatusRecord.CREATED));

        var latest = repository.findLatestByEmployeeId(employeeId);

        assertTrue(latest.isPresent());
        assertEquals(LocalDateTime.of(2026, 4, 11, 8, 0), latest.get().getStartWork());
    }

    @Test
    void deveValidarExistenciaPorData() {
        UUID employeeId = persistEmployee(UUID.randomUUID(), "Bruno", "62345678901");

        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 15, 9, 0), StatusRecord.CREATED));

        boolean exists = repository.existsByEmployeeIdAndDate(
                employeeId,
                LocalDateTime.of(2026, 4, 15, 0, 0),
                LocalDateTime.of(2026, 4, 15, 23, 59, 59)
        );

        assertTrue(exists);
    }

    @Test
    void deveListarTimeOffRequestsPorCompanyIdPaginado() {
        UUID companyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();

        UUID anaId = persistEmployee(companyId, "Ana Paula", "72345678901");
        UUID joaoId = persistEmployee(otherCompanyId, "Ana de Fora", "82345678901");

        repository.save(record(anaId, LocalDateTime.of(2026, 4, 10, 9, 0), StatusRecord.TIME_OFF_REQUEST));
        repository.save(record(joaoId, LocalDateTime.of(2026, 4, 10, 9, 0), StatusRecord.TIME_OFF_REQUEST));

        var page = repository.findTimeOffRequestsByCompanyId(
                PageRequest.of(0, 10),
                companyId,
                List.of(StatusRecord.TIME_OFF_REQUEST),
                "%ana%"
        );

        assertEquals(1, page.getContent().size());
        assertEquals(anaId, page.getContent().getFirst().getEmployeeId());
    }

    @Test
    void deveConsolidarPeriodosDeFerias() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = persistEmployee(companyId, "Marina", "92345678901");

        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 1, 9, 0), StatusRecord.REQUEST_VACATION));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 2, 9, 0), StatusRecord.REQUEST_VACATION));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 3, 9, 0), StatusRecord.REQUEST_VACATION));

        var page = repository.findVacationRequestPeriodsByCompanyId(
                PageRequest.of(0, 10),
                companyId,
                List.of(StatusRecord.REQUEST_VACATION.name()),
                "%marina%"
        );

        assertEquals(1, page.getContent().size());
        var projection = page.getContent().getFirst();

        assertEquals(LocalDate.of(2026, 4, 1), projection.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 3), projection.getEndDate());
        assertEquals(StatusRecord.REQUEST_VACATION.name(), projection.getStatus());
    }

    @Test
    void deveContarFolgasDeFimDeSemanaNoMes() {
        UUID employeeId = persistEmployee(UUID.randomUUID(), "Pedro", "10345678901");

        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 4, 9, 0), StatusRecord.DAY_OFF)); // sábado
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 5, 9, 0), StatusRecord.DAY_OFF)); // domingo
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 6, 9, 0), StatusRecord.DAY_OFF)); // segunda

        long count = repository.countWeekendDaysOffThisMonth(
                employeeId,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 20, 0, 0)
        );

        assertEquals(2, count);
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

    private static TimeRecordEntity record(UUID employeeId, LocalDateTime startWork, StatusRecord status) {
        return TimeRecordEntity.builder()
                .employeeId(employeeId)
                .startWork(startWork)
                .endWork(startWork.plusHours(8))
                .statusRecord(status)
                .active(true)
                .edited(false)
                .build();
    }
}