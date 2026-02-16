package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LegalController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa filtros de segurança para focar na lógica do Controller
class LegalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AdfUseCase afdUseCase;
    @MockitoBean private AejUseCase aejUseCase;
    @MockitoBean private PointMirrorPdfUseCase pointMirrorPdfUseCase;
    @MockitoBean private JwtAuthenticatedUser jwtAuthenticatedUser;
    @MockitoBean private EmployeeProvider employeeProvider;
    @MockitoBean private CompanyProvider companyProvider;
    @MockitoBean private TechnicalCertificatePdfService certificateService;
    @MockitoBean private DigitalSignatureService signatureService;

    private static final String BASE_URL = "/legal";
    private static final UUID EMP_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private Employee employeeMock;
    private Company companyMock;

    @BeforeEach
    void setup() {
        // Setup básico de objetos
        employeeMock = mock(Employee.class);
        companyMock = mock(Company.class);

        // Comportamento padrão para recuperar a empresa do usuário logado
        // Isso resolve a lógica do método privado 'getCompanyIdFromLoggedUser'
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(EMP_ID);
        when(employeeProvider.findById(EMP_ID)).thenReturn(Optional.of(employeeMock));
        when(employeeMock.companyId()).thenReturn(COMPANY_ID);
        when(companyProvider.findById(COMPANY_ID)).thenReturn(Optional.of(companyMock));
    }

    // ==================================================================================
    // 1. ATESTADO TÉCNICO (Technical Certificate)
    // ==================================================================================

    @Test
    @DisplayName("Deve baixar Atestado Técnico assinado com sucesso (200 OK)")
    void shouldDownloadTechnicalCertificateSuccessfully() throws Exception {
        byte[] pdfBytes = "PDF_RAW".getBytes();
        byte[] signedBytes = "PDF_SIGNED_P7S".getBytes();

        when(certificateService.generateCertificate(any(Company.class))).thenReturn(pdfBytes);
        when(signatureService.signData(pdfBytes)).thenReturn(signedBytes);

        mockMvc.perform(get(BASE_URL + "/technical-certificate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pkcs7-signature"))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"Atestado_Tecnico_Kronos")))
                .andExpect(content().bytes(signedBytes));

        verify(signatureService).signData(pdfBytes);
    }

    @Test
    @DisplayName("Deve retornar 404 se a empresa não for encontrada ao gerar Atestado")
    void shouldReturn404WhenCompanyNotFoundForCertificate() throws Exception {
        when(companyProvider.findById(COMPANY_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/technical-certificate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Empresa não encontrada"));
    }

    // ==================================================================================
    // 2. ARQUIVO FONTE DE DADOS (AFD)
    // ==================================================================================

    @Test
    @DisplayName("Deve baixar arquivo AFD com sucesso (200 OK)")
    void shouldDownloadAfdSuccessfully() throws Exception {
        // O método writeAfdToStream escreve no OutputStream da resposta.
        // Simulamos isso usando doAnswer para escrever no stream quando o mock for chamado.
        doAnswer(invocation -> {
            var outputStream = (java.io.OutputStream) invocation.getArgument(1);
            outputStream.write("CONTEUDO_DO_AFD".getBytes());
            return null;
        }).when(afdUseCase).writeAfdToStream(eq(COMPANY_ID), any());

        mockMvc.perform(get(BASE_URL + "/afd"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"AFD_")))
                .andExpect(content().string("CONTEUDO_DO_AFD"));
    }


    // ==================================================================================
    // 3. ARQUIVO ELETRÔNICO DE JORNADA (AEJ)
    // ==================================================================================

    @Test
    @DisplayName("Deve baixar arquivo AEJ assinado com sucesso (200 OK)")
    void shouldDownloadAejSuccessfully() throws Exception {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        doAnswer(invocation -> {
            var outputStream = (java.io.OutputStream) invocation.getArgument(3);
            outputStream.write("CONTEUDO_AEJ_P7S".getBytes());
            return null;
        }).when(aejUseCase).generateAej(eq(COMPANY_ID), eq(start), eq(end), any());

        mockMvc.perform(get(BASE_URL + "/aej")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pkcs7-signature"))
                .andExpect(header().string("Content-Disposition", containsString(".p7s")))
                .andExpect(content().string("CONTEUDO_AEJ_P7S"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se datas não forem informadas no AEJ")
    void shouldReturn400WhenDatesAreMissingForAej() throws Exception {
        mockMvc.perform(get(BASE_URL + "/aej"))
                .andExpect(status().isBadRequest());
    }

    // ==================================================================================
    // 4. ESPELHO DE PONTO (Point Mirror)
    // ==================================================================================

    @Test
    @DisplayName("Deve baixar Espelho de Ponto (Usuário Comum) com sucesso (200 OK)")
    void shouldDownloadMirrorForSelfSuccessfully() throws Exception {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        byte[] pdfBytes = "PDF_ESPELHO".getBytes();

        // Cenário: Usuário comum (não MANAGER), baixa seu próprio espelho
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("EMPLOYEE");
        when(pointMirrorPdfUseCase.generateMirror(EMP_ID, start, end)).thenReturn(pdfBytes);

        mockMvc.perform(get(BASE_URL + "/espelho-ponto")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("Deve baixar Espelho de Ponto de outro funcionário (Manager) com sucesso (200 OK)")
    void shouldDownloadMirrorForTargetEmployeeAsManager() throws Exception {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        UUID targetId = UUID.randomUUID();
        byte[] pdfBytes = "PDF_TARGET".getBytes();

        // Cenário: Manager solicitando espelho de outro ID
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(pointMirrorPdfUseCase.generateMirror(targetId, start, end)).thenReturn(pdfBytes);

        mockMvc.perform(get(BASE_URL + "/espelho-ponto")
                        .param("targetEmployeeId", targetId.toString())
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("Deve retornar 404 se o colaborador não for encontrado (Método Auxiliar falha)")
    void shouldReturn404WhenEmployeeNotFoundInHelper() throws Exception {
        // Simula falha no método privado getCompanyIdFromLoggedUser()
        when(employeeProvider.findById(EMP_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/afd")) // Qualquer endpoint que use o helper
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Colaborador não encontrado."));
    }

}