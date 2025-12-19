package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.AfdEntry;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AfdServiceTest {

    @Mock private AfdEntryProvider afdProvider;
    @Mock private CompanyProvider companyProvider;
    @InjectMocks private AfdService afdService;

    @Test
    @DisplayName("Deve gerar hash SHA-256 e salvar entrada AFD corretamente")
    void shouldLogMarkingWithHash() {
        // Dados de entrada
        Company company = mock(Company.class);
        when(company.companyId()).thenReturn(UUID.randomUUID());
        
        Employee employee = mock(Employee.class);
        when(employee.cpf()).thenReturn("123.456.789-00");
        when(employee.employeeId()).thenReturn(UUID.randomUUID());

        LocalDateTime now = LocalDateTime.of(2025, 12, 16, 10, 0, 0);
        Long nsr = 1L;

        // Mock do hash anterior
        when(afdProvider.findLastHashByCompanyId(any())).thenReturn(Optional.of("HASH_ANTERIOR"));

        // Execução
        afdService.logMarking(company, employee, now, nsr);

        // Verificação
        ArgumentCaptor<AfdEntry> captor = ArgumentCaptor.forClass(AfdEntry.class);
        verify(afdProvider).save(captor.capture());
        AfdEntry saved = captor.getValue();

        assertEquals(nsr, saved.nsr());
        assertEquals("7", saved.recordType()); // Tipo 7 = Marcação
        assertEquals("HASH_ANTERIOR", saved.previousHash());
        assertNotNull(saved.currentHash());
        assertNotEquals("", saved.currentHash());
        
        // Verifica se o CPF foi limpo (apenas números) para o log
        assertEquals("123.456.789-00", saved.employeeCpf()); 
    }

    @Test
    @DisplayName("Deve gerar arquivo AFD (txt) com formatação correta")
    void shouldGenerateAfdTxtFile() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company(companyId, "Empresa Teste", "12345678000199", "email@teste.com", true, null, null, 0L, 0L);
        
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        // Mock do Stream de registros
        AfdEntry entry = new AfdEntry(1L, "7", LocalDateTime.of(2025, 12, 16, 8, 0), "12345678900", null, companyId, UUID.randomUUID(), "HASH1", "HASH2");
        when(afdProvider.streamByCompanyIdOrderByNsr(companyId)).thenReturn(Stream.of(entry));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Execução
        afdService.writeAfdToStream(companyId, outputStream);
        
        String content = outputStream.toString();
        String[] lines = content.split("\r\n");

        // Validações do Layout (Portaria 671)
        // Linha 1: Cabeçalho (Tipo 000000001 + TipoId 1 + CNPJ 14 + Razão Social 150)
        assertTrue(lines[0].startsWith("0000000011112345678000199Empresa Teste"), "Cabeçalho incorreto: " + lines[0]);
        
        // Linha 2: Registro Tipo 7
        // NSR(9) + Tipo(1) + Data(ddMMyyyyHHmm) + CPF(12)
        // 000000001 + 7 + 161220250800 + 012345678900
        String expectedLine2Start = "0000000017161220250800012345678900";
        assertTrue(lines[1].startsWith(expectedLine2Start), "Registro de ponto incorreto: " + lines[1]);

        // Linha 3: Trailer
        assertEquals("999999999", lines[2], "Trailer incorreto");
    }
}