package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.inventory.CreateInventoryRequest;
import com.kts.kronos.adapter.in.web.dto.inventory.DataProcessingInventoryResponse;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.application.service.DataProcessingInventoryService;
import com.kts.kronos.domain.model.DataProcessingInventory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataProcessingInventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, DataProcessingInventoryControllerTest.MethodSecurityTestConfig.class})
@WithMockUser(roles = "CTO")
class DataProcessingInventoryControllerTest {
    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private DataProcessingInventoryService service;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private DataProcessingInventory createInventoryDomain() {
        return new DataProcessingInventory(
                UUID.randomUUID(),
                "BIOMETRIA_FACIAL",
                "Autenticação Biométrica",
                "Coleta e armazenamento de dados biométricos faciais",
                "BIOMETRIA",
                "Template facial, pontos característicos",
                "FUNCIONÁRIO",
                "Autenticação de acesso",
                "CONSENTIMENTO",
                true,
                "REGISTRO_PONTO",
                "Banco de dados PostgreSQL",
                "BIOMETRIC_RETENTION_REVIEW",
                "Provedor de nuvem AWS",
                false,
                "Criptografia em trânsito e repouso, MFA",
                true,
                "ALTO",
                true,
                "1.0",
                "AWS Rekognition",
                Instant.now(),
                Instant.now()
        );
    }

    private CreateInventoryRequest createInventoryRequest() {
        return new CreateInventoryRequest(
                "BIOMETRIA_FACIAL",
                "Autenticação Biométrica",
                "Coleta e armazenamento de dados biométricos faciais",
                "BIOMETRIA",
                "Template facial, pontos característicos",
                "FUNCIONÁRIO",
                "Autenticação de acesso",
                "CONSENTIMENTO",
                true,
                "REGISTRO_PONTO",
                "Banco de dados PostgreSQL",
                "BIOMETRIC_RETENTION_REVIEW",
                "Provedor de nuvem AWS",
                false,
                "Criptografia em trânsito e repouso, MFA",
                true,
                "ALTO",
                true,
                "1.0",
                "AWS Rekognition"
        );
    }

    @Test
    void shouldListAllInventories() throws Exception {
        var inventory = createInventoryDomain();
        var page = new PageImpl<>(List.of(inventory), PageRequest.of(0, 10), 1);

        when(service.listAllInventories(any())).thenReturn(page);

        mockMvc.perform(get("/api/lgpd/inventory?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].processCode").value("BIOMETRIA_FACIAL"))
                .andExpect(jsonPath("$.content[0].description").value("Coleta e armazenamento de dados biométricos faciais"))
                .andExpect(jsonPath("$.content[0].riskLevel").value("ALTO"))
                .andExpect(jsonPath("$.content[0].ripdRequired").value(true));
    }

    @Test
    void shouldListActiveInventories() throws Exception {
        var inventory = createInventoryDomain();
        var page = new PageImpl<>(List.of(inventory), PageRequest.of(0, 10), 1);

        when(service.listActiveInventories(any())).thenReturn(page);

        mockMvc.perform(get("/api/lgpd/inventory/active?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void shouldGetInventoryByProcessCode() throws Exception {
        var inventory = createInventoryDomain();

        when(service.getInventoryByProcessCode("BIOMETRIA_FACIAL")).thenReturn(inventory);

        mockMvc.perform(get("/api/lgpd/inventory/BIOMETRIA_FACIAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processCode").value("BIOMETRIA_FACIAL"))
                .andExpect(jsonPath("$.version").value("1.0"));
    }

    @Test
    void shouldCreateInventory() throws Exception {
        var request = createInventoryRequest();
        var created = createInventoryDomain();

        when(service.createInventory(any())).thenReturn(created);

        mockMvc.perform(post("/api/lgpd/inventory")
                .contentType("application/json")
                .content("""
                        {
                          "processCode": "BIOMETRIA_FACIAL",
                          "processName": "Autenticação Biométrica",
                          "description": "Coleta e armazenamento de dados biométricos faciais",
                          "dataCategory": "BIOMETRIA",
                          "dataFields": "Template facial, pontos característicos",
                          "dataSubjectCategory": "FUNCIONÁRIO",
                          "purpose": "Autenticação de acesso",
                          "legalBasis": "CONSENTIMENTO",
                          "sensitiveData": true,
                          "sourceSystem": "REGISTRO_PONTO",
                          "storageLocation": "Banco de dados PostgreSQL",
                          "retentionPolicyCode": "BIOMETRIC_RETENTION_REVIEW",
                          "externalSharing": "Provedor de nuvem AWS",
                          "internationalTransfer": false,
                          "securityMeasures": "Criptografia em trânsito e repouso, MFA",
                          "active": true,
                          "riskLevel": "ALTO",
                          "ripdRequired": true,
                          "version": "1.0",
                          "operators": "AWS Rekognition"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processCode").value("BIOMETRIA_FACIAL"))
                .andExpect(jsonPath("$.riskLevel").value("ALTO"));
    }

    @Test
    void shouldUpdateInventory() throws Exception {
        var inventoryId = UUID.randomUUID();
        var request = createInventoryRequest();
        var updated = createInventoryDomain();

        when(service.updateInventory(eq(inventoryId), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/lgpd/inventory/" + inventoryId)
                .contentType("application/json")
                .content("""
                        {
                          "processCode": "BIOMETRIA_FACIAL",
                          "processName": "Autenticação Biométrica",
                          "description": "Coleta e armazenamento de dados biométricos faciais",
                          "dataCategory": "BIOMETRIA",
                          "dataFields": "Template facial, pontos característicos",
                          "dataSubjectCategory": "FUNCIONÁRIO",
                          "purpose": "Autenticação de acesso",
                          "legalBasis": "CONSENTIMENTO",
                          "sensitiveData": true,
                          "sourceSystem": "REGISTRO_PONTO",
                          "storageLocation": "Banco de dados PostgreSQL",
                          "retentionPolicyCode": "BIOMETRIC_RETENTION_REVIEW",
                          "externalSharing": "Provedor de nuvem AWS",
                          "internationalTransfer": false,
                          "securityMeasures": "Criptografia em trânsito e repouso, MFA",
                          "active": true,
                          "riskLevel": "ALTO",
                          "ripdRequired": true,
                          "version": "1.0",
                          "operators": "AWS Rekognition"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ripdRequired").value(true));
    }
}
