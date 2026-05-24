package com.kts.kronos.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.LgpdController;
import com.kts.kronos.application.legal.DataProcessingCatalog;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.LgpdRetentionDryRunService;
import com.kts.kronos.domain.model.DataProcessingPurpose;
import com.kts.kronos.domain.model.enuns.DataCategory;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdProcessingCatalogIntegrationTest.MethodSecurityTestConfig.class})
@DisplayName("LGPD Processing Catalog Integration Tests")
class LgpdProcessingCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LgpdUseCase lgpdUseCase;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private DataProcessingCatalog dataProcessingCatalog;

    @MockitoBean
    private LgpdRetentionDryRunService lgpdRetentionDryRunService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String PROCESSING_CATALOG_ENDPOINT = "/lgpd/processing-catalog";

    @Test
    @DisplayName("CTO should access processing catalog and receive 200")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldAccessProcessingCatalog() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("EMPLOYEE_IDENTIFICATION"))
                .andExpect(jsonPath("$[0].dataCategory").value("IDENTIFICATION"))
                .andExpect(jsonPath("$[0].legalBasis").value("CONTRACT_EXECUTION"))
                .andExpect(jsonPath("$[0].purpose").isNotEmpty())
                .andExpect(jsonPath("$[0].retentionPolicyCode").isNotEmpty())
                .andExpect(jsonPath("$[0].sensitive").value(false))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("MANAGER should access processing catalog and receive 200")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldAccessProcessingCatalog() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("EMPLOYEE_IDENTIFICATION"))
                .andExpect(jsonPath("$[0].dataCategory").value("IDENTIFICATION"))
                .andExpect(jsonPath("$[0].legalBasis").value("CONTRACT_EXECUTION"))
                .andExpect(jsonPath("$[0].sensitive").value(false))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("PARTNER should receive 403 when accessing processing catalog")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldBeForbiddenFromAccessingProcessingCatalog() throws Exception {
        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMPLOYEE should receive 403 when accessing processing catalog")
    @WithMockUser(username = "employee-user", roles = "EMPLOYEE")
    void employeeShouldBeForbiddenFromAccessingProcessingCatalog() throws Exception {
        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user should receive 401 or be redirected")
    void unauthenticatedUserShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Processing catalog should contain required fields in all items")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void processingCatalogShouldContainAllRequiredFields() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").isNotEmpty())
                .andExpect(jsonPath("$[0].dataCategory").isNotEmpty())
                .andExpect(jsonPath("$[0].legalBasis").isNotEmpty())
                .andExpect(jsonPath("$[0].purpose").isNotEmpty())
                .andExpect(jsonPath("$[0].retentionPolicyCode").isNotEmpty());
    }

    @Test
    @DisplayName("Processing catalog should differentiate sensitive and non-sensitive items")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void processingCatalogShouldDifferentiateSensitiveItems() throws Exception {
        List<DataProcessingPurpose> catalogWithSensitive = List.of(
                new DataProcessingPurpose(
                        "BIOMETRIC_DATA",
                        DataCategory.BIOMETRIC,
                        LegalBasis.CONSENT,
                        "Biometric authentication",
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        true,
                        true
                ),
                new DataProcessingPurpose(
                        "EMPLOYEE_ID",
                        DataCategory.IDENTIFICATION,
                        LegalBasis.CONTRACT_EXECUTION,
                        "Employee identification",
                        "RETENTION_EMPLOYEE_CONTRACT",
                        false,
                        true
                )
        );
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalogWithSensitive);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sensitive").value(true))
                .andExpect(jsonPath("$[1].sensitive").value(false));
    }

    @Test
    @DisplayName("Processing catalog should only include active items")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void processingCatalogShouldOnlyIncludeActiveItems() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    private List<DataProcessingPurpose> createSampleCatalog() {
        return List.of(
                new DataProcessingPurpose(
                        "EMPLOYEE_IDENTIFICATION",
                        DataCategory.IDENTIFICATION,
                        LegalBasis.CONTRACT_EXECUTION,
                        "Identificação de colaboradores",
                        "RETENTION_EMPLOYEE_CONTRACT",
                        false,
                        true
                ),
                new DataProcessingPurpose(
                        "BIOMETRIC_AUTHENTICATION",
                        DataCategory.BIOMETRIC,
                        LegalBasis.CONSENT,
                        "Autenticação biométrica",
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        true,
                        true
                )
        );
    }
}
