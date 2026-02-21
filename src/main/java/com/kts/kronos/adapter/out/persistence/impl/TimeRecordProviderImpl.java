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
import java.util.Set;
import java.util.UUID;

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
    public long countWeekendDaysOffThisMonth(UUID empId, LocalDate referenceDate) {
        LocalDateTime startOfMonth = referenceDate.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfReferenceDay = referenceDate.atStartOfDay(); // Conta até ontem/hoje antes do processamento

        // Busca registros do mês e filtra em memória (Seguro e compatível com qualquer banco)
        return jpa.findByEmployeeIdAndStartWorkBetween(empId, startOfMonth, endOfReferenceDay)
                .stream()
                .filter(t -> t.getStatusRecord() == StatusRecord.DAY_OFF) // Apenas folgas
                .filter(t -> {
                    DayOfWeek day = t.getStartWork().getDayOfWeek();
                    return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY; // Apenas finais de semana
                })
                .count();
    }


    @Override
    public List<TimeRecord> findByRange(UUID employeeId, LocalDateTime start, LocalDateTime end) {
        return jpa.findByEmployeeIdAndStartWorkBetween(employeeId, start, end)
                .stream()
                .map(TimeRecordEntity::toDomain) // Converte para o modelo de domínio
                .toList();
    }

    @Override
    public List<TimeRecord> findByEmployeeAndDatesAndStatuses(
            UUID employeeId,
            java.util.Set<LocalDate> dates,
            java.util.Set<StatusRecord> statuses) {

        return jpa.findByEmployeeAndDatesAndStatuses(employeeId, dates, statuses)
                .stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }

    @Override
    public List<TimeRecord> findByIdIn(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpa.findByTimeRecordIdIn(ids)
                .stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }

    @Override
    public List<TimeRecord> saveAll(List<TimeRecord> timeRecords) {
        if (timeRecords == null || timeRecords.isEmpty()) {
            return List.of();
        }

        List<TimeRecordEntity> entitiesToSave = timeRecords.stream()
                .map(TimeRecordEntity::fromDomain)
                .toList();

        List<TimeRecordEntity> savedEntities = jpa.saveAll(entitiesToSave);

        return savedEntities.stream()
                .map(entity -> entity.toDomain().withId(entity.getTimeRecordId()))
                .toList();
    }

    @Override
    public List<TimeRecord> findByEmployeeIdInAndStatusesIn(Set<UUID> employeeIds, Set<StatusRecord> statuses) {

        if (employeeIds == null || employeeIds.isEmpty() || statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return jpa.findByEmployeeIdInAndStatusRecordIn(employeeIds, statuses)
                .stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }
}

