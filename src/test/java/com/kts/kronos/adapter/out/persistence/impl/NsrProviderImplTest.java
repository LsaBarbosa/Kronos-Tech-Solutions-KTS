package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.CompanyNsrRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NsrProviderImplTest {

    @Mock
    private CompanyNsrRepository repository;

    @InjectMocks
    private NsrProviderImpl provider;

    @Test
    @DisplayName("generateNextNsr: deve delegar ao repository e retornar valor válido")
    void shouldDelegateAndReturnNextNsr() {
        UUID companyId = UUID.randomUUID();
        when(repository.incrementAndGetNsr(companyId)).thenReturn(7L);

        Long result = provider.generateNextNsr(companyId);

        assertEquals(7L, result);
        verify(repository).incrementAndGetNsr(companyId);
    }
}