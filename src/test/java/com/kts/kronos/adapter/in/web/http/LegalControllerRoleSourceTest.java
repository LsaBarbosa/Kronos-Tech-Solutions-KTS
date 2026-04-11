package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalControllerRoleSourceTest {

    @InjectMocks
    private LegalController controller;

    @Mock
    private AdfUseCase afdUseCase;
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
    private TechnicalCertificatePdfService certificateService;
    @Mock
    private DigitalSignatureService signatureService;

    @Test
    void shouldUseCurrentRoleForMirrorTargetResolution() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        byte[] pdf = "pdf".getBytes();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(pointMirrorPdfUseCase.generateMirror(targetEmployeeId, startDate, endDate)).thenReturn(pdf);

        var response = new MockHttpServletResponse();

        controller.downloadMirror(targetEmployeeId, startDate, endDate, response);

        verify(jwtAuthenticatedUser).getCurrentRole();
        verify(jwtAuthenticatedUser, never()).getRoleFromToken();
        verify(pointMirrorPdfUseCase).generateMirror(targetEmployeeId, startDate, endDate);
    }
}
