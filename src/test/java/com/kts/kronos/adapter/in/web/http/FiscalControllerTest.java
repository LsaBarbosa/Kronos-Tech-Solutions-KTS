package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase; // <--- NOVO IMPORT (Interface)
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.service.AejService;
import com.kts.kronos.application.service.TechnicalCertificateService;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FiscalController.class)
@AutoConfigureMockMvc(addFilters = false)
class FiscalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdfUseCase afdUseCase;

    @MockBean
    private AejService aejService;

    @MockBean
    private TechnicalCertificateService certificateService;

    // --- CORREÇÃO AQUI ---
    // O Controller espera a Interface (UseCase), não a classe Service
    @MockBean
    private PointMirrorPdfUseCase pointMirrorPdfUseCase;
    // ---------------------

    @MockBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @MockBean
    private EmployeeProvider employeeProvider;

    private UUID empId;
    private UUID compId;

    @BeforeEach
    void setup() {
        empId = UUID.randomUUID();
        compId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empId);

        Employee employee = mock(Employee.class);
        when(employee.companyId()).thenReturn(compId);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(employee));
    }

    @Test
    @DisplayName("GET /fiscal/afd - Deve baixar arquivo AFD com headers corretos")
    void shouldDownloadAfd() throws Exception {
        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(1);
            out.write("AFD CONTENT".getBytes());
            return null;
        }).when(afdUseCase).writeAfdToStream(eq(compId), any(OutputStream.class));

        mockMvc.perform(get("/api/v1/fiscal/afd"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"AFD.txt\""))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("AFD CONTENT"));

        verify(afdUseCase).writeAfdToStream(eq(compId), any(OutputStream.class));
    }

    @Test
    @DisplayName("GET /fiscal/aej - Deve baixar arquivo AEJ para o período informado")
    void shouldDownloadAej() throws Exception {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(3);
            out.write("AEJ CONTENT".getBytes());
            return null;
        }).when(aejService).generateAej(eq(compId), eq(start), eq(end), any(OutputStream.class));

        mockMvc.perform(get("/api/v1/fiscal/aej")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"AEJ_2025-01-01_2025-01-31.txt\""))
                .andExpect(content().string("AEJ CONTENT"));
    }

    @Test
    @DisplayName("GET /fiscal/atestado-tecnico - Deve baixar PDF do atestado")
    void shouldDownloadCertificate() throws Exception {
        byte[] pdfContent = "%PDF-1.4...".getBytes();
        when(certificateService.generateCertificate(compId)).thenReturn(pdfContent);

        mockMvc.perform(get("/api/v1/fiscal/atestado-tecnico"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Atestado_Tecnico_REP_P.pdf\""))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("GET /fiscal/espelho-ponto - Deve baixar PDF do Espelho")
    void shouldDownloadPointMirror() throws Exception {
        byte[] pdfContent = "%PDF-Mirror...".getBytes();
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        // Configura o Mock usando a variável corrigida (UseCase)
        when(pointMirrorPdfUseCase.generateMirror(eq(empId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(pdfContent);

        mockMvc.perform(get("/api/v1/fiscal/espelho-ponto")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(content().bytes(pdfContent));
    }
}