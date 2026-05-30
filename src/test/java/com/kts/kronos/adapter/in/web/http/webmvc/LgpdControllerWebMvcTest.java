package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdEmployeeExportResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestAdminListResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestDetailsResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.PublicDataProcessingPurposeResponse;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.LgpdController;
import com.kts.kronos.application.exceptions.CodedForbiddenException;
import com.kts.kronos.application.legal.DataProcessingCatalog;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.retention.RetentionBatchExecutionSummary;
import com.kts.kronos.application.service.retention.RetentionExecutionService;
import com.kts.kronos.domain.model.DataProcessingPurpose;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.DataCategory;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdControllerWebMvcTest.MethodSecurityTestConfig.class})
@WithMockUser(roles = "MANAGER")
class LgpdControllerWebMvcTest {
    @Resource
    private MockMvc mockMvc;

    @Resource
    private LgpdController lgpdController;

    @MockitoBean
    private LgpdUseCase lgpdUseCase;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private DataProcessingCatalog dataProcessingCatalog;

    @MockitoBean
    private RetentionExecutionService retentionExecutionService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    void shouldCreateLgpdRequest() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.createRequest(any(), eq("127.0.0.1"), eq("JUnit"))).thenReturn(new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LgpdRequestType.ACCESS,
                LgpdRequestStatus.OPEN,
                "Exportar meus dados",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        ));

        mockMvc.perform(post("/lgpd/requests")
                        .contentType("application/json")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "type": "ACCESS",
                                  "description": "Exportar meus dados"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldReturnRequestHistory() throws Exception {
        UUID requestId = UUID.randomUUID();

        when(lgpdUseCase.getRequestHistory(requestId)).thenReturn(List.of(
                new LgpdRequestHistory(UUID.randomUUID(), requestId, LgpdRequestStatus.OPEN, "Criada", UUID.randomUUID(), Instant.now())
        ));

        mockMvc.perform(get("/lgpd/requests/{requestId}/history", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].requestId").value(requestId.toString()));
    }

    @Test
    void shouldUpdateRequestStatus() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(lgpdUseCase.updateRequestStatus(eq(requestId), any())).thenReturn(new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LgpdRequestType.ACCESS,
                LgpdRequestStatus.COMPLETED,
                "Exportar meus dados",
                "Atendido",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                UUID.randomUUID(),
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                "Atendido",
                null
        ));

        mockMvc.perform(patch("/lgpd/requests/{requestId}/status", requestId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "COMPLETED",
                                  "notes": "Atendido"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Atendido"));
    }

    @Test
    void shouldExportOwnDataViaMeEndpoint() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportOwnEmployeeData("127.0.0.1", "JUnit"))
                .thenReturn(minimalExportResponse(employeeId, false));

        mockMvc.perform(get("/lgpd/me/export")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.targetEmployeeId").value(employeeId.toString()));

        verify(lgpdUseCase).exportOwnEmployeeData("127.0.0.1", "JUnit");
    }

    @Test
    void shouldExportEmployeeData() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportEmployeeData(eq(employeeId), eq(false), eq("127.0.0.1"), eq("JUnit"), eq(null))).thenReturn(
                new LgpdEmployeeExportResponse(
                        new LgpdEmployeeExportResponse.ExportManifest(UUID.randomUUID(), Instant.now(), userId, employeeId, false,
                                java.util.Arrays.asList("employee", "user", "company", "documents", "timeRecords", "messages", "auditLogs", "legalConsents", "biometricStatus"),
                                java.util.Arrays.asList("Este arquivo contém dados pessoais.")),
                        new LgpdEmployeeExportResponse.ExportedEmployee(
                                employeeId,
                                "Lucas",
                                "12345678901",
                                "98765432100",
                                "Dev",
                                "lucas@kts.com",
                                5000.0,
                                "11999999999",
                                true,
                                new LgpdEmployeeExportResponse.ExportedAddress("Rua A", "100", "01001000", "São Paulo", "SP"),
                                UUID.randomUUID(),
                                LocalDateTime.now(),
                                true,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                Set.of(),
                                null,
                                null,
                                null
                        ),
                        new LgpdEmployeeExportResponse.ExportedUser(
                                UUID.randomUUID(),
                                "lucas",
                                com.kts.kronos.domain.model.enuns.Role.MANAGER,
                                true,
                                employeeId,
                                null,
                                null,
                                null
                        ),
                        new LgpdEmployeeExportResponse.ExportedCompany(
                                UUID.randomUUID(),
                                "KTS",
                                "12345678000199",
                                "contato@kts.com",
                                true,
                                new LgpdEmployeeExportResponse.ExportedAddress("Rua A", "100", "01001000", "São Paulo", "SP"),
                                new Location(-23.0, -46.0),
                                10,
                                1,
                                null,
                                null,
                                null
                        ),
                        List.of(new LgpdEmployeeExportResponse.ExportedDocumentMetadata(
                                UUID.randomUUID(),
                                employeeId,
                                com.kts.kronos.domain.model.enuns.DocumentType.BIOMETRIC_CONSENT_TERM,
                                "termo.pdf",
                                "application/pdf",
                                LocalDateTime.now(),
                                null,
                                false,
                                false,
                                "checksum-123"
                        )),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        new LgpdEmployeeExportResponse.ExportedBiometricStatus(true, true, true, 1),
                        Instant.now()
                )
        );

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", employeeId)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("lucas"))
                .andExpect(jsonPath("$.documents[0].checksumSha256").value("checksum-123"))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        verify(lgpdUseCase).exportEmployeeData(employeeId, false, "127.0.0.1", "JUnit", null);
    }

    @Test
    void shouldForwardPreciseGeolocationFlagToUseCase() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportEmployeeData(eq(employeeId), eq(true), eq("127.0.0.1"), eq("JUnit"), eq(null))).thenReturn(
                new LgpdEmployeeExportResponse(
                        new LgpdEmployeeExportResponse.ExportManifest(UUID.randomUUID(), Instant.now(), userId, employeeId, true,
                                java.util.Arrays.asList("employee", "user", "company", "documents", "timeRecords", "messages", "auditLogs", "legalConsents", "biometricStatus"),
                                java.util.Arrays.asList("Este arquivo contém dados pessoais.")),
                        null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), null, Instant.now()
                )
        );

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", employeeId)
                        .queryParam("includePreciseGeolocation", "true")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk());

        verify(lgpdUseCase).exportEmployeeData(employeeId, true, "127.0.0.1", "JUnit", null);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldBlockDirectThirdPartyExportForManager() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportEmployeeData(eq(employeeId), eq(false), eq("127.0.0.1"), eq("JUnit"), eq("Compliance")))
                .thenThrow(new CodedForbiddenException(
                        "LGPD_EXPORT_REQUIRES_APPROVED_REQUEST",
                        "Exportação de dados de terceiros exige solicitação LGPD aprovada."
                ));

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", employeeId)
                        .queryParam("exportReason", "Compliance")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LGPD_EXPORT_REQUIRES_APPROVED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Exportação de dados de terceiros exige solicitação LGPD aprovada."));
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldBlockDirectThirdPartyExportForCto() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportEmployeeData(eq(employeeId), eq(true), eq("127.0.0.1"), eq("JUnit"), eq("Legal audit")))
                .thenThrow(new CodedForbiddenException(
                        "LGPD_EXPORT_REQUIRES_APPROVED_REQUEST",
                        "Exportação de dados de terceiros exige solicitação LGPD aprovada."
                ));

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", employeeId)
                        .queryParam("includePreciseGeolocation", "true")
                        .queryParam("exportReason", "Legal audit")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LGPD_EXPORT_REQUIRES_APPROVED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Exportação de dados de terceiros exige solicitação LGPD aprovada."));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldExportForApprovedAdminRequest() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportEmployeeDataForApprovedRequest(
                eq(requestId),
                eq(false),
                eq("Art. 7, II, LGPD"),
                eq("Compliance request"),
                eq("Approved by legal"),
                eq("127.0.0.1"),
                eq("JUnit")
        )).thenReturn(minimalExportResponse(employeeId, false));

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                        .contentType("application/json")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "includePreciseGeolocation": false,
                                  "legalBasis": "Art. 7, II, LGPD",
                                  "operationalReason": "Compliance request",
                                  "reviewerNotes": "Approved by legal"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.targetEmployeeId").value(employeeId.toString()));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldExecuteConsentRevocationForAdminRequest() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.executeConsentRevocation(
                eq(requestId),
                eq(ConsentType.BIOMETRIC_AUTHENTICATION),
                eq("Solicitação confirmada pelo titular."),
                eq("127.0.0.1"),
                eq("JUnit")
        )).thenReturn(new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LgpdRequestType.CONSENT_REVOCATION,
                LgpdRequestStatus.COMPLETED,
                "Revogar consentimento biométrico",
                "Consentimento revogado conforme solicitação do titular.",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                UUID.randomUUID(),
                null,
                Instant.now().plusSeconds(86400 * 2),
                "NORMAL",
                null,
                "Consentimento revogado conforme solicitação do titular.",
                "Execução LGPD",
                ConsentType.BIOMETRIC_AUTHENTICATION,
                Instant.now(),
                false
        ));

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/execute-consent-revocation", requestId)
                        .contentType("application/json")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "targetConsentType": "BIOMETRIC_AUTHENTICATION",
                                  "justification": "Solicitação confirmada pelo titular."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestType").value("CONSENT_REVOCATION"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.targetConsentType").value("BIOMETRIC_AUTHENTICATION"))
                .andExpect(jsonPath("$.consentRevocationExecutedAt").exists())
                .andExpect(jsonPath("$.consentRevocationNoActiveConsent").value(false));

        verify(lgpdUseCase).executeConsentRevocation(
                eq(requestId),
                eq(ConsentType.BIOMETRIC_AUTHENTICATION),
                eq("Solicitação confirmada pelo titular."),
                eq("127.0.0.1"),
                eq("JUnit")
        );
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldReturnForbiddenWhenAdminRequestIsNotApprovedForExport() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportEmployeeDataForApprovedRequest(
                eq(requestId),
                eq(false),
                eq("Art. 7, II, LGPD"),
                eq("Compliance request"),
                eq("Waiting legal review"),
                eq("127.0.0.1"),
                eq("JUnit")
        )).thenThrow(new com.kts.kronos.application.exceptions.ForbiddenException(
                "Exportação exige status APPROVED_FOR_EXPORT. Status atual: OPEN"
        ));

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                        .contentType("application/json")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "includePreciseGeolocation": false,
                                  "legalBasis": "Art. 7, II, LGPD",
                                  "operationalReason": "Compliance request",
                                  "reviewerNotes": "Waiting legal review"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Exportação exige status APPROVED_FOR_EXPORT. Status atual: OPEN"));
    }

    @Test
    void shouldAnonymizeEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize", employeeId)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isNoContent());

        verify(lgpdUseCase).anonymizeEmployee(employeeId, "127.0.0.1", "JUnit");
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void shouldForbidPartnerFromAnonymizingEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize", employeeId)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldAllowCtoToAnonymizeEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize", employeeId)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isNoContent());

        verify(lgpdUseCase).anonymizeEmployee(employeeId, "127.0.0.1", "JUnit");
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldListAdminRequestsForCto() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        LgpdRequestAdminListResponse response = new LgpdRequestAdminListResponse(
                requestId,
                "João Silva",
                "Empresa XYZ",
                LgpdRequestType.ACCESS,
                LgpdRequestStatus.OPEN,
                Instant.now(),
                null,
                Instant.now(),
                false
        );

        Page<LgpdRequestAdminListResponse> page = new PageImpl<>(List.of(response));
        when(lgpdUseCase.listAdminRequests(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/lgpd/admin/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.content[0].employeeFullName").value("João Silva"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldListAdminRequestsForManager() throws Exception {
        UUID requestId = UUID.randomUUID();

        LgpdRequestAdminListResponse response = new LgpdRequestAdminListResponse(
                requestId,
                "João Silva",
                "Empresa XYZ",
                LgpdRequestType.ACCESS,
                LgpdRequestStatus.IN_ANALYSIS,
                Instant.now(),
                "admin",
                Instant.now(),
                false
        );

        Page<LgpdRequestAdminListResponse> page = new PageImpl<>(List.of(response));
        when(lgpdUseCase.listAdminRequests(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/lgpd/admin/requests")
                        .queryParam("type", "ACCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("ACCESS"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void shouldForbidEmployeeFromListingAdminRequests() throws Exception {
        mockMvc.perform(get("/lgpd/admin/requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldGetRequestDetails() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        LgpdRequestDetailsResponse response = new LgpdRequestDetailsResponse(
                new com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestResponse(
                        requestId,
                        employeeId,
                        UUID.randomUUID(),
                        companyId,
                        LgpdRequestType.ACCESS,
                        LgpdRequestStatus.OPEN,
                        "Exportar dados",
                        null,
                        Instant.now(),
                        Instant.now(),
                        null,
                        null
                ),
                new com.kts.kronos.adapter.in.web.dto.lgpd.EmployeeSummaryResponse(
                        employeeId,
                        "João Silva",
                        "joao@kts.com",
                        "Dev"
                ),
                new com.kts.kronos.adapter.in.web.dto.lgpd.CompanySummaryResponse(
                        companyId,
                        "12345678000199",
                        "KTS"
                ),
                new com.kts.kronos.adapter.in.web.dto.lgpd.UserSummaryResponse(
                        userId,
                        "admin",
                        com.kts.kronos.domain.model.enuns.Role.CTO
                ),
                List.of()
        );

        when(lgpdUseCase.getRequestDetails(requestId)).thenReturn(response);

        mockMvc.perform(get("/lgpd/admin/requests/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.employee.fullName").value("João Silva"))
                .andExpect(jsonPath("$.company.tradeName").value("KTS"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void shouldForbidEmployeeFromGettingRequestDetails() throws Exception {
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(get("/lgpd/admin/requests/{requestId}", requestId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldReturnProcessingCatalogForCto() throws Exception {
        List<PublicDataProcessingPurposeResponse> catalog = List.of(
                new PublicDataProcessingPurposeResponse(
                        "EMPLOYEE_IDENTIFICATION",
                        DataCategory.IDENTIFICATION,
                        LegalBasis.CONTRACT_EXECUTION,
                        "Gerenciamento da sua identificação para cumprimento do contrato de trabalho.",
                        "RETENTION_EMPLOYEE_CONTRACT",
                        false,
                        true
                ),
                new PublicDataProcessingPurposeResponse(
                        "BIOMETRIC_AUTHENTICATION",
                        DataCategory.BIOMETRIC,
                        LegalBasis.CONSENT,
                        "Autenticação segura com dados biométricos (requer consentimento).",
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        true,
                        true
                )
        );

        when(dataProcessingCatalog.getPublicTreatments()).thenReturn(catalog);

        mockMvc.perform(get("/lgpd/processing-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("EMPLOYEE_IDENTIFICATION"))
                .andExpect(jsonPath("$[0].dataCategory").value("IDENTIFICATION"))
                .andExpect(jsonPath("$[0].legalBasis").value("CONTRACT_EXECUTION"))
                .andExpect(jsonPath("$[0].sensitive").value(false))
                .andExpect(jsonPath("$[1].code").value("BIOMETRIC_AUTHENTICATION"))
                .andExpect(jsonPath("$[1].sensitive").value(true));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldReturnProcessingCatalogForManager() throws Exception {
        List<PublicDataProcessingPurposeResponse> catalog = List.of(
                new PublicDataProcessingPurposeResponse(
                        "EMPLOYEE_IDENTIFICATION",
                        DataCategory.IDENTIFICATION,
                        LegalBasis.CONTRACT_EXECUTION,
                        "Gerenciamento da sua identificação para cumprimento do contrato de trabalho.",
                        "RETENTION_EMPLOYEE_CONTRACT",
                        false,
                        true
                )
        );

        when(dataProcessingCatalog.getPublicTreatments()).thenReturn(catalog);

        mockMvc.perform(get("/lgpd/processing-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("EMPLOYEE_IDENTIFICATION"));
    }

    @Test
    void shouldAllowAnonymousAccessToProcessingCatalog() throws Exception {
        List<PublicDataProcessingPurposeResponse> catalog = List.of(
                new PublicDataProcessingPurposeResponse(
                        "EMPLOYEE_IDENTIFICATION",
                        DataCategory.IDENTIFICATION,
                        LegalBasis.CONTRACT_EXECUTION,
                        "Gerenciamento da sua identificação para cumprimento do contrato de trabalho.",
                        "RETENTION_EMPLOYEE_CONTRACT",
                        false,
                        true
                ),
                new PublicDataProcessingPurposeResponse(
                        "BIOMETRIC_AUTHENTICATION",
                        DataCategory.BIOMETRIC,
                        LegalBasis.CONSENT,
                        "Autenticação segura com dados biométricos (requer consentimento).",
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        true,
                        true
                )
        );

        when(dataProcessingCatalog.getPublicTreatments()).thenReturn(catalog);

        mockMvc.perform(get("/lgpd/processing-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("EMPLOYEE_IDENTIFICATION"))
                .andExpect(jsonPath("$[0].dataCategory").value("IDENTIFICATION"))
                .andExpect(jsonPath("$[0].legalBasis").value("CONTRACT_EXECUTION"))
                .andExpect(jsonPath("$[0].sensitive").value(false))
                .andExpect(jsonPath("$[1].code").value("BIOMETRIC_AUTHENTICATION"))
                .andExpect(jsonPath("$[1].sensitive").value(true));
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void shouldAllowPartnerToAccessProcessingCatalog() throws Exception {
        PublicDataProcessingPurposeResponse purpose = new PublicDataProcessingPurposeResponse(
                "TEST_PURPOSE",
                DataCategory.IDENTIFICATION,
                LegalBasis.CONSENT,
                "Test purpose for LGPD transparency",
                "RETENTION_TEST",
                false,
                true
        );
        when(dataProcessingCatalog.getPublicTreatments()).thenReturn(List.of(purpose));

        mockMvc.perform(get("/lgpd/processing-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TEST_PURPOSE"))
                .andExpect(jsonPath("$[0].purpose").value("Test purpose for LGPD transparency"));
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldExecuteDryRunRetentionForCto() throws Exception {
        List<RetentionDryRunResult> results = List.of(
                new RetentionDryRunResult(
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        "legal_consent",
                        100,
                        0,
                        "PRESERVE_LEGAL_EVIDENCE",
                        true
                ),
                new RetentionDryRunResult(
                        "RETENTION_SECURITY_LOG",
                        "audit_log",
                        5000,
                        250,
                        "MINIMIZE",
                        false
                )
        );

        when(retentionExecutionService.executeActivePolicies(any(), any(), anyBoolean(), any()))
                .thenReturn(new RetentionBatchExecutionSummary("DRY_RUN", results.size(), 5100, 250, 0, true, results));

        mockMvc.perform(get("/lgpd/admin/retention/dry-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyCode").value("RETENTION_BIOMETRIC_ACTIVE_CONSENT"))
                .andExpect(jsonPath("$[0].resourceType").value("legal_consent"))
                .andExpect(jsonPath("$[0].totalScanned").value(100))
                .andExpect(jsonPath("$[0].totalEligible").value(0))
                .andExpect(jsonPath("$[1].policyCode").value("RETENTION_SECURITY_LOG"))
                .andExpect(jsonPath("$[1].totalEligible").value(250));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldForbidManagerFromExecutingDryRunRetention() throws Exception {
        mockMvc.perform(get("/lgpd/admin/retention/dry-run"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void shouldForbidPartnerFromExecutingDryRunRetention() throws Exception {
        mockMvc.perform(get("/lgpd/admin/retention/dry-run"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidAnonymousFromExecutingDryRunRetention() throws Exception {
        mockMvc.perform(get("/lgpd/admin/retention/dry-run"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldApplyRetentionSuccessfully() throws Exception {
        ReflectionTestUtils.setField(lgpdController, "allowApply", true);

        var results = List.of(
                new RetentionDryRunResult(
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        "legal_consent",
                        100,
                        0,
                        "PRESERVE_WHILE_CONSENT_ACTIVE",
                        false
                ),
                new RetentionDryRunResult(
                        "RETENTION_SECURITY_LOG",
                        "audit_log",
                        5000,
                        250,
                        "MINIMIZE",
                        false
                )
        );

        when(retentionExecutionService.executeActivePolicies(any(), any(), anyBoolean(), any()))
                .thenReturn(new RetentionBatchExecutionSummary("APPLY", results.size(), 5100, 250, 0, true, results));

        mockMvc.perform(post("/lgpd/admin/retention/apply")
                .contentType("application/json")
                .content("""
                        {
                          "justification": "Regular schedule retention execution",
                          "confirmed": true
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("APPLY"))
                .andExpect(jsonPath("$.totalPolicies").value(2))
                .andExpect(jsonPath("$.totalScanned").value(5100))
                .andExpect(jsonPath("$.totalEligible").value(250));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldForbidManagerFromApplyingRetention() throws Exception {
        mockMvc.perform(post("/lgpd/admin/retention/apply")
                .contentType("application/json")
                .content("""
                        {
                          "justification": "Test",
                          "confirmed": true
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void shouldForbidPartnerFromApplyingRetention() throws Exception {
        mockMvc.perform(post("/lgpd/admin/retention/apply")
                .contentType("application/json")
                .content("""
                        {
                          "justification": "Test",
                          "confirmed": true
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidAnonymousFromApplyingRetention() throws Exception {
        mockMvc.perform(post("/lgpd/admin/retention/apply")
                .contentType("application/json")
                .content("""
                        {
                          "justification": "Test",
                          "confirmed": true
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldRejectApplyRetentionWithMissingJustification() throws Exception {
        mockMvc.perform(post("/lgpd/admin/retention/apply")
                .contentType("application/json")
                .content("""
                        {
                          "justification": "",
                          "confirmed": true
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldRejectApplyRetentionWithoutConfirmation() throws Exception {
        mockMvc.perform(post("/lgpd/admin/retention/apply")
                .contentType("application/json")
                .content("""
                        {
                          "justification": "Regular execution",
                          "confirmed": false
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldRejectApplyRetentionWhenFeatureFlagDisabled() throws Exception {
        mockMvc.perform(post("/lgpd/admin/retention/apply")
                .contentType("application/json")
                .content("""
                        {
                          "justification": "Regular execution",
                          "confirmed": true
                        }
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("RETENTION_APPLY_DISABLED"))
                .andExpect(jsonPath("$.message").value("Data retention APPLY is currently disabled. Set LGPD_RETENTION_ALLOW_APPLY=true to enable."));
    }

    private LgpdEmployeeExportResponse minimalExportResponse(UUID employeeId, boolean includePreciseGeolocation) {
        return new LgpdEmployeeExportResponse(
                new LgpdEmployeeExportResponse.ExportManifest(
                        UUID.randomUUID(),
                        Instant.now(),
                        UUID.randomUUID(),
                        employeeId,
                        includePreciseGeolocation,
                        List.of("employee"),
                        List.of("Este arquivo contém dados pessoais.")
                ),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                Instant.now()
        );
    }
}
