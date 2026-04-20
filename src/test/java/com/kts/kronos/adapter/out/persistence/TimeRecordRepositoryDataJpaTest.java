package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeRecordRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

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
    void deveBuscarRegistroAbertoMaisRecente() {
        UUID employeeId = persistEmployee(UUID.randomUUID(), "Clara", "13345678901");
        repository.save(openRecord(employeeId, LocalDateTime.of(2026, 4, 15, 9, 0)));
        repository.save(openRecord(employeeId, LocalDateTime.of(2026, 4, 16, 9, 0)));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 17, 9, 0), StatusRecord.CREATED));

        var result = repository.findFirstByEmployeeIdAndEndWorkIsNullOrderByStartWorkDesc(employeeId);

        assertTrue(result.isPresent());
        assertEquals(LocalDateTime.of(2026, 4, 16, 9, 0), result.get().getStartWork());
    }

    @Test
    void deveBuscarPorEmployeeIdEActiveEExcluirPorEmployeeId() {
        UUID employeeId = persistEmployee(UUID.randomUUID(), "Daniel", "14345678901");
        UUID otherEmployeeId = persistEmployee(UUID.randomUUID(), "Eduardo", "15345678901");

        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 1, 9, 0), StatusRecord.CREATED, true));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 2, 9, 0), StatusRecord.CREATED, false));
        repository.save(record(otherEmployeeId, LocalDateTime.of(2026, 4, 3, 9, 0), StatusRecord.CREATED, true));
        repository.flush();

        assertEquals(2, repository.findByEmployeeId(employeeId).size());
        assertEquals(1, repository.findByEmployeeIdAndActive(employeeId, true).size());
        assertEquals(1, repository.findByEmployeeIdAndActive(employeeId, false).size());

        repository.deleteByEmployeeId(employeeId);
        repository.flush();

        assertTrue(repository.findByEmployeeId(employeeId).isEmpty());
        assertEquals(1, repository.findByEmployeeId(otherEmployeeId).size());
    }

    @Test
    void deveBuscarMaiorNsrPorCompanyId() {
        UUID companyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        UUID employeeId = persistEmployee(companyId, "Fabiana", "16345678901");
        UUID otherEmployeeId = persistEmployee(otherCompanyId, "Gustavo", "17345678901");

        repository.save(recordWithNsr(employeeId, LocalDateTime.of(2026, 4, 1, 9, 0), 10L, 20L));
        repository.save(recordWithNsr(employeeId, LocalDateTime.of(2026, 4, 2, 9, 0), 25L, 23L));
        repository.save(recordWithNsr(otherEmployeeId, LocalDateTime.of(2026, 4, 3, 9, 0), 99L, 100L));

        assertEquals(25L, repository.findMaxNsrByCompanyId(companyId));
    }

    @Test
    void deveBuscarPorEmployeeIdsStatusEStartWorkNaoNulo() {
        UUID companyId = UUID.randomUUID();
        UUID employeeA = persistEmployee(companyId, "Helena", "18345678901");
        UUID employeeB = persistEmployee(companyId, "Igor", "19345678901");

        repository.save(record(employeeA, LocalDateTime.of(2026, 4, 1, 9, 0), StatusRecord.TIME_OFF_REQUEST));
        repository.save(record(employeeB, LocalDateTime.of(2026, 4, 2, 9, 0), StatusRecord.CREATED));
        repository.save(TimeRecordEntity.builder()
                .employeeId(employeeB)
                .startWork(null)
                .endWork(null)
                .statusRecord(StatusRecord.TIME_OFF_REQUEST)
                .active(true)
                .edited(false)
                .build());

        var result = repository.findByEmployeeIdInAndStatusRecordInAndStartWorkIsNotNull(
                List.of(employeeA, employeeB),
                List.of(StatusRecord.TIME_OFF_REQUEST)
        );

        assertEquals(1, result.size());
        assertEquals(employeeA, result.getFirst().getEmployeeId());
    }

    @Test
    void deveBuscarPorIntervaloStatusEActive() {
        UUID employeeId = persistEmployee(UUID.randomUUID(), "Julia", "20345678901");

        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 1, 9, 0), StatusRecord.CREATED, true));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 2, 9, 0), StatusRecord.TIME_OFF, true));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 3, 9, 0), StatusRecord.TIME_OFF, false));
        repository.save(record(employeeId, LocalDateTime.of(2026, 5, 1, 9, 0), StatusRecord.TIME_OFF, true));

        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 30, 23, 59);

        assertEquals(3, repository.findByEmployeeIdAndStartWorkBetween(employeeId, start, end).size());
        assertEquals(2, repository.findByEmployeeIdAndStartWorkBetweenAndStatusRecordIn(
                employeeId,
                start,
                end,
                List.of(StatusRecord.TIME_OFF)
        ).size());
        assertEquals(1, repository.findByEmployeeIdAndActiveAndStartWorkBetweenAndStatusRecordIn(
                employeeId,
                true,
                start,
                end,
                List.of(StatusRecord.TIME_OFF)
        ).size());
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
    void deveListarTimeOffRequestsSemFiltroDeNomeOrdenadoDesc() {
        UUID companyId = UUID.randomUUID();
        UUID anaId = persistEmployee(companyId, "Ana Paula", "21345678901");

        repository.save(record(anaId, LocalDateTime.of(2026, 4, 10, 9, 0), StatusRecord.TIME_OFF_REQUEST));
        repository.save(record(anaId, LocalDateTime.of(2026, 4, 11, 9, 0), StatusRecord.TIME_OFF_REQUEST));

        var page = repository.findTimeOffRequestsByCompanyId(
                PageRequest.of(0, 1),
                companyId,
                List.of(StatusRecord.TIME_OFF_REQUEST),
                null
        );

        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals(LocalDateTime.of(2026, 4, 11, 9, 0), page.getContent().getFirst().getStartWork());
    }

    @Test
    void deveAgruparPeriodosDeFeriasConsecutivosPorEmpresa() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = persistEmployee(companyId, "Karina", "22345678901");

        TimeRecordEntity day1 = repository.save(record(employeeId, LocalDateTime.of(2026, 7, 1, 9, 0), StatusRecord.REQUEST_VACATION));
        TimeRecordEntity day2 = repository.save(record(employeeId, LocalDateTime.of(2026, 7, 2, 9, 0), StatusRecord.REQUEST_VACATION));
        TimeRecordEntity day4 = repository.save(record(employeeId, LocalDateTime.of(2026, 7, 4, 9, 0), StatusRecord.REQUEST_VACATION));
        repository.flush();

        var page = repository.findVacationRequestPeriodsByCompanyId(
                PageRequest.of(0, 10),
                companyId,
                List.of(StatusRecord.REQUEST_VACATION.name()),
                "%karina%"
        );

        assertEquals(2, page.getContent().size());
        assertEquals(employeeId, page.getContent().getFirst().getEmployeeId());
        assertEquals("Karina", page.getContent().getFirst().getEmployeeName());
        assertEquals("REQUEST_VACATION", page.getContent().getFirst().getStatus());
        assertEquals(day1.getTimeRecordId() + "," + day2.getTimeRecordId(), page.getContent().getFirst().getTimeRecordIdsCsv());
        assertEquals(String.valueOf(day4.getTimeRecordId()), page.getContent().get(1).getTimeRecordIdsCsv());
    }

    @Test
    void deveContarFolgasDeFimDeSemanaNoMes() {
        UUID employeeId = persistEmployee(UUID.randomUUID(), "Leandro", "23345678901");

        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 4, 9, 0), StatusRecord.DAY_OFF));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 5, 9, 0), StatusRecord.DAY_OFF));
        repository.save(record(employeeId, LocalDateTime.of(2026, 4, 6, 9, 0), StatusRecord.DAY_OFF));
        repository.save(record(employeeId, LocalDateTime.of(2026, 5, 2, 9, 0), StatusRecord.DAY_OFF));

        long result = repository.countWeekendDaysOffThisMonth(
                employeeId,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 5, 1, 0, 0)
        );

        assertEquals(2, result);
    }

    @Test
    void deveBuscarPorEmployeeIdsNoIntervalo() {
        UUID companyId = UUID.randomUUID();
        UUID employeeA = persistEmployee(companyId, "Marina", "92345678901");
        UUID employeeB = persistEmployee(companyId, "Pedro", "10345678901");

        repository.save(record(employeeA, LocalDateTime.of(2026, 4, 1, 9, 0), StatusRecord.CREATED));
        repository.save(record(employeeB, LocalDateTime.of(2026, 4, 2, 9, 0), StatusRecord.CREATED));
        repository.save(record(employeeB, LocalDateTime.of(2026, 5, 2, 9, 0), StatusRecord.CREATED));

        var result = repository.findByEmployeeIdsAndStartWorkBetween(
                List.of(employeeA, employeeB),
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59, 59)
        );

        assertEquals(2, result.size());
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
        return record(employeeId, startWork, status, true);
    }

    private static TimeRecordEntity record(UUID employeeId, LocalDateTime startWork, StatusRecord status, boolean active) {
        return TimeRecordEntity.builder()
                .employeeId(employeeId)
                .startWork(startWork)
                .endWork(startWork.plusHours(8))
                .statusRecord(status)
                .active(active)
                .edited(false)
                .build();
    }

    private static TimeRecordEntity openRecord(UUID employeeId, LocalDateTime startWork) {
        return TimeRecordEntity.builder()
                .employeeId(employeeId)
                .startWork(startWork)
                .endWork(null)
                .statusRecord(StatusRecord.PENDING)
                .active(true)
                .edited(false)
                .build();
    }

    private static TimeRecordEntity recordWithNsr(UUID employeeId, LocalDateTime startWork, Long nsrCheckin, Long nsrCheckout) {
        return TimeRecordEntity.builder()
                .employeeId(employeeId)
                .startWork(startWork)
                .endWork(startWork.plusHours(8))
                .statusRecord(StatusRecord.CREATED)
                .active(true)
                .edited(false)
                .nsrCheckin(nsrCheckin)
                .nsrCheckout(nsrCheckout)
                .build();
    }
}
