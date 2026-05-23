package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunResponse;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.LgpdController;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
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

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdDryRunControllerTest.MethodSecurityTestConfig.class})
@WithMockUser(roles = "CTO")
class LgpdDryRunControllerTest {
    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private LgpdUseCase lgpdUseCase;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    void shouldReturnDryRunResultForEmployeeAnonymization() throws Exception {
        UUID employeeId = UUID.randomUUID();

        AnonymizationDryRunResponse response = new AnonymizationDryRunResponse(
                employeeId,
                10,
                5,
                15,
                20,
                100,
                1,
                0,
                Arrays.asList(
                        "Serão deletados 10 documentos.",
                        "20 mensagens serão anonimizadas.",
                        "Artefatos biométricos serão deletados permanentemente.",
                        "Esta é uma visualização. Nenhum dado foi modificado."
                )
        );

        when(lgpdUseCase.dryRunAnonymizeEmployee(employeeId)).thenReturn(response);

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize/dry-run", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.totalDocumentsToDelete").value(10))
                .andExpect(jsonPath("$.totalMessagesToAnonymize").value(20))
                .andExpect(jsonPath("$.totalBiometricArtifactsToDelete").value(1))
                .andExpect(jsonPath("$.warnings.length()").value(4))
                .andExpect(jsonPath("$.warnings[3]").value("Esta é uma visualização. Nenhum dado foi modificado."));
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void shouldForbidPartnerFromDryRunAnonymization() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize/dry-run", employeeId))
                .andExpect(status().isForbidden());
    }
}
