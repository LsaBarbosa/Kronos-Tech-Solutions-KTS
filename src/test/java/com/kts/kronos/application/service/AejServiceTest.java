package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AejServiceTest {

    @Mock private CompanyProvider companyProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private TimeRecordProvider recordRepository;
    @InjectMocks private AejService aejService;

    @Test
    @DisplayName("Deve gerar arquivo AEJ com estrutura hierárquica correta")
    void shouldGenerateAejStructure() {
        // 1. Mock Empresa
        UUID companyId = UUID.randomUUID();
        Company company = new Company(companyId, "Empresa KTS", "12345678000199", "contato@kts.com", true, null, null, 0L, 0L);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        // 2. Mock Funcionário
        UUID empId = UUID.randomUUID();
        // Ajuste: Certifique-se de que o construtor do Employee no teste bata com o seu record atual (com workStartTime, etc)
        Employee employee = new Employee(empId, "Funcionario Teste", "111.222.333-44", "","Dev", "f@k.com", 5000.0, null, true, null, companyId, null, false, null,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(12, 0), LocalTime.of(13, 0));

        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));

        // 3. Mock Ponto
        LocalDateTime originalStart = LocalDateTime.of(2025, 10, 1, 9, 5, 0);
        LocalDateTime treatedStart = LocalDateTime.of(2025, 10, 1, 9, 0, 0);

        TimeRecord record = new TimeRecord(1L, treatedStart, null, StatusRecord.PENDING, true, true, empId, null, null, null, null, 100L, null, originalStart, null);

        when(recordRepository.findByEmployeeId(empId)).thenReturn(List.of(record));

        // Execução
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        aejService.generateAej(companyId, LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 31), out);

        String content = out.toString();
        String[] lines = content.split("\r\n");

        // --- VALIDAÇÕES CORRIGIDAS ---

        // Registro 01: Cabeçalho (Verifica o início da linha, sem pipe no começo)
        // Esperado: 01|1|12345678000199||Empresa KTS|...
        assertTrue(lines[0].startsWith("01|1|12345678000199||Empresa KTS|"),
                "Falha no Cabeçalho (Tipo 01). Gerado: " + lines[0]);

        // Registro 02: REPs (Busca no conteúdo total, sem pipe inicial pois pode estar no meio do arquivo após \r\n)
        assertTrue(content.contains("02|001|4|"),
                "Falha no Registro de REP (Tipo 02)");

        // Registro 03: Vínculo
        assertTrue(content.contains("03|000000001|11122233344|Funcionario Teste|"),
                "Falha no Vínculo (Tipo 03)");

        // Registro 04: Horário
        assertTrue(content.contains("04|H000000001|480|0900|1200|1300|1800|"),
                "Falha no Horário Contratual (Tipo 04)");

        // Registro 05: Marcação
        // Formato data esperado: 2025-10-01T09:00:00-0300
        assertTrue(content.contains("05|000000001|2025-10-01T09:00:00-0300|001|E||I|H000000001||"),
                "Falha na Marcação (Tipo 05)");
    }
}