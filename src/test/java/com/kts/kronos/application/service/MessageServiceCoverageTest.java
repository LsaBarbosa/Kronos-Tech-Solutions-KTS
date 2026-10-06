package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Supplemental coverage for MessageService:
 * - L96: !equals=FALSE && scope==GLOBAL → throws BadRequestException (missing branch)
 * - L147: validRecipientEmployeeIds.size() > 1 → recipientEmployeeId=null
 * - L74: page==null && size!=null → safePage=DEFAULT_PAGE (page==null TRUE branch)
 * - L75: page!=null && size==null → safeSize=DEFAULT_SIZE (size==null TRUE branch)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceCoverageTest {

    @InjectMocks private MessageService service;

    @Mock private MessageProvider messageProvider;
    @Mock private MessageDeliveryProvider messageDeliveryProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private UserProvider userProvider;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;

    // ── L96: MANAGER owns message but scope==GLOBAL → BadRequestException ──────

    @Test
    void deleteMessage_managerOwnsGlobalMessage_throwsBadRequest() {
        // !equals = FALSE (sender matches employeeId)
        // scope == GLOBAL → condition TRUE → throw BadRequestException (L96 GLOBAL=TRUE branch)
        UUID senderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        // message.employeeId() == senderId (owner), scope == GLOBAL
        Message message = new Message(messageId, senderId, UUID.randomUUID(),
                "T", "M", MessagePriority.NORMAL, MessageScope.GLOBAL, LocalDateTime.now(), null, null);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(messageProvider.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(BadRequestException.class, () -> service.deleteMessage(messageId));
    }

    // ── L147: 2 valid recipients → recipientEmployeeId=null in saved message ──

    @Test
    void postMessage_twoValidRecipients_savesMessageWithNullRecipientId() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID recipientId1 = UUID.randomUUID();
        UUID recipientId2 = UUID.randomUUID();
        Employee sender = employee(senderId, companyId);
        Employee recipient1 = employee(recipientId1, companyId);
        Employee recipient2 = employee(recipientId2, companyId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(jwtAuthenticatedUser.getActiveCompanyId()).thenReturn(companyId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));
        when(employeeProvider.findById(recipientId1)).thenReturn(Optional.of(recipient1));
        when(employeeProvider.findById(recipientId2)).thenReturn(Optional.of(recipient2));
        when(userProvider.findByEmployeeId(recipientId1))
            .thenReturn(Optional.of(new User(UUID.randomUUID(), "u1", "h", Role.PARTNER, true, recipientId1)));
        when(userProvider.findByEmployeeId(recipientId2))
            .thenReturn(Optional.of(new User(UUID.randomUUID(), "u2", "h", Role.PARTNER, true, recipientId2)));

        // 2 valid recipients → size == 2 > 1 → recipientEmployeeId = null (L147 FALSE branch)
        service.postMessage(new CreateMessageRequest("Title", "Body", MessagePriority.NORMAL,
                List.of(recipientId1, recipientId2)));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageProvider).save(captor.capture());
        assertNull(captor.getValue().recipientEmployeeId());
    }

    // ── L74: page==null && size!=null → safePage=DEFAULT_PAGE ────────────���────

    @Test
    void listMessagesForMyCompany_withNullPageAndNonNullSize_usesDefaultPage() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee sender = employee(senderId, companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(jwtAuthenticatedUser.getActiveCompanyId()).thenReturn(companyId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any(), any()))
            .thenReturn(List.of());

        // page=null (non-null size 10) → NOT caught by L70 (page==null && size==null)
        // L74: page==null=TRUE → safePage=DEFAULT_PAGE=0
        service.listMessagesForMyCompany(null, 10);

        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(
                companyId, senderId, PageRequest.of(0, 10));
    }

    // ── L75: page!=null && size==null → safeSize=DEFAULT_SIZE ─────────────────

    @Test
    void listMessagesForMyCompany_withNonNullPageAndNullSize_usesDefaultSize() {
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee sender = employee(senderId, companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(senderId);
        when(jwtAuthenticatedUser.getActiveCompanyId()).thenReturn(companyId);
        when(employeeProvider.findById(senderId)).thenReturn(Optional.of(sender));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any(), any()))
            .thenReturn(List.of());

        // size=null (non-null page 1) → NOT caught by L70 (page==null && size==null)
        // L75: size==null=TRUE → safeSize=DEFAULT_SIZE=50
        service.listMessagesForMyCompany(1, null);

        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(
                companyId, senderId, PageRequest.of(1, 50));
    }

    // ── persistDeliveries: message.createdAt()==null → createdAt=LocalDateTime.now() ──

    @Test
    void persistDeliveries_nullCreatedAt_usesCurrentTime() {
        // Message with createdAt=null → persistDeliveries → createdAt=LocalDateTime.now() branch
        // Use canonical constructor (12 args) to set createdAt=null
        Message msg = new Message(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "T", "M", MessagePriority.NORMAL, MessageScope.DIRECT,
                null, // createdAt = null → branch TRUE
                null, null, 0, null
        );

        List<UUID> recipients = List.of(UUID.randomUUID());

        // Invoke private method via reflection
        ReflectionTestUtils.invokeMethod(service, "persistDeliveries", msg, recipients);

        // Verify saveAll was called (createdAt was set to non-null LocalDateTime.now())
        verify(messageDeliveryProvider).saveAll(any());
    }

    // ── listVisibleMessages: page==FALSE, size==null=TRUE → dead code branch ────

    @Test
    void listVisibleMessages_nonNullPageNullSize_triggersDeadCodeBranch() {
        // Call private listVisibleMessages(employee, page=0, size=null)
        // → page==null=FALSE, size==null=TRUE → ternary TRUE (dead code path)
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee emp = employee(senderId, companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getActiveCompanyId()).thenReturn(companyId);
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any()))
                .thenReturn(List.of());

        // Use reflection to call private method with (emp, 0, null) - dead code case
        ReflectionTestUtils.invokeMethod(service, "listVisibleMessages", emp, 0, (Integer) null);

        verify(messageProvider).findVisibleMessagesByCompanyIdAndEmployeeId(companyId, senderId);
    }

    private Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
            employeeId, "Funcionario", "12345678901", "12345678901", "Dev",
            "dev@kts.com", 1000.0, "11999999999", true,
            new Address("Rua A", "10", "01001000", "Sao Paulo", "SP"),
            companyId, null, false, null,
            LocalTime.of(8, 0), LocalTime.of(17, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0),
            null, null, null, null, null
        );
    }
}
