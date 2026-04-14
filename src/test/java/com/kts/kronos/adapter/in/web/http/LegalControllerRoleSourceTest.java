package com.kts.kronos.adapter.in.web.http;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
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
    @Mock
    private DomainAuthorizationService domainAuthorizationService;

    @Test
    void shouldUseCurrentRoleForMirrorTargetResolution() throws Exception {
        UUID loggedEmployeeId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        byte[] pdf = "pdf".getBytes();

        Employee targetEmployee = buildEmployee(targetEmployeeId, UUID.randomUUID());

        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId)).thenReturn(targetEmployee);
        when(pointMirrorPdfUseCase.generateMirror(targetEmployeeId, startDate, endDate)).thenReturn(pdf);

        var response = new MockHttpServletResponse();

        controller.downloadMirror(targetEmployeeId, startDate, endDate, response);

        verify(domainAuthorizationService).authorizeEmployeeAccess(targetEmployeeId);
        verify(pointMirrorPdfUseCase).generateMirror(targetEmployeeId, startDate, endDate);
    }
    @Test
    void shouldBlockMirrorForOtherTenantTarget() {
        UUID targetEmployeeId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        var response = new MockHttpServletResponse();

        assertThrows(
                ForbiddenException.class,
                () -> controller.downloadMirror(targetEmployeeId, startDate, endDate, response)
        );

        verify(pointMirrorPdfUseCase, never()).generateMirror(any(), any(), any());
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
