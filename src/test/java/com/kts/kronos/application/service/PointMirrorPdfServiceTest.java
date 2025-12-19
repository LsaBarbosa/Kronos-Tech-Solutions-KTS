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

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointMirrorPdfServiceTest {

    @Mock private CompanyProvider companyProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private TimeRecordProvider recordRepository;
    @InjectMocks private PointMirrorPdfService pointMirrorPdfService;

    @Test
    @DisplayName("Deve gerar PDF do Espelho de Ponto")
    void shouldGenerateMirrorPdf() {
        UUID empId = UUID.randomUUID();
        UUID compId = UUID.randomUUID();

        Employee employee = mock(Employee.class);
        // --- CORREÇÃO AQUI ---
        when(employee.employeeId()).thenReturn(empId); // Configura o retorno do ID!
        // ---------------------
        when(employee.companyId()).thenReturn(compId);
        when(employee.fullName()).thenReturn("Maria Teste");
        when(employee.cpf()).thenReturn("12345678900"); // Adicionei CPF pois o PDF usa
        when(employee.getDailyWorkMinutes()).thenReturn(480L);

        Company company = mock(Company.class);
        when(company.name()).thenReturn("Empresa SA");
        when(company.cnpj()).thenReturn("00000000000100"); // Adicionei CNPJ pois o PDF usa

        when(employeeProvider.findById(empId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(compId)).thenReturn(Optional.of(company));

        // Configura o repositório para aceitar exatamente o empId configurado acima
        when(recordRepository.findByEmployeeId(empId)).thenReturn(Collections.emptyList());

        byte[] pdfBytes = pointMirrorPdfService.generateMirror(empId, LocalDate.now(), LocalDate.now());

        assertTrue(pdfBytes.length > 0);
        assertTrue(new String(pdfBytes, 0, 4).startsWith("%PDF"));
    }
}