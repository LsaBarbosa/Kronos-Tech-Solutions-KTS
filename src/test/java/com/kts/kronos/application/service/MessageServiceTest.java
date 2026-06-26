package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.MessageDeliveryProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.MessageScope;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

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
        lenient().when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
    }

    @Test
    @DisplayName("postMessage: falha quando colaborador autenticado não existe")
    void shouldFailPostingWhenAuthenticatedEmployeeDoesNotExist() {
        UUID senderId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.postMessage(request(List.of(UUID.randomUUID()))));
        verify(messageProvider, never()).save(any());
    }

    @Test
    @DisplayName("postMessage: exige pelo menos um destinatário")
    void shouldRequireRecipientsWhenPostingMessage() {
        UUID senderId = UUID.randomUUID();
        Employee sender = employee(senderId, UUID.randomUUID());
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));

        assertThrows(BadRequestException.class, () -> service.postMessage(request(List.of())));
        verify(messageProvider, never()).save(any());
    }

    @Test
    @DisplayName("postMessage: rejeita destinatários fora da empresa")
    void shouldRejectMessageWhenNoValidRecipientExists() {
        UUID companyId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Employee sender = employee(senderId, companyId);
        Employee otherCompanyRecipient = employee(recipientId, UUID.randomUUID());

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));
        when(employeeProvider.findById(recipientId)).thenReturn(Optional.of(otherCompanyRecipient));

        assertThrows(BadRequestException.class, () -> service.postMessage(request(List.of(recipientId))));
        verify(messageProvider, never()).save(any());
    }

    @Test
    @DisplayName("postMessage: salva uma mensagem por destinatário válido")
    void shouldSaveOneMessageForEachValidRecipient() {
        UUID companyId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID validRecipientId = UUID.randomUUID();
        UUID missingRecipientId = UUID.randomUUID();
        Employee sender = employee(senderId, companyId);
        Employee validRecipient = employee(validRecipientId, companyId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));
        when(employeeProvider.findById(validRecipientId)).thenReturn(Optional.of(validRecipient));
        when(employeeProvider.findById(missingRecipientId)).thenReturn(Optional.empty());
        when(userProvider.findByEmployeeId(validRecipientId)).thenReturn(Optional.of(activeUser(validRecipientId, Role.PARTNER)));

        service.postMessage(request(List.of(validRecipientId, missingRecipientId)));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageProvider).save(captor.capture());
        assertEquals(senderId, captor.getValue().employeeId());
        assertEquals(validRecipientId, captor.getValue().recipientEmployeeId());
    }

    @Test
    @DisplayName("listMessagesForMyCompany: falha quando colaborador autenticado não existe")
    void shouldFailListWhenAuthenticatedEmployeeDoesNotExist() {
        UUID senderId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listMessagesForMyCompany());
    }

    @Test
    @DisplayName("listMessagesForMyCompany: usa defaults seguros para paginação inválida")
    void shouldUseSafePaginationDefaults() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee sender = employee(senderId, companyId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));

        service.listMessagesForMyCompany(-1, 500);

        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(
                companyId,
                senderId,
                PageRequest.of(0, 200)
        );
    }

    @Test
    @DisplayName("deleteMessage: falha quando mensagem não existe")
    void shouldFailDeleteWhenMessageDoesNotExist() {
        UUID senderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(messageProvider.findById(messageId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteMessage(messageId));
    }

    @Test
    @DisplayName("deleteMessage: impede exclusão por outro colaborador")
    void shouldRejectDeleteByNonOwner() {
        UUID senderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = new Message(
                messageId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Aviso",
                "Texto",
                MessagePriority.NORMAL,
                LocalDateTime.now(),
                UUID.randomUUID()
        );
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(messageProvider.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(BadRequestException.class, () -> service.deleteMessage(messageId));
    }

    @Test
    @DisplayName("postMessage: CTO envia global ignorando destinatários manipulados")
    void shouldCreateGlobalMessageForCtoIgnoringPayloadRecipients() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID activeRecipientId = UUID.randomUUID();
        UUID inactiveRecipientId = UUID.randomUUID();
        Employee sender = employee(senderId, companyId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));
        when(userProvider.findByActive(true)).thenReturn(List.of(
                activeUser(activeRecipientId, Role.PARTNER),
                new User(UUID.randomUUID(), "cto-no-employee", "hash", Role.CTO, true, null),
                new User(UUID.randomUUID(), "same-employee", "hash", Role.PARTNER, true, activeRecipientId)
        ));

        var response = service.postMessage(request(List.of(inactiveRecipientId)));

        assertEquals(MessageScope.GLOBAL, response.scope());
        assertEquals(1, response.deliveredCount());
        verify(messageProvider).save(any(Message.class));
    }

    @Test
    @DisplayName("deleteMessage: CTO pode excluir aviso global")
    void shouldAllowCtoToDeleteGlobalMessage() {
        UUID senderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Message message = new Message(
                messageId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Aviso global",
                "Texto",
                MessagePriority.CRITICAL,
                MessageScope.GLOBAL,
                LocalDateTime.now(),
                null,
                null
        );

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(messageProvider.findById(messageId)).thenReturn(Optional.of(message));

        service.deleteMessage(messageId);

        verify(messageProvider).deleteByMessageId(messageId);
    }

    @Test
    @DisplayName("postMessage: PARTNER não pode publicar")
    void shouldRejectPartnerPublishingMessages() {
        UUID senderId = UUID.randomUUID();
        Employee sender = employee(senderId, UUID.randomUUID());

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));

        assertThrows(com.kts.kronos.application.exceptions.ForbiddenException.class,
                () -> service.postMessage(request(List.of(UUID.randomUUID()))));
        verify(messageProvider, never()).save(any());
    }

    private CreateMessageRequest request(List<UUID> recipients) {
        return new CreateMessageRequest("Texto", "Aviso", MessagePriority.NORMAL, recipients);
    }

    private Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Funcionario",
                "12345678901",
                "12345678901",
                "Dev",
                "dev@kts.com",
                1000.0,
                "11999999999",
                true,
                new Address("Rua A", "10", "01001000", "Sao Paulo", "SP"),
                companyId,
                null,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );
    }

    private User activeUser(UUID employeeId, Role role) {
        return new User(UUID.randomUUID(), "user-" + employeeId, "hash", role, true, employeeId);
    }
}
