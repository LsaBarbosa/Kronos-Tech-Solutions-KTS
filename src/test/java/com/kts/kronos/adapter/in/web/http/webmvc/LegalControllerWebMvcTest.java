package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.http.LegalController;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LegalController.class)
@AutoConfigureMockMvc(addFilters = false)
class LegalControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdfUseCase afdUseCase;

    @MockitoBean
    private AejUseCase aejUseCase;

    @MockitoBean
    private PointMirrorPdfUseCase pointMirrorPdfUseCase;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @MockitoBean
    private EmployeeProvider employeeProvider;

    @MockitoBean
    private CompanyProvider companyProvider;

    @MockitoBean
    private DomainAuthorizationService domainAuthorizationService;

    @MockitoBean
    private TechnicalCertificatePdfService certificateService;

    @MockitoBean
    private DigitalSignatureService signatureService;

    @MockitoBean
    private ObservabilityStatusUseCase observabilityStatusUseCase;

    @MockitoBean
    private KronosMetrics kronosMetrics;

    @MockitoBean
    private KronosTracing kronosTracing;

    @BeforeEach
    void setUpTracing() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(kronosTracing).observe(any(String.class), any(Runnable.class));

        doAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        }).when(kronosTracing).observe(any(String.class), any(Supplier.class));
    }

    @Test
    @DisplayName("downloadTechnicalCertificate: deve retornar .p7s com header correto")
    void shouldDownloadTechnicalCertificate() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = employee(loggedEmployeeId, companyId);
        var company = company(companyId);

        byte[] pdfBytes = "pdf".getBytes(StandardCharsets.UTF_8);
        byte[] signedBytes = "signed-p7s".getBytes(StandardCharsets.UTF_8);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(certificateService.generateCertificate(company)).thenReturn(pdfBytes);
        when(signatureService.signData(pdfBytes)).thenReturn(signedBytes);

        mockMvc.perform(get("/legal/technical-certificate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pkcs7-signature"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Atestado_Tecnico_Kronos_" + LocalDate.now().getYear() + ".p7s\""
                ))
                .andExpect(content().bytes(signedBytes));
    }

    @Test
    @DisplayName("downloadAfd: deve retornar txt com header correto e delegar para o use case")
    void shouldDownloadAfd() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = employee(loggedEmployeeId, companyId);
        byte[] afdBytes = "AFD-CONTENT".getBytes(StandardCharsets.UTF_8);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));

        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(1);
            outputStream.write(afdBytes);
            return null;
        }).when(afdUseCase).writeAfdToStream(eq(companyId), any(OutputStream.class));

        mockMvc.perform(get("/legal/afd"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"AFD_%s.txt\"".formatted(companyId)
                ))
                .andExpect(content().bytes(afdBytes));

        verify(afdUseCase).writeAfdToStream(eq(companyId), any(OutputStream.class));
    }

    @Test
    @DisplayName("downloadAej: deve retornar .p7s com header correto e repassar parâmetros")
    void shouldDownloadAej() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = employee(loggedEmployeeId, companyId);
        byte[] aejBytes = "AEJ-CONTENT".getBytes(StandardCharsets.UTF_8);
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));

        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(3);
            outputStream.write(aejBytes);
            return null;
        }).when(aejUseCase).generateAej(eq(companyId), eq(startDate), eq(endDate), any(OutputStream.class));

        mockMvc.perform(get("/legal/aej")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pkcs7-signature"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"AEJ_%s_%s.p7s\"".formatted(startDate, endDate)
                ))
                .andExpect(content().bytes(aejBytes));

        verify(aejUseCase).generateAej(eq(companyId), eq(startDate), eq(endDate), any(OutputStream.class));
    }

    @Test
    @DisplayName("downloadMirror: deve gerar espelho para o próprio colaborador quando targetEmployeeId não é informado")
    void shouldDownloadMirrorWithoutTargetEmployeeId() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);
        byte[] pdfBytes = "PDF-MIRROR".getBytes(StandardCharsets.UTF_8);
        var authorizedEmployee = employee(employeeId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(authorizedEmployee);
        when(pointMirrorPdfUseCase.generateMirror(employeeId, startDate, endDate)).thenReturn(pdfBytes);

        mockMvc.perform(get("/legal/espelho-ponto")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Espelho_%s_%s.pdf\"".formatted(startDate, endDate)
                ))
                .andExpect(content().bytes(pdfBytes));

        verify(domainAuthorizationService).authorizeEmployeeAccess(null);
        verify(pointMirrorPdfUseCase).generateMirror(employeeId, startDate, endDate);
    }

    @Test
    @DisplayName("downloadMirror: deve gerar espelho para targetEmployeeId informado")
    void shouldDownloadMirrorWithTargetEmployeeId() throws Exception {
        UUID targetEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);
        byte[] pdfBytes = "PDF-MIRROR-TARGET".getBytes(StandardCharsets.UTF_8);
        var targetEmployee = employee(targetEmployeeId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId)).thenReturn(targetEmployee);
        when(pointMirrorPdfUseCase.generateMirror(targetEmployeeId, startDate, endDate)).thenReturn(pdfBytes);

        mockMvc.perform(get("/legal/espelho-ponto")
                        .param("targetEmployeeId", targetEmployeeId.toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Espelho_%s_%s.pdf\"".formatted(startDate, endDate)
                ))
                .andExpect(content().bytes(pdfBytes));

        verify(domainAuthorizationService).authorizeEmployeeAccess(targetEmployeeId);
        verify(pointMirrorPdfUseCase).generateMirror(targetEmployeeId, startDate, endDate);
    }

    @Test
    @DisplayName("downloadAfd: deve traduzir exceção quando colaborador autenticado não existe")
    void shouldReturnNotFoundWhenLoggedEmployeeDoesNotExist() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/legal/afd"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.detail")
                        .value("Colaborador não encontrado."));
    }

    @Test
    @DisplayName("downloadTechnicalCertificate: deve traduzir empresa inexistente")
    void shouldReturnNotFoundWhenCertificateCompanyDoesNotExist() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = employee(loggedEmployeeId, companyId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/legal/technical-certificate"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.detail")
                        .value("Empresa não encontrada"));
    }

    @Test
    @DisplayName("downloadTechnicalCertificate: deve retornar erro padronizado quando assinatura falhar")
    void shouldReturnStandardErrorWhenTechnicalCertificateSignatureFails() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = employee(loggedEmployeeId, companyId);
        var company = company(companyId);
        byte[] pdfBytes = "pdf".getBytes(StandardCharsets.UTF_8);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(certificateService.generateCertificate(company)).thenReturn(pdfBytes);
        when(signatureService.signData(pdfBytes)).thenThrow(new RuntimeException("sign failed"));

        mockMvc.perform(get("/legal/technical-certificate"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code")
                        .value("INTERNAL_ERROR"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Erro inesperado"));
    }

    @Test
    @DisplayName("downloadMirror: deve traduzir bloqueio de autorização")
    void shouldReturnForbiddenWhenMirrorTargetIsForbidden() throws Exception {
        UUID targetEmployeeId = UUID.randomUUID();
        doThrow(new ForbiddenException("Acesso negado"))
                .when(domainAuthorizationService).authorizeEmployeeAccess(targetEmployeeId);

        mockMvc.perform(get("/legal/espelho-ponto")
                        .param("targetEmployeeId", targetEmployeeId.toString())
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.detail")
                        .value("Acesso negado"));
    }

    private Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Lucas Silva",
                "52998224725",
                "12345678901",
                "Software Engineer",
                "lucas@kronos.com",
                6500.00,
                "21999999999",
                true,
                new Address("Rua A", "100", "12345678", "Rio de Janeiro", "RJ"),
                companyId,
                LocalDateTime.of(2026, 1, 10, 8, 0),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Company company(UUID companyId) {
        return new Company(
                companyId,
                "Kronos Tech",
                "12345678000199",
                "empresa@kronos.com",
                true,
                new Address("Rua A", "100", "12345678", "Rio de Janeiro", "RJ"),
                null,
                0,
                0
        );
    }
}
