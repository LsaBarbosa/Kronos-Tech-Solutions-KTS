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

        var summary = new com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunSummary(
                100, 50, 50, 0
        );
        var domains = Arrays.asList(
                new com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain(
                        "TIME_RECORD", 50, 50, 0,
                        "REMOVE_PRECISE_GEOLOCATION",
                        "Registros trabalhistas serão preservados. Apenas geolocalização será removida."
                )
        );
        var warnings = Arrays.asList(
                "Esta é uma visualização. Nenhum dado foi modificado."
        );

        AnonymizationDryRunResponse response = new AnonymizationDryRunResponse(
                employeeId, summary, domains, warnings
        );

        when(lgpdUseCase.dryRunAnonymizeEmployee(employeeId)).thenReturn(response);

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize/dry-run", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.summary.totalScanned").value(100))
                .andExpect(jsonPath("$.summary.totalAffected").value(50))
                .andExpect(jsonPath("$.summary.totalSkipped").value(50))
                .andExpect(jsonPath("$.domains.length()").value(1))
                .andExpect(jsonPath("$.domains[0].resourceType").value("TIME_RECORD"))
                .andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.warnings[0]").value("Esta é uma visualização. Nenhum dado foi modificado."));
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void shouldForbidPartnerFromDryRunAnonymization() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize/dry-run", employeeId))
                .andExpect(status().isForbidden());
    }
}
