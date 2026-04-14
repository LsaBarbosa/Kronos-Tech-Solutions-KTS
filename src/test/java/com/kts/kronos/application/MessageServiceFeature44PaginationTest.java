package com.kts.kronos.application;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.application.service.MessageService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceFeature44PaginationTest {

    @InjectMocks
    private MessageService service;

    @Mock
    private MessageProvider messageProvider;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @Test
    @DisplayName("listMessagesForMyCompany: sem page/size mantém contrato legado")
    void shouldKeepLegacyFlowWhenPaginationParamsAreMissing() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId, "Ana");

        Message message = new Message(
                employeeId,
                companyId,
                "Aviso",
                "Texto",
                MessagePriority.NORMAL,
                employeeId
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId))
                .thenReturn(List.of(message));

        var result = service.listMessagesForMyCompany(null, null);

        assertEquals(1, result.size());
        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId);
        verify(messageProvider, never()).findVisibleMessagesByCompanyIdAndEmployeeId(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("listMessagesForMyCompany: com page/size usa consulta paginada")
    void shouldUsePagedFlowWhenPaginationParamsAreProvided() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId, "Ana");

        Message message = new Message(
                employeeId,
                companyId,
                "Aviso",
                "Texto",
                MessagePriority.NORMAL,
                employeeId
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(message));

        var result = service.listMessagesForMyCompany(1, 25);

        assertEquals(1, result.size());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(
                org.mockito.ArgumentMatchers.eq(companyId),
                org.mockito.ArgumentMatchers.eq(employeeId),
                pageableCaptor.capture()
        );
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(25, pageableCaptor.getValue().getPageSize());
        verify(messageProvider, never()).findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId);
    }

    @Test
    @DisplayName("listMessagesForMyCompany: normaliza page/size inválidos para defaults seguros")
    void shouldNormalizeInvalidPaginationParams() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId, "Ana");

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        service.listMessagesForMyCompany(-10, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(
                org.mockito.ArgumentMatchers.eq(companyId),
                org.mockito.ArgumentMatchers.eq(employeeId),
                pageableCaptor.capture()
        );
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    private Employee employee(UUID employeeId, UUID companyId, String name) {
        return new Employee(
                employeeId,
                name,
                "12345678901",
                "12345678901",
                "Developer",
                name.toLowerCase() + "@kts.com",
                5000.0,
                "21999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
