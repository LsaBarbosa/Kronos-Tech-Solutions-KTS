package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.TimeRecordApprovalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

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
}
