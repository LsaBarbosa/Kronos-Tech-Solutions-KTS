package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageProvider messageProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock JwtAuthenticatedUser jwtAuthenticatedUser;

    @InjectMocks MessageService service;

    @Test
    void listMessagesForMyCompanyReturnsProviderData() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findCompanyIdByEmployeeId(employeeId)).thenReturn(Optional.of(companyId));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)).thenReturn(List.of());

        assertEquals(0, service.listMessagesForMyCompany().size());
    }

    @Test
    void deleteMessageThrowsWhenSenderIsNotOwner() {
        UUID sender = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(sender);
        when(messageProvider.findById(messageId)).thenReturn(Optional.of(new Message(other, UUID.randomUUID(), "t", "m", MessagePriority.ALERT, sender)));

        assertThrows(RuntimeException.class, () -> service.deleteMessage(messageId));
    }

    @Test
    void postMessageThrowsWhenRecipientsMissing() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(UUID.randomUUID());

        assertThrows(RuntimeException.class,
                () -> service.postMessage(new CreateMessageRequest("msg", "title", MessagePriority.ALERT, List.of())));
    }
}
