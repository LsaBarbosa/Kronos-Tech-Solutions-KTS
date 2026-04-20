package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeRecordProviderImplTest {

    @Mock
    private TimeRecordRepository jpa;

    @InjectMocks
    private TimeRecordProviderImpl provider;

    @Test
    @DisplayName("save: deve salvar e retornar domínio com id")
    void shouldSave() {
        TimeRecord record = timeRecord();

        when(jpa.save(any(TimeRecordEntity.class))).thenAnswer(invocation -> {
            TimeRecordEntity entity = invocation.getArgument(0);
            entity.setTimeRecordId(123L);
            return entity;
        });

        TimeRecord result = provider.save(record);

        ArgumentCaptor<TimeRecordEntity> captor = ArgumentCaptor.forClass(TimeRecordEntity.class);
        verify(jpa).save(captor.capture());

        TimeRecordEntity entity = captor.getValue();
        assertEquals(record.employeeId(), entity.getEmployeeId());
        assertEquals(record.startWork(), entity.getStartWork());
        assertEquals(record.endWork(), entity.getEndWork());
        assertEquals(record.statusRecord(), entity.getStatusRecord());

        assertEquals(123L, result.timeRecordId());
        assertEquals(record.employeeId(), result.employeeId());
    }

    @Test
    @DisplayName("findById: deve mapear entidade para domínio")
    void shouldFindById() {
        TimeRecordEntity entity = entity(timeRecord(), 1L, null);
        when(jpa.findById(1L)).thenReturn(Optional.of(entity));

        Optional<TimeRecord> result = provider.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().timeRecordId());
        assertEquals(entity.getEmployeeId(), result.get().employeeId());
    }

    @Test
    @DisplayName("findTopByEmployeeIdOrderByStartWorkDesc: deve delegar e mapear")
    void shouldFindLatestByEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        TimeRecordEntity entity = entity(timeRecord(employeeId), 2L, null);

        when(jpa.findLatestByEmployeeId(employeeId)).thenReturn(Optional.of(entity));

        Optional<TimeRecord> result = provider.findTopByEmployeeIdOrderByStartWorkDesc(employeeId);

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().timeRecordId());
        verify(jpa).findLatestByEmployeeId(employeeId);
    }

    @Test
    @DisplayName("deleteTimeRecord: deve converter e deletar entidade")
    void shouldDeleteTimeRecord() {
        TimeRecord record = timeRecord();

        provider.deleteTimeRecord(record);

        ArgumentCaptor<TimeRecordEntity> captor = ArgumentCaptor.forClass(TimeRecordEntity.class);
        verify(jpa).delete(captor.capture());

        TimeRecordEntity entity = captor.getValue();
        assertEquals(record.timeRecordId(), entity.getTimeRecordId());
        assertEquals(record.employeeId(), entity.getEmployeeId());
    }

    @Test
    @DisplayName("findOpenByEmployeeId: deve buscar registro aberto")
    void shouldFindOpenByEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        TimeRecordEntity entity = entity(timeRecord(employeeId), null, null);

        when(jpa.findFirstByEmployeeIdAndEndWorkIsNullOrderByStartWorkDesc(employeeId))
                .thenReturn(Optional.of(entity));

        Optional<TimeRecord> result = provider.findOpenByEmployeeId(employeeId);

        assertTrue(result.isPresent());
        assertNull(result.get().endWork());
        verify(jpa).findFirstByEmployeeIdAndEndWorkIsNullOrderByStartWorkDesc(employeeId);
    }

    @Test
    @DisplayName("findByEmployeeIdAndActive: deve retornar lista mapeada")
    void shouldFindByEmployeeIdAndActive() {
        UUID employeeId = UUID.randomUUID();
        TimeRecordEntity entity = entity(timeRecord(employeeId), 10L, null);

        when(jpa.findByEmployeeIdAndActive(employeeId, true)).thenReturn(List.of(entity));

        List<TimeRecord> result = provider.findByEmployeeIdAndActive(employeeId, true);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).timeRecordId());
    }

    @Test
    @DisplayName("findByEmployeeId: deve retornar lista mapeada")
    void shouldFindByEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        TimeRecordEntity entity = entity(timeRecord(employeeId), 11L, null);

        when(jpa.findByEmployeeId(employeeId)).thenReturn(List.of(entity));

        List<TimeRecord> result = provider.findByEmployeeId(employeeId);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).timeRecordId());
    }

    @Test
    @DisplayName("existsByEmployeeIdAndDate: deve calcular início e fim do dia corretamente")
    void shouldCheckExistenceByEmployeeIdAndDate() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 18);

        when(jpa.existsByEmployeeIdAndDate(eq(employeeId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        boolean result = provider.existsByEmployeeIdAndDate(employeeId, date);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(jpa).existsByEmployeeIdAndDate(eq(employeeId), startCaptor.capture(), endCaptor.capture());

        assertTrue(result);
        assertEquals(date.atStartOfDay(), startCaptor.getValue());
        assertEquals(date.atTime(23, 59, 59), endCaptor.getValue());
    }

    @Test
    @DisplayName("deleteByEmployeeId: deve delegar ao repository")
    void shouldDeleteByEmployeeId() {
        UUID employeeId = UUID.randomUUID();

        provider.deleteByEmployeeId(employeeId);

        verify(jpa).deleteByEmployeeId(employeeId);
    }

    @Test
    @DisplayName("findMaxNsrByCompanyId: deve delegar ao repository")
    void shouldFindMaxNsrByCompanyId() {
        UUID companyId = UUID.randomUUID();
        when(jpa.findMaxNsrByCompanyId(companyId)).thenReturn(99L);

        Long result = provider.findMaxNsrByCompanyId(companyId);

        assertEquals(99L, result);
        verify(jpa).findMaxNsrByCompanyId(companyId);
    }

    @Test
    @DisplayName("countWeekendDaysOffThisMonth: deve calcular range do mês corretamente")
    void shouldCountWeekendDaysOffThisMonth() {
        UUID employeeId = UUID.randomUUID();
        LocalDate referenceDate = LocalDate.of(2026, 4, 18);

        when(jpa.countWeekendDaysOffThisMonth(eq(employeeId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);

        long result = provider.countWeekendDaysOffThisMonth(employeeId, referenceDate);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(jpa).countWeekendDaysOffThisMonth(eq(employeeId), startCaptor.capture(), endCaptor.capture());

        assertEquals(2L, result);
        assertEquals(LocalDate.of(2026, 4, 1).atStartOfDay(), startCaptor.getValue());
        assertEquals(referenceDate.atStartOfDay(), endCaptor.getValue());
    }

    @Test
    @DisplayName("findByRange: deve delegar e mapear")
    void shouldFindByRange() {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 30, 23, 59);

        TimeRecordEntity entity = entity(timeRecord(employeeId), 50L, null);

        when(jpa.findByEmployeeIdAndStartWorkBetween(employeeId, start, end))
                .thenReturn(List.of(entity));

        List<TimeRecord> result = provider.findByRange(employeeId, start, end);

        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).timeRecordId());
        verify(jpa).findByEmployeeIdAndStartWorkBetween(employeeId, start, end);
    }

    private TimeRecordEntity entity(TimeRecord record, Long id, LocalDateTime endWork) {
        TimeRecordEntity entity = TimeRecordEntity.fromDomain(record);
        entity.setTimeRecordId(id);
        entity.setEndWork(endWork);
        return entity;
    }

    private TimeRecord timeRecord() {
        return timeRecord(UUID.randomUUID());
    }

    private TimeRecord timeRecord(UUID employeeId) {
        return new TimeRecord(
                1L,
                LocalDateTime.of(2026, 4, 18, 8, 0),
                LocalDateTime.of(2026, 4, 18, 17, 0),
                StatusRecord.UPDATED,
                false,
                true,
                employeeId,
                -23.5505,
                -46.6333,
                -23.5505,
                -46.6333,
                10L,
                11L,
                LocalDateTime.of(2026, 4, 18, 8, 0),
                LocalDateTime.of(2026, 4, 18, 17, 0)
        );
    }
}