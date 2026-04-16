package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.kts.kronos.application.port.out.projection.VacationRequestPeriodProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor
@Component
public class TimeRecordProviderImpl implements TimeRecordProvider {
    private final TimeRecordRepository jpa;

    @Override
    public TimeRecord save(TimeRecord tr) {
        var entity = TimeRecordEntity.fromDomain(tr);
        var saved = jpa.save(entity);
        return saved.toDomain().withId(saved.getTimeRecordId());
    }

    @Override
    public Optional<TimeRecord> findById(Long id) {
        return jpa.findById(id)
                .map(TimeRecordEntity::toDomain);
    }

    @Override
    public Optional<TimeRecord> findTopByEmployeeIdOrderByStartWorkDesc(UUID employeeId) {
        return jpa.findLatestByEmployeeId(employeeId)
                .map(TimeRecordEntity::toDomain);
    }

    @Override
    public void deleteTimeRecord(TimeRecord timeRecord) {
        var entity = TimeRecordEntity.fromDomain(timeRecord);
        jpa.delete(entity);
    }


    @Override
    public Optional<TimeRecord> findOpenByEmployeeId(UUID employeeId) {
        return jpa
                .findFirstByEmployeeIdAndEndWorkIsNullOrderByStartWorkDesc(employeeId)
                .map(TimeRecordEntity::toDomain);
    }

    @Override
    public List<TimeRecord> findByEmployeeIdAndActive(UUID employeeId, boolean active) {
        return jpa.findByEmployeeIdAndActive(employeeId, active)
                .stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }
    @Override
    public List<TimeRecord> findByEmployeeId(UUID employeeId) {
        return jpa.findByEmployeeId(employeeId)
                .stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmployeeIdAndDate(UUID employeeId, LocalDate date) {
        var dayStart = date.atStartOfDay();
        var dayEnd   = date.atTime(23, 59, 59);
        return jpa.existsByEmployeeIdAndDate(employeeId, dayStart, dayEnd);
    }

    @Override
    public void deleteByEmployeeId(UUID employeeId) {
        jpa.deleteByEmployeeId(employeeId);
    }

    @Override
    public Long findMaxNsrByCompanyId(UUID companyId) {
        return jpa.findMaxNsrByCompanyId(companyId);
    }

    @Override
    public List<TimeRecord> findAllByIds(Collection<Long> ids) {
        List<TimeRecord> result = new ArrayList<>();
        jpa.findAllById(ids).forEach(entity -> result.add(entity.toDomain()));
        return result;
    }

    @Override
    public Page<TimeRecord> findTimeOffRequestsByCompanyId(
            Pageable pageable,
            UUID companyId,
            Collection<StatusRecord> statuses,
            String employeeName
    ) {
        String searchName = buildSearchName(employeeName);
        return jpa.findTimeOffRequestsByCompanyId(pageable, companyId, statuses, searchName)
                .map(TimeRecordEntity::toDomain);
    }

    @Override
    public Page<VacationRequestPeriodProjection> findVacationRequestPeriodsByCompanyId(
            Pageable pageable,
            UUID companyId,
            Collection<String> statuses,
            String employeeName
    ) {
        String searchName = buildSearchName(employeeName);
        return jpa.findVacationRequestPeriodsByCompanyId(pageable, companyId, statuses, searchName);
    }

    private String buildSearchName(String employeeName) {
        if (employeeName == null || employeeName.isBlank()) {
            return null;
        }
        return "%" + employeeName.toLowerCase() + "%";
    }

    @Override
    public long countWeekendDaysOffThisMonth(UUID empId, LocalDate referenceDate) {
        LocalDateTime startOfMonth = referenceDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endExclusive = referenceDate.atStartOfDay();

        return jpa.countWeekendDaysOffThisMonth(empId, startOfMonth, endExclusive);
    }

    @Override
    public List<TimeRecord> findByEmployeeIdsAndStatuses(Collection<UUID> employeeIds,
                                                         Collection<StatusRecord> statuses) {
        if (employeeIds == null || employeeIds.isEmpty() || statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        return jpa.findByEmployeeIdInAndStatusRecordInAndStartWorkIsNotNull(employeeIds, statuses)
                .stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }

    @Override
    public List<TimeRecord> findByRange(UUID employeeId, LocalDateTime start, LocalDateTime end) {
        // Usa o método mágico do repositório que você já criou
        return jpa.findByEmployeeIdAndStartWorkBetween(employeeId, start, end)
                .stream()
                .map(TimeRecordEntity::toDomain) // Converte para o modelo de domínio
                .toList();
    }

    @Override
    public List<TimeRecord> findByEmployeeIdsAndRange(Collection<UUID> employeeIds,
                                                      LocalDateTime start,
                                                      LocalDateTime end) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }

        return jpa.findByEmployeeIdsAndStartWorkBetween(employeeIds, start, end)
                .stream()
    public List<TimeRecord> findReportRecords(UUID employeeId,
                                              LocalDateTime start,
                                              LocalDateTime end,
                                              Collection<StatusRecord> statuses,
                                              Boolean active) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        var entities = active == null
                ? jpa.findByEmployeeIdAndStartWorkBetweenAndStatusRecordIn(employeeId, start, end, statuses)
                : jpa.findByEmployeeIdAndActiveAndStartWorkBetweenAndStatusRecordIn(employeeId, active, start, end, statuses);

        return entities.stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }
}
