package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.AfdEntryRepository;
import com.kts.kronos.adapter.out.persistence.entity.AfdEntryEntity;
import com.kts.kronos.domain.model.AfdEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfdEntryProviderImplTest {

    @Mock
    private AfdEntryRepository repository;

    @InjectMocks
    private AfdEntryProviderImpl provider;

    @Test
    @DisplayName("save: deve mapear domínio -> entidade -> domínio")
    void shouldSaveMappingDomainEntityAndBack() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDateTime recordDate = LocalDateTime.of(2026, 4, 18, 8, 30);

        AfdEntry domain = new AfdEntry(
                null,
                10L,
                "7",
                recordDate,
                "12345678901",
                "12345678901",
                companyId,
                employeeId,
                "prev-hash",
                "curr-hash"
        );

        when(repository.save(any(AfdEntryEntity.class))).thenAnswer(invocation -> {
            AfdEntryEntity entity = invocation.getArgument(0);
            return AfdEntryEntity.builder()
                    .id(99L)
                    .nsr(entity.getNsr())
                    .recordType(entity.getRecordType())
                    .recordDate(entity.getRecordDate())
                    .employeeCpf(entity.getEmployeeCpf())
                    .employeePis(entity.getEmployeePis())
                    .companyId(entity.getCompanyId())
                    .employeeId(entity.getEmployeeId())
                    .previousHash(entity.getPreviousHash())
                    .currentHash(entity.getCurrentHash())
                    .build();
        });

        AfdEntry result = provider.save(domain);

        ArgumentCaptor<AfdEntryEntity> captor = ArgumentCaptor.forClass(AfdEntryEntity.class);
        verify(repository).save(captor.capture());
        AfdEntryEntity savedEntity = captor.getValue();

        assertNull(savedEntity.getId());
        assertEquals(10L, savedEntity.getNsr());
        assertEquals("7", savedEntity.getRecordType());
        assertEquals(recordDate, savedEntity.getRecordDate());
        assertEquals("12345678901", savedEntity.getEmployeeCpf());
        assertEquals("12345678901", savedEntity.getEmployeePis());
        assertEquals(companyId, savedEntity.getCompanyId());
        assertEquals(employeeId, savedEntity.getEmployeeId());
        assertEquals("prev-hash", savedEntity.getPreviousHash());
        assertEquals("curr-hash", savedEntity.getCurrentHash());

        assertEquals(99L, result.id());
        assertEquals(domain.nsr(), result.nsr());
        assertEquals(domain.recordType(), result.recordType());
        assertEquals(domain.recordDate(), result.recordDate());
        assertEquals(domain.employeeCpf(), result.employeeCpf());
        assertEquals(domain.employeePis(), result.employeePis());
        assertEquals(domain.companyId(), result.companyId());
        assertEquals(domain.employeeId(), result.employeeId());
        assertEquals(domain.previousHash(), result.previousHash());
        assertEquals(domain.currentHash(), result.currentHash());
    }

    @Test
    @DisplayName("findLastHashByCompanyId: deve delegar ao repository")
    void shouldFindLastHashByCompanyId() {
        UUID companyId = UUID.randomUUID();
        when(repository.findLastHashByCompanyId(companyId)).thenReturn(Optional.of("last-hash"));

        Optional<String> result = provider.findLastHashByCompanyId(companyId);

        assertTrue(result.isPresent());
        assertEquals("last-hash", result.get());
        verify(repository).findLastHashByCompanyId(companyId);
    }

    @Test
    @DisplayName("streamByCompanyIdOrderByNsr: deve retornar stream vazio")
    void shouldReturnEmptyStream() {
        UUID companyId = UUID.randomUUID();
        when(repository.streamAllByCompanyIdOrderByNsrAsc(companyId)).thenReturn(Stream.empty());

        try (Stream<AfdEntry> result = provider.streamByCompanyIdOrderByNsr(companyId)) {
            assertTrue(result.toList().isEmpty());
        }

        verify(repository).streamAllByCompanyIdOrderByNsrAsc(companyId);
    }

    @Test
    @DisplayName("streamByCompanyIdOrderByNsr: deve mapear stream não vazio")
    void shouldMapNonEmptyStream() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        AfdEntryEntity first = AfdEntryEntity.builder()
                .id(1L)
                .nsr(1L)
                .recordType("7")
                .recordDate(LocalDateTime.of(2026, 4, 18, 8, 0))
                .employeeCpf("12345678901")
                .employeePis("12345678901")
                .companyId(companyId)
                .employeeId(employeeId)
                .previousHash(null)
                .currentHash("hash-1")
                .build();

        AfdEntryEntity second = AfdEntryEntity.builder()
                .id(2L)
                .nsr(2L)
                .recordType("7")
                .recordDate(LocalDateTime.of(2026, 4, 18, 12, 0))
                .employeeCpf("12345678901")
                .employeePis("12345678901")
                .companyId(companyId)
                .employeeId(employeeId)
                .previousHash("hash-1")
                .currentHash("hash-2")
                .build();

        when(repository.streamAllByCompanyIdOrderByNsrAsc(companyId))
                .thenReturn(Stream.of(first, second));

        try (Stream<AfdEntry> result = provider.streamByCompanyIdOrderByNsr(companyId)) {
            List<AfdEntry> entries = result.toList();

            assertEquals(2, entries.size());
            assertEquals(1L, entries.get(0).nsr());
            assertEquals(2L, entries.get(1).nsr());
            assertEquals("hash-1", entries.get(0).currentHash());
            assertEquals("hash-2", entries.get(1).currentHash());
        }

        verify(repository).streamAllByCompanyIdOrderByNsrAsc(companyId);
    }
}