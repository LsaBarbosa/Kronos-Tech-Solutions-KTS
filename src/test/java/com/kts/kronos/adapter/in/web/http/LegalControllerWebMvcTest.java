package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LegalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class LegalControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean private AdfUseCase afdUseCase;
    @MockitoBean private AejUseCase aejUseCase;
    @MockitoBean private PointMirrorPdfUseCase pointMirrorPdfUseCase;
    @MockitoBean private JwtAuthenticatedUser jwtAuthenticatedUser;
    @MockitoBean private EmployeeProvider employeeProvider;
    @MockitoBean private CompanyProvider companyProvider;
    @MockitoBean private DomainAuthorizationService domainAuthorizationService;
    @MockitoBean private TechnicalCertificatePdfService certificateService;
    @MockitoBean private DigitalSignatureService signatureService;

    @Test
    void shouldDownloadTechnicalCertificate() throws Exception {
        // implementar
    }

    @Test
    void shouldDownloadAfd() throws Exception {
        // implementar
    }

    @Test
    void shouldDownloadAej() throws Exception {
        // implementar
    }

    @Test
    void shouldDownloadMirror() throws Exception {
        // implementar
    }
}