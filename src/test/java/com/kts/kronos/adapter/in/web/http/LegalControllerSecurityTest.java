package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LegalControllerSecurityTest {

    @Mock
    private AdfUseCase adfUseCase;
    @Mock
    private AejUseCase aejUseCase;
    @Mock
    private PointMirrorPdfUseCase pointMirrorPdfUseCase;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;
    @Mock
    private TechnicalCertificatePdfService certificateService;
    @Mock
    private DigitalSignatureService signatureService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new LegalController(
                adfUseCase,
                aejUseCase,
                pointMirrorPdfUseCase,
                jwtAuthenticatedUser,
                employeeProvider,
                companyProvider,
                domainAuthorizationService,
                certificateService,
                signatureService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("espelho: manager pode gerar PDF para colaborador do mesmo tenant")
    void shouldAllowMirrorGenerationForSameTenantEmployee() throws Exception {
        UUID targetEmployeeId = UUID.randomUUID();
        var targetEmployee = buildEmployee(targetEmployeeId, UUID.randomUUID());
        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId)).thenReturn(targetEmployee);
        when(pointMirrorPdfUseCase.generateMirror(targetEmployeeId, java.time.LocalDate.parse("2026-01-01"), java.time.LocalDate.parse("2026-01-31")))
                .thenReturn("pdf".getBytes());

        mockMvc.perform(get("/legal/espelho-ponto")
                        .param("targetEmployeeId", targetEmployeeId.toString())
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk());

        verify(pointMirrorPdfUseCase).generateMirror(targetEmployeeId, java.time.LocalDate.parse("2026-01-01"), java.time.LocalDate.parse("2026-01-31"));
    }

    @Test
    @DisplayName("espelho: bloqueia targetEmployeeId de outro tenant")
    void shouldBlockMirrorGenerationForCrossTenantEmployee() throws Exception {
        UUID targetEmployeeId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId))
                .thenThrow(new ForbiddenException("forbidden"));

        mockMvc.perform(get("/legal/espelho-ponto")
                        .param("targetEmployeeId", targetEmployeeId.toString())
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("espelho: employeeId opcional usa alvo seguro por padrão")
    void shouldUseSafeDefaultWhenTargetEmployeeIdIsNotProvided() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();
        var loggedEmployee = buildEmployee(loggedEmployeeId, UUID.randomUUID());
        when(domainAuthorizationService.authorizeEmployeeAccess(null)).thenReturn(loggedEmployee);
        when(pointMirrorPdfUseCase.generateMirror(loggedEmployeeId, java.time.LocalDate.parse("2026-01-01"), java.time.LocalDate.parse("2026-01-31")))
                .thenReturn("pdf".getBytes());

        mockMvc.perform(get("/legal/espelho-ponto")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk());

        verify(pointMirrorPdfUseCase).generateMirror(loggedEmployeeId, java.time.LocalDate.parse("2026-01-01"), java.time.LocalDate.parse("2026-01-31"));
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Nome",
                "12345678901",
                "12345678901",
                "Dev",
                "dev@kts.com",
                1000.0,
                "11999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );
    }
}
