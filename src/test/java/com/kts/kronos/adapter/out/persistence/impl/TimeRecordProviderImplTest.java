package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeRecordProviderImplTest {

    @Mock TimeRecordRepository repository;
    @InjectMocks TimeRecordProviderImpl provider;

    @Test
    void shouldCoverMethodsAndBranches() {
        TimeRecord tr = mock(TimeRecord.class);
        TimeRecordEntity entity = mock(TimeRecordEntity.class);
        when(repository.save(any())).thenReturn(entity);
        when(entity.getTimeRecordId()).thenReturn(10L);
        TimeRecord dom = mock(TimeRecord.class);
        TimeRecord withId = mock(TimeRecord.class);
        when(entity.toDomain()).thenReturn(dom);
        when(dom.withId(10L)).thenReturn(withId);
        assertThat(provider.save(tr)).isSameAs(withId);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertThat(provider.findById(1L)).contains(dom);
        when(repository.findLatestByEmployeeId(any())).thenReturn(Optional.of(entity));
        assertThat(provider.findTopByEmployeeIdOrderByStartWorkDesc(UUID.randomUUID())).contains(dom);
        provider.deleteTimeRecord(tr);
        verify(repository).delete(any(TimeRecordEntity.class));
        when(repository.findFirstByEmployeeIdAndEndWorkIsNullOrderByStartWorkDesc(any())).thenReturn(Optional.of(entity));
        assertThat(provider.findOpenByEmployeeId(UUID.randomUUID())).contains(dom);

        when(repository.findByEmployeeIdAndActive(any(), anyBoolean())).thenReturn(List.of(entity));
        when(repository.findByEmployeeId(any())).thenReturn(List.of(entity));
        assertThat(provider.findByEmployeeIdAndActive(UUID.randomUUID(), true)).hasSize(1);
        assertThat(provider.findByEmployeeId(UUID.randomUUID())).hasSize(1);

        UUID employeeId = UUID.randomUUID();
        when(repository.existsByEmployeeIdAndDate(eq(employeeId), any(), any())).thenReturn(true);
        assertThat(provider.existsByEmployeeIdAndDate(employeeId, LocalDate.now())).isTrue();
        provider.deleteByEmployeeId(employeeId);
        when(repository.findMaxNsrByCompanyId(any())).thenReturn(88L);
        assertThat(provider.findMaxNsrByCompanyId(UUID.randomUUID())).isEqualTo(88L);

        TimeRecordEntity dayOffSat = mock(TimeRecordEntity.class);
        when(dayOffSat.getStatusRecord()).thenReturn(StatusRecord.DAY_OFF);
        when(dayOffSat.getStartWork()).thenReturn(LocalDateTime.of(2026, 3, 7, 10, 0));
        TimeRecordEntity dayOffMon = mock(TimeRecordEntity.class);
        when(dayOffMon.getStatusRecord()).thenReturn(StatusRecord.DAY_OFF);
        when(dayOffMon.getStartWork()).thenReturn(LocalDateTime.of(2026, 3, 9, 10, 0));
        when(repository.findByEmployeeIdAndStartWorkBetween(any(), any(), any())).thenReturn(List.of(dayOffSat, dayOffMon));
        assertThat(provider.countWeekendDaysOffThisMonth(employeeId, LocalDate.of(2026,3,10))).isEqualTo(1);

        when(repository.findByEmployeeIdAndStartWorkBetween(eq(employeeId), any(), any())).thenReturn(List.of(entity));
        assertThat(provider.findByRange(employeeId, LocalDateTime.now().minusDays(1), LocalDateTime.now())).hasSize(1);
        when(repository.findByEmployeeAndDatesAndStatuses(any(), anySet(), anySet())).thenReturn(List.of(entity));
        assertThat(provider.findByEmployeeAndDatesAndStatuses(employeeId, Set.of(LocalDate.now()), Set.of(StatusRecord.CREATED))).hasSize(1);

        assertThat(provider.findByIdIn(Set.of())).isEmpty();
        when(repository.findByTimeRecordIdIn(Set.of(1L))).thenReturn(List.of(entity));
        assertThat(provider.findByIdIn(Set.of(1L))).hasSize(1);

        assertThat(provider.saveAll(List.of())).isEmpty();
        when(repository.saveAll(anyList())).thenReturn(List.of(entity));
        assertThat(provider.saveAll(List.of(tr))).hasSize(1);

        assertThat(provider.findByEmployeeIdInAndStatusesIn(Set.of(), Set.of(StatusRecord.CREATED))).isEmpty();
        assertThat(provider.findByEmployeeIdInAndStatusesIn(Set.of(employeeId), Set.of())).isEmpty();
        when(repository.findByEmployeeIdInAndStatusRecordIn(anySet(), anySet())).thenReturn(List.of(entity));
        assertThat(provider.findByEmployeeIdInAndStatusesIn(Set.of(employeeId), Set.of(StatusRecord.CREATED))).hasSize(1);

        assertThat(provider.findFirstByEmployeeIdAndStartWorkBetweenAndStatusIn(employeeId, LocalDateTime.now(), LocalDateTime.now(), Set.of())).isEmpty();
        when(repository.findFirstByEmployeeIdAndStartWorkBetweenAndStatusRecordIn(any(), any(), any(), anySet())).thenReturn(Optional.of(entity));
        assertThat(provider.findFirstByEmployeeIdAndStartWorkBetweenAndStatusIn(employeeId, LocalDateTime.now(), LocalDateTime.now(), Set.of(StatusRecord.CREATED))).contains(dom);

        when(repository.findByEmployeeIdAndActiveAndStartWorkBetweenOrderByStartWorkAsc(any(), eq(true), any(), any())).thenReturn(List.of(entity));
        assertThat(provider.findActiveByEmployeeIdAndStartWorkBetween(employeeId, LocalDateTime.now().minusHours(1), LocalDateTime.now())).hasSize(1);
    }
}
