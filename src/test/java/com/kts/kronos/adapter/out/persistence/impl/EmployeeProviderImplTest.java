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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("save: deve converter domínio para entidade e retornar domínio salvo")
    void shouldSaveEmployee() {
        EmployeeEntity entity = employeeEntity("12345678901");
        when(repository.save(any(EmployeeEntity.class))).thenReturn(entity);

        var result = provider.save(entity.toDomain());

        assertEquals(entity.getEmployeeId(), result.employeeId());
        assertEquals(entity.getCpf(), result.cpf());
        verify(repository).save(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("findById/findAll/delete: devem delegar e mapear resultados")
    void shouldDelegateSimpleRepositoryMethods() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity entity = employeeEntity("12345678901");
        entity.setEmployeeId(employeeId);

        when(repository.findById(employeeId)).thenReturn(Optional.of(entity));
        when(repository.findAll()).thenReturn(List.of(entity));

        assertTrue(provider.findById(employeeId).isPresent());
        assertEquals(List.of(employeeId), provider.findAll().stream().map(com.kts.kronos.domain.model.Employee::employeeId).toList());

        provider.deleteById(employeeId);

        verify(repository).deleteById(employeeId);
    }

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
    @DisplayName("findByCpf: deve retornar vazio sem consultar quando CPF é nulo ou vazio")
    void shouldReturnEmptyForNullOrBlankCpf() {
        assertTrue(provider.findByCpf(null).isEmpty());
        assertTrue(provider.findByCpf(" ").isEmpty());
        verify(repository, never()).findByCpf(any());
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

    @Test
    @DisplayName("consultas por empresa e lote devem mapear entidades")
    void shouldMapCompanyAndBatchQueries() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity entity = employeeEntity("12345678901");
        entity.setEmployeeId(employeeId);
        entity.setCompanyId(companyId);

        when(repository.findByCompanyId(companyId)).thenReturn(List.of(entity));
        when(repository.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of(entity));
        when(repository.countByCompanyIdAndActive(companyId, true)).thenReturn(7L);
        when(repository.findAllById(List.of(employeeId))).thenReturn(List.of(entity));

        assertEquals(1, provider.findByCompanyId(companyId).size());
        assertEquals(1, provider.findByCompanyIdAndActive(companyId, true).size());
        assertEquals(7L, provider.countByCompanyIdAndActive(companyId, true));
        assertEquals(List.of(employeeId), provider.findAllByIds(List.of(employeeId)).stream()
                .map(com.kts.kronos.domain.model.Employee::employeeId)
                .toList());
    }

    @Test
    @DisplayName("countByCompanyIds: evita consulta para entrada nula ou vazia")
    void shouldAvoidCountQueryForNullOrEmptyCompanyIds() {
        assertEquals(List.of(), provider.countByCompanyIds(null));
        assertEquals(List.of(), provider.countByCompanyIds(List.of()));
        verify(repository, never()).countByCompanyIds(any());
    }

    @Test
    @DisplayName("countByCompanyIds: delega quando há empresas")
    void shouldDelegateCountByCompanyIds() {
        UUID companyId = UUID.randomUUID();
        when(repository.countByCompanyIds(List.of(companyId))).thenReturn(List.of());

        assertEquals(List.of(), provider.countByCompanyIds(List.of(companyId)));

        verify(repository).countByCompanyIds(List.of(companyId));
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

    // ── cpfExistsInCompany ───────────────────────────────────────────────────

    @Test
    @DisplayName("cpfExistsInCompany: retorna true quando repositorio encontra CPF na empresa")
    void cpfExistsInCompany_returnsTrue_whenRepositoryFinds() {
        UUID companyId = UUID.randomUUID();
        String digits = "12345678901";
        String masked = "123.456.789-01";

        when(repository.existsByCompanyIdAndCpfAndDeletedAtIsNull(companyId, digits)).thenReturn(false);
        when(repository.existsByCompanyIdAndCpfAndDeletedAtIsNull(companyId, masked)).thenReturn(true);

        assertTrue(provider.cpfExistsInCompany(companyId, digits));
    }

    @Test
    @DisplayName("cpfExistsInCompany: retorna false quando nenhum candidato encontrado")
    void cpfExistsInCompany_returnsFalse_whenNotFound() {
        UUID companyId = UUID.randomUUID();
        String digits = "12345678901";

        when(repository.existsByCompanyIdAndCpfAndDeletedAtIsNull(eq(companyId), any())).thenReturn(false);

        assertFalse(provider.cpfExistsInCompany(companyId, digits));
    }

    // ── findByCompanyIdAndCpf ────────────────────────────────────────────────

    @Test
    @DisplayName("findByCompanyIdAndCpf: retorna presente quando repositorio encontra CPF mascarado")
    void findByCompanyIdAndCpf_returnsPresent_whenRepositoryFinds() {
        UUID companyId = UUID.randomUUID();
        String digits = "12345678901";
        String masked = "123.456.789-01";
        EmployeeEntity entity = employeeEntity(masked);
        entity.setCompanyId(companyId);

        when(repository.findByCompanyIdAndCpfAndDeletedAtIsNull(companyId, digits)).thenReturn(Optional.empty());
        when(repository.findByCompanyIdAndCpfAndDeletedAtIsNull(companyId, masked)).thenReturn(Optional.of(entity));

        var result = provider.findByCompanyIdAndCpf(companyId, digits);

        assertTrue(result.isPresent());
        assertEquals(masked, result.get().cpf());
    }

    @Test
    @DisplayName("findByCompanyIdAndCpf: retorna vazio quando nenhum candidato encontrado")
    void findByCompanyIdAndCpf_returnsEmpty_whenNotFound() {
        UUID companyId = UUID.randomUUID();

        when(repository.findByCompanyIdAndCpfAndDeletedAtIsNull(eq(companyId), any())).thenReturn(Optional.empty());

        assertTrue(provider.findByCompanyIdAndCpf(companyId, "12345678901").isEmpty());
    }

    // ── findAllByCpf ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllByCpf: retorna lista quando repositorio encontra registros")
    void findAllByCpf_returnsList_whenRepositoryFinds() {
        String digits = "12345678901";
        EmployeeEntity entity = employeeEntity(digits);

        when(repository.findAllByCpf(digits)).thenReturn(List.of(entity));

        var result = provider.findAllByCpf(digits);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findAllByCpf: retorna lista vazia quando nenhum candidato encontrado")
    void findAllByCpf_returnsEmpty_whenNotFound() {
        when(repository.findAllByCpf(any())).thenReturn(List.of());

        assertTrue(provider.findAllByCpf("12345678901").isEmpty());
    }

    // ── buildCpfCandidates edge cases ────────────────────────────────────────

    @Test
    @DisplayName("buildCpfCandidates: CPF com menos de 11 digitos nao formata como mascara")
    void cpfExists_shortDigitsCpf_doesNotFormatAsMasked() {
        // CPF com 8 digitos: digits.length() != 11 → nao formata mascara
        String shortCpf = "12345678"; // 8 dígitos - não tem pontuação

        when(repository.existsByCpf(shortCpf)).thenReturn(false);

        boolean result = provider.cpfExists(shortCpf);

        assertFalse(result);
        verify(repository).existsByCpf(shortCpf); // candidate = raw + digits (same), sem mascara
    }

    @Test
    @DisplayName("buildCpfCandidates: CPF sem digitos (apenas letras) gera somente candidato raw")
    void cpfExists_noDigitsCpf_returnsOnlyRawCandidate() {
        // CPF sem dígitos: digits="" -> !digits.isEmpty() = false -> candidates = [raw apenas]
        String noDigitsCpf = "abc-def";

        when(repository.existsByCpf(noDigitsCpf)).thenReturn(false);

        boolean result = provider.cpfExists(noDigitsCpf);

        assertFalse(result);
        verify(repository).existsByCpf(noDigitsCpf);
    }

}