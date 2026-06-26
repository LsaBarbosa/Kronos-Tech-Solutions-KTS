package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.MessageDeliveryProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private MessageDeliveryProvider messageDeliveryProvider;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private UserProvider userProvider;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @BeforeEach
    void setUp() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
    }

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

    @Test
    @DisplayName("listMessagesForMyCompany: com page nulo e size alto usa página padrão e limita size máximo")
    void shouldClampLargePageSizeAndDefaultNullPage() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId, "Ana");

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        service.listMessagesForMyCompany(null, 1000);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(
                org.mockito.ArgumentMatchers.eq(companyId),
                org.mockito.ArgumentMatchers.eq(employeeId),
                pageableCaptor.capture()
        );
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(200, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("listMessagesForMyCompany: com size nulo aplica tamanho padrão")
    void shouldApplyDefaultSizeWhenSizeIsNull() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId, "Ana");

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        service.listMessagesForMyCompany(2, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(
                org.mockito.ArgumentMatchers.eq(companyId),
                org.mockito.ArgumentMatchers.eq(employeeId),
                pageableCaptor.capture()
        );
        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("postMessage: falha quando lista de destinatários está vazia")
    void shouldFailWhenPostingMessageWithoutRecipients() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee sender = employee(senderId, companyId, "Manager");
        CreateMessageRequest request = new CreateMessageRequest(
                "Mensagem",
                "Aviso",
                MessagePriority.NORMAL,
                List.of()
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));

        assertThrows(BadRequestException.class, () -> service.postMessage(request));
        verify(messageProvider, never()).save(any());
    }

    @Test
    @DisplayName("postMessage: persiste mensagens apenas para destinatários válidos da mesma empresa")
    void shouldPersistMessagesOnlyForValidRecipientsInSameCompany() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID validRecipient = UUID.randomUUID();
        UUID invalidRecipient = UUID.randomUUID();

        Employee sender = employee(senderId, companyId, "Manager");
        Employee validEmployee = employee(validRecipient, companyId, "Ana");
        Employee invalidEmployee = employee(invalidRecipient, UUID.randomUUID(), "Bruno");
        CreateMessageRequest request = new CreateMessageRequest(
                "Mensagem",
                "Aviso",
                MessagePriority.NORMAL,
                List.of(validRecipient, invalidRecipient)
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));
        when(employeeProvider.findById(validRecipient)).thenReturn(Optional.of(validEmployee));
        when(employeeProvider.findById(invalidRecipient)).thenReturn(Optional.of(invalidEmployee));
        when(userProvider.findByEmployeeId(validRecipient)).thenReturn(Optional.of(activeUser(validRecipient)));

        service.postMessage(request);

        verify(messageProvider).save(any(Message.class));
    }

    @Test
    @DisplayName("deleteMessage: falha quando usuário autenticado não é o remetente")
    void shouldRejectDeleteWhenAuthenticatedUserIsNotSender() {
        UUID senderId = UUID.randomUUID();
        UUID anotherEmployee = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Message message = new Message(
                messageId,
                senderId,
                companyId,
                "Aviso",
                "Texto",
                MessagePriority.NORMAL,
                java.time.LocalDateTime.now(),
                senderId
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(anotherEmployee);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(messageProvider.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(BadRequestException.class, () -> service.deleteMessage(messageId));
        verify(messageProvider, never()).deleteByMessageIdAndEmployeeId(any(), any());
    }

    @Test
    @DisplayName("deleteMessage: remove mensagem quando usuário autenticado é o remetente")
    void shouldDeleteMessageWhenAuthenticatedUserIsSender() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Message message = new Message(
                messageId,
                senderId,
                companyId,
                "Aviso",
                "Texto",
                MessagePriority.NORMAL,
                java.time.LocalDateTime.now(),
                senderId
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(messageProvider.findById(messageId)).thenReturn(Optional.of(message));

        service.deleteMessage(messageId);

        verify(messageProvider).deleteByMessageIdAndEmployeeId(messageId, senderId);
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

    private User activeUser(UUID employeeId) {
        return new User(UUID.randomUUID(), "user-" + employeeId, "hash", Role.PARTNER, true, employeeId);
    }
}
