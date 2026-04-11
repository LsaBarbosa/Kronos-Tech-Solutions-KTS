package com.kts.kronos.application;

import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.PointMirrorPdfService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointMirrorPdfServiceSecurityTest {

    @InjectMocks
    private PointMirrorPdfService service;

    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private TimeRecordProvider recordRepository;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;

    @Test
    @DisplayName("espelho: permite geração para colaborador autorizado")
    void shouldAllowMirrorGenerationForAuthorizedEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = buildEmployee(employeeId, companyId);
        var company = buildCompany(companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of());

        byte[] pdf = service.generateMirror(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("espelho: bloqueia geração cross-tenant")
    void shouldBlockMirrorGenerationForCrossTenantEmployee() {
        UUID employeeId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId))
                .thenThrow(new ForbiddenException("forbidden"));

        assertThrows(ForbiddenException.class,
                () -> service.generateMirror(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)));

        verify(companyProvider, never()).findById(org.mockito.ArgumentMatchers.any());
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

    private Company buildCompany(UUID companyId) {
        return new Company(
                companyId,
                "KTS",
                "00000000000100",
                "empresa@kts.com",
                true,
                null,
                null,
                0,
                0
        );
    }
}
