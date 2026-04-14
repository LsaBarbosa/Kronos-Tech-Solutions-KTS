package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.TimeRecordApprovalRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordApprovalEntity;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeRecordApprovalProviderImplTest {

    @InjectMocks
    private TimeRecordApprovalProviderImpl provider;

    @Mock
    private TimeRecordApprovalRepository repository;

    @Test
    @DisplayName("findAllByCompanyId: normaliza busca textual para prefixo lowercase")
    void shouldNormalizeEmployeeNameAsLowercasePrefix() {
        UUID companyId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        when(repository.findAllByCompanyId(pageable, companyId, "ana%"))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        provider.findAllByCompanyId(pageable, "  Ana  ", companyId);

        verify(repository).findAllByCompanyId(pageable, companyId, "ana%");
    }

    @Test
    @DisplayName("findAllByCompanyId: quando nome vier em branco envia filtro nulo")
    void shouldSendNullNameFilterWhenEmployeeNameIsBlank() {
        UUID companyId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        when(repository.findAllByCompanyId(pageable, companyId, null))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        provider.findAllByCompanyId(pageable, "   ", companyId);

        verify(repository).findAllByCompanyId(pageable, companyId, null);
    }

    @Test
    @DisplayName("findAllByCompanyId: quando nome vier nulo envia filtro nulo")
    void shouldSendNullNameFilterWhenEmployeeNameIsNull() {
        UUID companyId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        when(repository.findAllByCompanyId(pageable, companyId, null))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        provider.findAllByCompanyId(pageable, null, companyId);

        verify(repository).findAllByCompanyId(pageable, companyId, null);
    }

    @Test
    @DisplayName("save: converte domínio e delega persistência ao repository")
    void shouldSaveApprovalRequest() {
        TimeRecordApprovalRequest request = approvalRequest(10L);

        provider.save(request);

        verify(repository).save(TimeRecordApprovalEntity.fromDomain(request));
    }

    @Test
    @DisplayName("findByTimeRecordId: retorna Optional vazio quando não encontrar")
    void shouldReturnEmptyWhenApprovalDoesNotExist() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        var result = provider.findByTimeRecordId(10L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByTimeRecordId: converte entidade para domínio quando encontrar")
    void shouldMapEntityToDomainWhenFindingById() {
        TimeRecordApprovalRequest request = approvalRequest(11L);
        when(repository.findById(11L)).thenReturn(Optional.of(TimeRecordApprovalEntity.fromDomain(request)));

        var result = provider.findByTimeRecordId(11L);

        assertTrue(result.isPresent());
        assertEquals(request.timeRecordId(), result.get().timeRecordId());
        assertEquals(request.requestingEmployeeId(), result.get().requestingEmployeeId());
        assertEquals(request.managerId(), result.get().managerId());
    }

    @Test
    @DisplayName("deleteByTimeRecordId: delega exclusão por id")
    void shouldDeleteApprovalByTimeRecordId() {
        provider.deleteByTimeRecordId(77L);

        verify(repository).deleteById(77L);
    }

    private TimeRecordApprovalRequest approvalRequest(Long timeRecordId) {
        return new TimeRecordApprovalRequest(
                timeRecordId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 4, 10, 8, 0),
                LocalDateTime.of(2026, 4, 10, 17, 0),
                LocalDateTime.of(2026, 4, 10, 18, 0)
        );
    }
}
