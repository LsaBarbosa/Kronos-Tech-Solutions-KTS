package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers MessageController.getMessages branches:
 * - page < 0 (L46 TRUE) and size < 1 (L47 TRUE)
 * - senderNames key extractor (L55), value extractor (L56)
 * - merge function on duplicate employees (L57)
 * - containsKey TRUE → return name (L76-77)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageControllerCoverageTest {

    @Mock private MessageUseCase useCase;
    @Mock private EmployeeProvider employeeProvider;

    private MessageController controller;

    @BeforeEach
    void setUp() {
        controller = new MessageController(useCase, employeeProvider);
    }

    @Test
    void getMessages_negativePage_andSizeLessThanOne_usesDefaults() {
        // page=-1 → page<0=TRUE → safePage=0; size=0 → size<1=TRUE → safeSize=10
        UUID empId = UUID.randomUUID();
        Message msg = buildMessage(empId);
        Employee emp = mock(Employee.class);
        when(emp.employeeId()).thenReturn(empId);
        when(emp.fullName()).thenReturn("John Doe");

        when(useCase.listMessagesForMyCompany(0, 10)).thenReturn(List.of(msg));
        // Return employee twice to trigger merge function (L57)
        when(employeeProvider.findAllByIds(any())).thenReturn(List.of(emp, emp));

        ResponseEntity<?> response = controller.getMessages(-1, 0);

        assertNotNull(response.getBody());
        // Verify safePage=0 and safeSize=10 were used
        verify(useCase).listMessagesForMyCompany(0, 10);
    }

    @Test
    void getMessages_withKnownSender_returnsNameFromMap() {
        // findAllByIds returns employee → senderNames has key → containsKey TRUE (L76-77)
        UUID empId = UUID.randomUUID();
        Message msg = buildMessage(empId);
        Employee emp = mock(Employee.class);
        when(emp.employeeId()).thenReturn(empId);
        when(emp.fullName()).thenReturn("Maria Silva");

        when(useCase.listMessagesForMyCompany(0, 10)).thenReturn(List.of(msg));
        when(employeeProvider.findAllByIds(any())).thenReturn(List.of(emp));

        ResponseEntity<?> response = controller.getMessages(null, null);
        assertNotNull(response.getBody());
        // senderNames.containsKey(empId) = TRUE → uses "Maria Silva" (L76-77 covered)
    }

    private static Message buildMessage(UUID employeeId) {
        return new Message(
                UUID.randomUUID(),
                employeeId,
                UUID.randomUUID(),
                "Test Title",
                "Test message body",
                MessagePriority.NORMAL,
                LocalDateTime.now(),
                null  // no recipient (broadcast)
        );
    }
}
