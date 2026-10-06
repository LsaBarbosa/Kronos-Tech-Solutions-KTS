package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.LgpdRequestNotificationRepository;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.NotificationProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationCoverageTest {

    @Mock private LgpdRequestNotificationRepository notificationRepository;
    @Mock private UserProvider userProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private NotificationProvider notificationProvider;

    private LgpdRequestNotificationService service;

    @BeforeEach
    void setUp() {
        service = new LgpdRequestNotificationService(
                notificationRepository, userProvider, employeeProvider,
                notificationProvider, new PrivacyLogReferenceService("test-secret")
        );
    }

    // ── Covers outer catch block in each notify* method ────────────────────────
    // The outer catch is reached when sendEmailNotification throws a plain RuntimeException
    // (which is NOT NotificationProvider.NotificationException, so it propagates through
    // sendNotification's inner catch and is caught by the outer catch in notify*).

    @Test
    void notifyRequestCreated_runtimeExceptionInProvider_coversCatchBlock() {
        var request = buildRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(buildEmployee()));
        doThrow(new RuntimeException("unexpected failure"))
                .when(notificationProvider).sendEmailNotification(any(), any(), any(), any());

        // outer catch should swallow the exception
        assertDoesNotThrow(() -> service.notifyRequestCreated(request));
    }

    @Test
    void notifyStatusChanged_runtimeExceptionInProvider_coversCatchBlock() {
        var request = buildRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(buildEmployee()));
        doThrow(new RuntimeException("unexpected failure"))
                .when(notificationProvider).sendEmailNotification(any(), any(), any(), any());

        assertDoesNotThrow(() -> service.notifyStatusChanged(request, "OPEN", UUID.randomUUID()));
    }

    @Test
    void notifyResponsibilityAssigned_runtimeExceptionInProvider_coversCatchBlock() {
        UUID assignedToUserId = UUID.randomUUID();
        var request = buildRequest();
        var user = buildUser(assignedToUserId);
        when(userProvider.findById(assignedToUserId)).thenReturn(Optional.of(user));
        when(employeeProvider.findById(user.employeeId())).thenReturn(Optional.of(buildEmployee()));
        doThrow(new RuntimeException("unexpected failure"))
                .when(notificationProvider).sendEmailNotification(any(), any(), any(), any());

        assertDoesNotThrow(() -> service.notifyResponsibilityAssigned(request, assignedToUserId));
    }

    @Test
    void notifyCompletionRequest_runtimeExceptionInProvider_coversCatchBlock() {
        var request = buildRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(buildEmployee()));
        doThrow(new RuntimeException("unexpected failure"))
                .when(notificationProvider).sendEmailNotification(any(), any(), any(), any());

        assertDoesNotThrow(() -> service.notifyCompletionRequest(request));
    }

    @Test
    void notifyRejectionRequest_runtimeExceptionInProvider_coversCatchBlock() {
        var request = buildRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(buildEmployee()));
        doThrow(new RuntimeException("unexpected failure"))
                .when(notificationProvider).sendEmailNotification(any(), any(), any(), any());

        assertDoesNotThrow(() -> service.notifyRejectionRequest(request));
    }

    @Test
    void notifyComplementRequest_runtimeExceptionInProvider_coversCatchBlock() {
        var request = buildRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(buildEmployee()));
        doThrow(new RuntimeException("unexpected failure"))
                .when(notificationProvider).sendEmailNotification(any(), any(), any(), any());

        assertDoesNotThrow(() -> service.notifyComplementRequest(request, "por favor complementar"));
    }

    // ── sendNotification with INTERNAL channel → covers else branch (line 231) ──

    @Test
    void sendNotification_internalChannel_callsSendInternalNotification() throws Exception {
        // sendNotification is private — call via reflection with INTERNAL channel
        // to cover the else branch (sendInternalNotification call)
        var request = buildRequest();
        // Build a NotificationContent via reflection (private inner record)
        Class<?> contentClass = null;
        for (Class<?> inner : LgpdRequestNotificationService.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("NotificationContent")) {
                contentClass = inner;
                break;
            }
        }
        var ctor = contentClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object content = ctor.newInstance(
                "recipient@example.com", "Subject", "<p>html</p>", "plain text"
        );

        Method sendNotification = LgpdRequestNotificationService.class.getDeclaredMethod(
                "sendNotification",
                java.util.UUID.class,
                java.util.UUID.class,
                String.class,
                NotificationChannel.class,
                contentClass
        );
        sendNotification.setAccessible(true);

        // Call with INTERNAL channel → covers else branch
        assertDoesNotThrow(() -> sendNotification.invoke(service,
                request.requestId(),
                request.requestedByUserId(),
                "TEST_TYPE",
                NotificationChannel.INTERNAL,
                content
        ));

        verify(notificationProvider).sendInternalNotification(
                eq(request.requestedByUserId()), anyString(), anyString(), eq("TEST_TYPE"), eq(request.requestId())
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private LgpdRequest buildRequest() {
        UUID requestId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID employeeId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID requestedByUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID companyId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Instant now = Instant.now();
        return new LgpdRequest(
                requestId, employeeId, requestedByUserId, companyId,
                LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN,
                "Solicitação de acesso", null,
                now, now, null, null, null, now.plusSeconds(604800),
                "NORMAL", null, null, null, null, null, false
        );
    }

    private Employee buildEmployee() {
        return new Employee(
                UUID.randomUUID(), "Test User", "12345678901", "12345678901", "Dev",
                "test@example.com", 5000.0, "21999999999", true,
                new Address("Rua A", "10", "00000000", "Rio", "RJ"),
                UUID.randomUUID(), null, false, null,
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );
    }

    private User buildUser(UUID userId) {
        return new User(
                userId, "user@example.com", "hash", Role.MANAGER, true,
                UUID.randomUUID(), 1L, null, null, null
        );
    }
}
