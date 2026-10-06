package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.LgpdRequestNotificationRepository;
import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestNotificationEntity;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.NotificationProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestNotificationEntity;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.LgpdNotificationType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import com.kts.kronos.domain.model.enuns.NotificationChannel;
import com.kts.kronos.domain.model.enuns.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class LgpdRequestNotificationServiceTest {

    @Mock
    private LgpdRequestNotificationRepository notificationRepository;
    @Mock
    private UserProvider userProvider;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private NotificationProvider notificationProvider;

    private LgpdRequestNotificationService service;

    @BeforeEach
    void setUp() {
        service = new LgpdRequestNotificationService(
                notificationRepository,
                userProvider,
                employeeProvider,
                notificationProvider,
                new PrivacyLogReferenceService("test-log-secret")
        );
    }

    @Test
    void shouldCreateSpringBeanWithEmailAndInternalNotificationProviders() {
        LgpdRequestNotificationRepository repository = mock(LgpdRequestNotificationRepository.class);
        UserProvider userProvider = mock(UserProvider.class);
        EmployeeProvider employeeProvider = mock(EmployeeProvider.class);
        NotificationProvider emailProvider = mock(NotificationProvider.class);
        NotificationProvider internalProvider = mock(NotificationProvider.class);

        new ApplicationContextRunner()
                .withBean(LgpdRequestNotificationRepository.class, () -> repository)
                .withBean(UserProvider.class, () -> userProvider)
                .withBean(EmployeeProvider.class, () -> employeeProvider)
                .withBean("emailNotificationProviderImpl", NotificationProvider.class, () -> emailProvider)
                .withBean("internalNotificationProviderImpl", NotificationProvider.class, () -> internalProvider)
                .withBean(PrivacyLogReferenceService.class, () -> new PrivacyLogReferenceService("test-log-secret"))
                .withBean(LgpdRequestNotificationService.class)
                .run(context -> {
                    assertNotNull(context.getBean(LgpdRequestNotificationService.class));
                    assertNotNull(context.getBean("emailNotificationProviderImpl"));
                    assertNotNull(context.getBean("internalNotificationProviderImpl"));
                });
    }

    @Test
    void shouldUseEmailProviderForLgpdRequestNotificationWhenBothProvidersExist() {
        LgpdRequest request = lgpdRequest();
        Employee employee = employee("titular@example.com");
        LgpdRequestNotificationRepository repository = mock(LgpdRequestNotificationRepository.class);
        UserProvider userProvider = mock(UserProvider.class);
        EmployeeProvider employeeProvider = mock(EmployeeProvider.class);
        NotificationProvider emailProvider = mock(NotificationProvider.class);
        NotificationProvider internalProvider = mock(NotificationProvider.class);

        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee));

        new ApplicationContextRunner()
                .withBean(LgpdRequestNotificationRepository.class, () -> repository)
                .withBean(UserProvider.class, () -> userProvider)
                .withBean(EmployeeProvider.class, () -> employeeProvider)
                .withBean("emailNotificationProviderImpl", NotificationProvider.class, () -> emailProvider)
                .withBean("internalNotificationProviderImpl", NotificationProvider.class, () -> internalProvider)
                .withBean(PrivacyLogReferenceService.class, () -> new PrivacyLogReferenceService("test-log-secret"))
                .withBean(LgpdRequestNotificationService.class)
                .run(context -> {
                    context.getBean(LgpdRequestNotificationService.class).notifyRequestCreated(request);

                    verify(emailProvider).sendEmailNotification(
                            eq("titular@example.com"),
                            anyString(),
                            anyString(),
                            anyString()
                    );
                    verify(internalProvider, never()).sendEmailNotification(anyString(), anyString(), anyString(), anyString());
                    verify(internalProvider, never()).sendInternalNotification(any(), anyString(), anyString(), anyString(), any());
                });
    }

    @Test
    void shouldPersistSentEmailNotification() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee("titular@example.com")));

        service.notifyRequestCreated(request);

        ArgumentCaptor<LgpdRequestNotificationEntity> captor = ArgumentCaptor.forClass(LgpdRequestNotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        LgpdRequestNotificationEntity saved = captor.getValue();

        assertEquals(request.requestId(), saved.getRequestId());
        assertEquals(request.requestedByUserId(), saved.getRecipientUserId());
        assertEquals(LgpdNotificationType.REQUEST_CREATED.name(), saved.getNotificationType());
        assertEquals(NotificationChannel.EMAIL.name(), saved.getNotificationChannel());
        assertEquals(NotificationStatus.SENT.name(), saved.getStatus());
        verify(notificationProvider, never()).sendInternalNotification(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldNotLogSensitiveProviderFailureDetails(CapturedOutput output) {
        LgpdRequest request = lgpdRequest();
        String rawEmail = "titular.sensivel@example.com";
        String rawCpf = "12345678901";
        String rawPhone = "+5511999999999";
        String rawToken = "token-super-secreto";
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee(rawEmail)));
        doThrow(new NotificationProvider.NotificationException(
                "cpf=" + rawCpf + " email=" + rawEmail + " phone=" + rawPhone + " token=" + rawToken
        )).when(notificationProvider).sendEmailNotification(anyString(), anyString(), anyString(), anyString());

        service.notifyRequestCreated(request);

        String logs = output.getAll();
        assertFalse(logs.contains(rawCpf));
        assertFalse(logs.contains(rawEmail));
        assertFalse(logs.contains(rawPhone));
        assertFalse(logs.contains(rawToken));

        ArgumentCaptor<LgpdRequestNotificationEntity> captor = ArgumentCaptor.forClass(LgpdRequestNotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        LgpdRequestNotificationEntity saved = captor.getValue();
        assertEquals(NotificationStatus.FAILED.name(), saved.getStatus());
        assertFalse(saved.getFailureReason().contains(rawCpf));
        assertFalse(saved.getFailureReason().contains(rawEmail));
        assertFalse(saved.getFailureReason().contains(rawPhone));
        assertFalse(saved.getFailureReason().contains(rawToken));
    }

    private static LgpdRequest lgpdRequest() {
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        return new LgpdRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                LgpdRequestType.ACCESS,
                LgpdRequestStatus.OPEN,
                "Solicitacao de acesso",
                null,
                now,
                now,
                null,
                null,
                null,
                now.plusSeconds(604800),
                "NORMAL",
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    // ==================== NOTIFY STATUS CHANGED ====================

    @Test
    void shouldNotifyStatusChanged() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee("titular@example.com")));

        service.notifyStatusChanged(request, "OPEN", request.requestedByUserId());

        verify(notificationRepository).save(any());
    }

    @Test
    void shouldSkipStatusChangedWhenEmployeeNotFound() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.empty());

        service.notifyStatusChanged(request, "OPEN", request.requestedByUserId());

        verify(notificationRepository, never()).save(any());
    }

    // ==================== NOTIFY RESPONSIBILITY ASSIGNED ====================

    @Test
    void shouldNotifyResponsibilityAssigned() {
        LgpdRequest request = lgpdRequest();
        UUID assignedUserId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        User user = new User(assignedUserId, "gestor@kts.com", "hash", Role.MANAGER, true, empId);
        Employee emp = employee("gestor@kts.com");
        when(userProvider.findById(assignedUserId)).thenReturn(Optional.of(user));
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(emp));

        service.notifyResponsibilityAssigned(request, assignedUserId);

        verify(notificationRepository).save(any());
    }

    @Test
    void shouldUseDefaultEmailWhenEmployeeNotFoundInResponsibility() {
        LgpdRequest request = lgpdRequest();
        UUID assignedUserId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        User user = new User(assignedUserId, "gestor@kts.com", "hash", Role.MANAGER, true, empId);
        when(userProvider.findById(assignedUserId)).thenReturn(Optional.of(user));
        when(employeeProvider.findById(empId)).thenReturn(Optional.empty());

        service.notifyResponsibilityAssigned(request, assignedUserId);

        verify(notificationRepository).save(any());
    }

    @Test
    void shouldSkipResponsibilityNotificationWhenUserNotFound() {
        LgpdRequest request = lgpdRequest();
        UUID assignedUserId = UUID.randomUUID();
        when(userProvider.findById(assignedUserId)).thenReturn(Optional.empty());

        service.notifyResponsibilityAssigned(request, assignedUserId);

        verify(notificationRepository, never()).save(any());
    }

    // ==================== NOTIFY COMPLETION ====================

    @Test
    void shouldNotifyCompletion() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee("titular@example.com")));

        service.notifyCompletionRequest(request);

        verify(notificationRepository).save(any());
    }

    @Test
    void shouldSkipCompletionWhenEmployeeNotFound() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.empty());

        service.notifyCompletionRequest(request);

        verify(notificationRepository, never()).save(any());
    }

    // ==================== NOTIFY REJECTION ====================

    @Test
    void shouldNotifyRejection() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee("titular@example.com")));

        service.notifyRejectionRequest(request);

        verify(notificationRepository).save(any());
    }

    // ==================== NOTIFY COMPLEMENT ====================

    @Test
    void shouldNotifyComplement() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee("titular@example.com")));

        service.notifyComplementRequest(request, "Por favor envie o documento X");

        verify(notificationRepository).save(any());
    }

    // ==================== RETRY FAILED NOTIFICATIONS ====================

    @Test
    void shouldRetryFailedNotificationsWithinMaxAttempts() {
        LgpdRequestNotificationEntity notification = LgpdRequestNotificationEntity.builder()
                .notificationId(UUID.randomUUID())
                .requestId(UUID.randomUUID())
                .recipientUserId(UUID.randomUUID())
                .notificationType("REQUEST_CREATED")
                .notificationChannel("EMAIL")
                .status("FAILED")
                .retryCount(1)
                .createdAt(Instant.now())
                .build();
        when(notificationRepository.findFailedNotificationsReadyForRetry(any()))
                .thenReturn(List.of(notification));

        service.retryFailedNotifications();

        verify(notificationRepository).save(notification);
        assertEquals("PENDING", notification.getStatus());
        assertEquals(2, notification.getRetryCount());
    }

    @Test
    void shouldMarkNotificationAsFailedWhenMaxRetriesExceeded() {
        LgpdRequestNotificationEntity notification = LgpdRequestNotificationEntity.builder()
                .notificationId(UUID.randomUUID())
                .requestId(UUID.randomUUID())
                .recipientUserId(UUID.randomUUID())
                .notificationType("REQUEST_CREATED")
                .notificationChannel("EMAIL")
                .status("FAILED")
                .retryCount(3) // MAX_RETRY_ATTEMPTS = 3
                .createdAt(Instant.now())
                .build();
        when(notificationRepository.findFailedNotificationsReadyForRetry(any()))
                .thenReturn(List.of(notification));

        service.retryFailedNotifications();

        verify(notificationRepository).save(notification);
        assertEquals("FAILED", notification.getStatus());
        assertEquals("Max retry attempts exceeded", notification.getFailureReason());
    }

    private static Employee employee(String email) {
        return new Employee(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                "Titular LGPD",
                "12345678901",
                "12345678901",
                "Analista",
                email,
                1000.0,
                "+5511999999999",
                true,
                null,
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
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
                null,
                null,
                null,
                null,
                Set.of(),
                null,
                null,
                null
        );
    }

    @Test
    void shouldSkipRequestCreatedWhenEmployeeNotFound() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.empty());

        service.notifyRequestCreated(request);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldSkipRejectionWhenEmployeeNotFound() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.empty());

        service.notifyRejectionRequest(request);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldSkipComplementWhenEmployeeNotFound() {
        LgpdRequest request = lgpdRequest();
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.empty());

        service.notifyComplementRequest(request, "Por favor envie documentos");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldNotifyCompletionWithResolutionNotes() {
        LgpdRequest request = lgpdRequestWithNotes("Solicitação processada com sucesso.", "DELETION foi concluída.");
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee("titular@example.com")));

        service.notifyCompletionRequest(request);

        verify(notificationRepository).save(any());
    }

    @Test
    void shouldNotifyRejectionWithClosedReasonAndNotes() {
        LgpdRequest request = lgpdRequestWithNotes("Motivo da rejeição.", "Rejeição justificada.");
        when(employeeProvider.findById(request.employeeId())).thenReturn(Optional.of(employee("titular@example.com")));

        service.notifyRejectionRequest(request);

        verify(notificationRepository).save(any());
    }


    private static LgpdRequest lgpdRequestWithNotes(String closedReason, String publicResolutionNotes) {
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        return new LgpdRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                LgpdRequestType.DELETION,
                LgpdRequestStatus.COMPLETED,
                "Solicitação de exclusão",
                "Nota interna",
                now,
                now,
                now,
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                null,
                now.plusSeconds(604800),
                "NORMAL",
                closedReason,
                publicResolutionNotes,
                null,
                null,
                null,
                false
        );
    }
}
