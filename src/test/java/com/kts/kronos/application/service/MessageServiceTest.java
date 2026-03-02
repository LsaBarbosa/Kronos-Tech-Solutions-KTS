package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.CHOOSE_EMPLOYEE;
import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;
import static com.kts.kronos.constants.Messages.INVALID_EMPLOYEE;
import static com.kts.kronos.constants.Messages.MESSAGE_NOT_FOUND;
import static com.kts.kronos.constants.Messages.ONLY_MANAGER_CAN_DELETE_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageProvider messageProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock JwtAuthenticatedUser jwtAuthenticatedUser;

    @InjectMocks MessageService service;

    @Test
    void postMessageShouldSaveMessagesForUniqueValidRecipientsInSameCompany() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID recipient1 = UUID.randomUUID();
        UUID recipient2 = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(employee(senderId, companyId)));
        when(employeeProvider.findByIdIn(List.of(recipient1, recipient2))).thenReturn(List.of(
                employee(recipient1, companyId),
                employee(recipient2, companyId),
                employee(UUID.randomUUID(), UUID.randomUUID())
        ));

        var request = new CreateMessageRequest(
                "texto",
                "titulo",
                MessagePriority.ALERT,
                java.util.Arrays.asList(recipient1, senderId, recipient1, null, recipient2)
        );

        service.postMessage(request);

        ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(messageProvider).saveAll(messagesCaptor.capture());

        var savedMessages = messagesCaptor.getValue();
        assertEquals(2, savedMessages.size());
        assertIterableEquals(List.of(recipient1, recipient2),
                savedMessages.stream().map(Message::recipientEmployeeId).toList());
        assertEquals(senderId, savedMessages.get(0).employeeId());
        assertEquals(companyId, savedMessages.get(0).companyId());
        assertEquals("titulo", savedMessages.get(0).title());
        assertEquals("texto", savedMessages.get(0).messageText());
        assertEquals(MessagePriority.ALERT, savedMessages.get(0).priority());
    }

    @Test
    void postMessageShouldThrowWhenSenderNotFound() {
        UUID senderId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class,
                () -> service.postMessage(new CreateMessageRequest("m", "t", MessagePriority.ALERT, List.of(UUID.randomUUID()))));

        assertEquals(EMPLOYEE_NOT_FOUND, ex.getMessage());
        verify(messageProvider, never()).saveAll(anyList());
    }

    @Test
    void postMessageShouldThrowWhenRecipientsAreNull() {
        UUID senderId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(employee(senderId, UUID.randomUUID())));

        var ex = assertThrows(BadRequestException.class,
                () -> service.postMessage(new CreateMessageRequest("m", "t", MessagePriority.ALERT, null)));

        assertEquals(CHOOSE_EMPLOYEE, ex.getMessage());
    }

    @Test
    void postMessageShouldThrowWhenRecipientsAreEmpty() {
        UUID senderId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(employee(senderId, UUID.randomUUID())));

        var ex = assertThrows(BadRequestException.class,
                () -> service.postMessage(new CreateMessageRequest("m", "t", MessagePriority.ALERT, List.of())));

        assertEquals(CHOOSE_EMPLOYEE, ex.getMessage());
    }

    @Test
    void postMessageShouldThrowWhenOnlyNullAndSenderRecipientsRemain() {
        UUID senderId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(employee(senderId, UUID.randomUUID())));

        var ex = assertThrows(BadRequestException.class,
                () -> service.postMessage(new CreateMessageRequest("m", "t", MessagePriority.ALERT, java.util.Arrays.asList(null, senderId, senderId))));

        assertEquals(INVALID_EMPLOYEE, ex.getMessage());
    }

    @Test
    void postMessageShouldThrowWhenNoValidRecipientInSameCompany() {
        UUID senderId = UUID.randomUUID();
        UUID senderCompanyId = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(employee(senderId, senderCompanyId)));
        when(employeeProvider.findByIdIn(List.of(recipient))).thenReturn(List.of(employee(recipient, UUID.randomUUID())));

        var ex = assertThrows(BadRequestException.class,
                () -> service.postMessage(new CreateMessageRequest("m", "t", MessagePriority.ALERT, List.of(recipient))));

        assertEquals(INVALID_EMPLOYEE, ex.getMessage());
        verify(messageProvider, never()).saveAll(anyList());
    }

    @Test
    void listMessagesForMyCompanyShouldReturnProviderMessages() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        List<Message> messages = List.of(new Message(employeeId, companyId, "t", "m", MessagePriority.NORMAL, UUID.randomUUID()));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findCompanyIdByEmployeeId(employeeId)).thenReturn(Optional.of(companyId));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)).thenReturn(messages);

        assertEquals(messages, service.listMessagesForMyCompany());
    }

    @Test
    void listMessagesForMyCompanyShouldThrowWhenEmployeeNotFound() {
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findCompanyIdByEmployeeId(employeeId)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> service.listMessagesForMyCompany());

        assertEquals(EMPLOYEE_NOT_FOUND, ex.getMessage());
    }

    @Test
    void deleteMessageShouldDeleteWhenSenderIsOwner() {
        UUID sender = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(sender);
        when(messageProvider.findById(messageId)).thenReturn(Optional.of(
                new Message(messageId, sender, companyId, "t", "m", MessagePriority.ALERT, java.time.LocalDateTime.now(), UUID.randomUUID())
        ));

        service.deleteMessage(messageId);

        verify(messageProvider).deleteByMessageIdAndEmployeeId(messageId, sender);
    }

    @Test
    void deleteMessageShouldThrowWhenMessageNotFound() {
        UUID sender = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(sender);
        when(messageProvider.findById(messageId)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteMessage(messageId));

        assertEquals(MESSAGE_NOT_FOUND, ex.getMessage());
    }

    @Test
    void deleteMessageShouldThrowWhenSenderIsNotOwner() {
        UUID sender = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(sender);
        when(messageProvider.findById(messageId)).thenReturn(Optional.of(
                new Message(messageId, owner, UUID.randomUUID(), "t", "m", MessagePriority.ALERT, java.time.LocalDateTime.now(), UUID.randomUUID())
        ));

        var ex = assertThrows(BadRequestException.class, () -> service.deleteMessage(messageId));

        assertEquals(ONLY_MANAGER_CAN_DELETE_MESSAGE, ex.getMessage());
    }

    private Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "nome",
                "cpf",
                "pis",
                "cargo",
                "email@kts.com",
                10d,
                "9999",
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
