package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeProviderImplTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeProviderImpl provider;

    @Test
    @DisplayName("findByCpf: deve localizar registro com CPF mascarado quando entrada vier sem máscara")
    void shouldFindByCpfWhenDatabaseStoresMaskedValue() {
        String digits = "12345678901";
        String masked = "123.456.789-01";

        when(repository.findByCpf(digits)).thenReturn(Optional.empty());
        when(repository.findByCpf(masked)).thenReturn(Optional.of(employeeEntity(masked)));

        var result = provider.findByCpf(digits);

        assertTrue(result.isPresent());
        assertTrue(result.get().cpf().equals(masked));
    }

    @Test
    @DisplayName("findByCpf: deve localizar registro com CPF sem máscara quando entrada vier mascarada")
    void shouldFindByCpfWhenDatabaseStoresDigitsOnlyValue() {
        String digits = "12345678901";
        String masked = "123.456.789-01";

        when(repository.findByCpf(masked)).thenReturn(Optional.empty());
        when(repository.findByCpf(digits)).thenReturn(Optional.of(employeeEntity(digits)));

        var result = provider.findByCpf(masked);

        assertTrue(result.isPresent());
        assertTrue(result.get().cpf().equals(digits));
    }

    @Test
    @DisplayName("cpfExists: deve considerar CPF existente mesmo com diferença de máscara")
    void shouldReportCpfExistsAcrossMaskedAndUnmaskedRepresentations() {
        String digits = "12345678901";
        String masked = "123.456.789-01";

        when(repository.existsByCpf(digits)).thenReturn(false);
        when(repository.existsByCpf(masked)).thenReturn(true);

        boolean exists = provider.cpfExists(digits);

        assertTrue(exists);
        verify(repository).existsByCpf(digits);
        verify(repository).existsByCpf(masked);
    }

    @Test
    @DisplayName("cpfExists: deve retornar falso para CPF vazio sem consultar o banco")
    void shouldReturnFalseForBlankCpfWithoutQueryingRepository() {
        boolean exists = provider.cpfExists("   ");

        assertFalse(exists);
        verify(repository, never()).existsByCpf("   ");
    }

    private EmployeeEntity employeeEntity(String cpf) {
        return EmployeeEntity.builder()
                .employeeId(UUID.randomUUID())
                .fullName("Pessoa Teste")
                .cpf(cpf)
                .jobPosition("Analista")
                .email("teste@kts.com")
                .salary(1000.0)
                .phone("21999999999")
                .companyId(UUID.randomUUID())
                .build();
    }
}
