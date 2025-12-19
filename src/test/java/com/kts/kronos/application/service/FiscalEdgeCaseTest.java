package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiscalEdgeCaseTest {

    @Mock private CompanyProvider companyProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private TimeRecordProvider recordRepository;
    @InjectMocks private AejService aejService;

    @Test
    @DisplayName("AEJ: Deve processar corretamente período de virada de ano")
    void shouldHandleYearTransition() {
        UUID companyId = UUID.randomUUID();
        Company company = mock(Company.class);
        when(company.companyId()).thenReturn(companyId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        
        // Simula lista de funcionários vazia ou sem pontos
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(Collections.emptyList());

        // Executa para virada de ano
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        assertDoesNotThrow(() -> 
            aejService.generateAej(companyId, LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1), out)
        );
        
        String content = out.toString();
        // Verifica se as datas no cabeçalho (Registro 01) estão corretas
        // Formato esperado: yyyy-MM-dd
        assertTrue(content.contains("2025-12-31|2026-01-01"));
    }

    @Test
    @DisplayName("AEJ: Não deve quebrar se funcionário não tiver horário cadastrado (Null Safety)")
    void shouldHandleNullWorkHours() {
        UUID companyId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        Company company = mock(Company.class);
        when(company.companyId()).thenReturn(companyId);
        
        // Funcionário com horários NULOS
        Employee employee = new Employee(empId, "Funcionario Sem Horario", "12345678900","", "Cargo", "e@e.com", 100.0, null, true, null, companyId, null, false, null,
                null, null, null, null); // Horários nulos

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(recordRepository.findByEmployeeId(empId)).thenReturn(Collections.emptyList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        aejService.generateAej(companyId, LocalDate.now(), LocalDate.now(), out);
        
        String content = out.toString();
        
        // Deve usar o default 08:00 - 17:00 implementado no AejService para evitar crash
        // Registro 04: ...|480|0800|1200|1300|1700|
// Removido o primeiro '|' antes do 04
        assertTrue(content.contains("04|H000000001|480|0800|1200|1300|1700|"), "Deveria usar fallback para horários nulos");    }
}