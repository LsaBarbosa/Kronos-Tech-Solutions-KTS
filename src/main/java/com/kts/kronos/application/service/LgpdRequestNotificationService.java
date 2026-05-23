package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.LgpdRequestNotificationRepository;
import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestNotificationEntity;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.NotificationProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.LgpdNotificationType;
import com.kts.kronos.domain.model.enuns.NotificationChannel;
import com.kts.kronos.domain.model.enuns.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LgpdRequestNotificationService {
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_SECONDS = 300;

    private final LgpdRequestNotificationRepository notificationRepository;
    private final UserProvider userProvider;
    private final EmployeeProvider employeeProvider;
    private final NotificationProvider notificationProvider;

    @Async
    public void notifyRequestCreated(LgpdRequest request) {
        try {
            var employee = employeeProvider.findById(request.employeeId()).orElse(null);
            if (employee == null) {
                log.warn("event=lgpd_notification_skipped reason=employee_not_found requestId={}", request.requestId());
                return;
            }

            sendNotification(
                    request.requestId(),
                    request.requestedByUserId(),
                    LgpdNotificationType.REQUEST_CREATED.name(),
                    NotificationChannel.EMAIL,
                    buildRequestCreatedContent(request, employee.email())
            );
        } catch (Exception e) {
            log.error("event=lgpd_notification_error type=request_created requestId={} error={}", request.requestId(), e.getMessage());
        }
    }

    @Async
    public void notifyStatusChanged(LgpdRequest request, String oldStatus, UUID changedByUserId) {
        try {
            var employee = employeeProvider.findById(request.employeeId()).orElse(null);
            if (employee == null) {
                log.warn("event=lgpd_notification_skipped reason=employee_not_found requestId={}", request.requestId());
                return;
            }

            sendNotification(
                    request.requestId(),
                    request.requestedByUserId(),
                    LgpdNotificationType.STATUS_CHANGED.name(),
                    NotificationChannel.EMAIL,
                    buildStatusChangedContent(request, oldStatus, employee.email())
            );
        } catch (Exception e) {
            log.error("event=lgpd_notification_error type=status_changed requestId={} error={}", request.requestId(), e.getMessage());
        }
    }

    @Async
    public void notifyResponsibilityAssigned(LgpdRequest request, UUID assignedToUserId) {
        try {
            var assignedUser = userProvider.findById(assignedToUserId).orElse(null);
            if (assignedUser == null) {
                log.warn("event=lgpd_notification_skipped reason=assigned_user_not_found userId={}", assignedToUserId);
                return;
            }

            var assignedEmployeeEmail = "no-reply@kronos-tech.com";
            var employee = employeeProvider.findById(assignedUser.employeeId()).orElse(null);
            if (employee != null) {
                assignedEmployeeEmail = employee.email();
            }

            sendNotification(
                    request.requestId(),
                    assignedToUserId,
                    LgpdNotificationType.RESPONSIBILITY_ASSIGNED.name(),
                    NotificationChannel.EMAIL,
                    buildResponsibilityAssignedContent(request, assignedEmployeeEmail)
            );
        } catch (Exception e) {
            log.error("event=lgpd_notification_error type=responsibility_assigned requestId={} error={}", request.requestId(), e.getMessage());
        }
    }

    @Async
    public void notifyCompletionRequest(LgpdRequest request) {
        try {
            var employee = employeeProvider.findById(request.employeeId()).orElse(null);
            if (employee == null) {
                log.warn("event=lgpd_notification_skipped reason=employee_not_found requestId={}", request.requestId());
                return;
            }

            sendNotification(
                    request.requestId(),
                    request.requestedByUserId(),
                    LgpdNotificationType.REQUEST_COMPLETED.name(),
                    NotificationChannel.EMAIL,
                    buildCompletionContent(request, employee.email())
            );
        } catch (Exception e) {
            log.error("event=lgpd_notification_error type=request_completed requestId={} error={}", request.requestId(), e.getMessage());
        }
    }

    @Async
    public void notifyRejectionRequest(LgpdRequest request) {
        try {
            var employee = employeeProvider.findById(request.employeeId()).orElse(null);
            if (employee == null) {
                log.warn("event=lgpd_notification_skipped reason=employee_not_found requestId={}", request.requestId());
                return;
            }

            sendNotification(
                    request.requestId(),
                    request.requestedByUserId(),
                    LgpdNotificationType.REQUEST_REJECTED.name(),
                    NotificationChannel.EMAIL,
                    buildRejectionContent(request, employee.email())
            );
        } catch (Exception e) {
            log.error("event=lgpd_notification_error type=request_rejected requestId={} error={}", request.requestId(), e.getMessage());
        }
    }

    @Async
    public void notifyComplementRequest(LgpdRequest request, String complementMessage) {
        try {
            var employee = employeeProvider.findById(request.employeeId()).orElse(null);
            if (employee == null) {
                log.warn("event=lgpd_notification_skipped reason=employee_not_found requestId={}", request.requestId());
                return;
            }

            sendNotification(
                    request.requestId(),
                    request.requestedByUserId(),
                    LgpdNotificationType.COMPLEMENT_REQUESTED.name(),
                    NotificationChannel.EMAIL,
                    buildComplementRequestContent(request, complementMessage, employee.email())
            );
        } catch (Exception e) {
            log.error("event=lgpd_notification_error type=complement_requested requestId={} error={}", request.requestId(), e.getMessage());
        }
    }

    @Transactional
    public void retryFailedNotifications() {
        var failedNotifications = notificationRepository.findFailedNotificationsReadyForRetry(Instant.now());

        for (var notification : failedNotifications) {
            if (notification.getRetryCount() < MAX_RETRY_ATTEMPTS) {
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setNextRetryAt(Instant.now().plusSeconds(RETRY_DELAY_SECONDS * (long) Math.pow(2, notification.getRetryCount())));
                notification.setStatus(NotificationStatus.PENDING.name());
                notificationRepository.save(notification);

                log.info("event=lgpd_notification_retry_scheduled notification_id={} retry_count={}", notification.getNotificationId(), notification.getRetryCount());
            } else {
                notification.setStatus(NotificationStatus.FAILED.name());
                notification.setFailureReason("Max retry attempts exceeded");
                notificationRepository.save(notification);

                log.warn("event=lgpd_notification_max_retries_exceeded notification_id={}", notification.getNotificationId());
            }
        }
    }

    private void sendNotification(
            UUID requestId,
            UUID recipientUserId,
            String notificationType,
            NotificationChannel channel,
            NotificationContent content
    ) {
        var notification = LgpdRequestNotificationEntity.builder()
                .requestId(requestId)
                .recipientUserId(recipientUserId)
                .notificationType(notificationType)
                .notificationChannel(channel.name())
                .status(NotificationStatus.PENDING.name())
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        try {
            if (channel == NotificationChannel.EMAIL) {
                notificationProvider.sendEmailNotification(
                        content.recipientEmail,
                        content.subject,
                        content.htmlContent,
                        content.plainTextContent
                );
            } else {
                notificationProvider.sendInternalNotification(
                        recipientUserId,
                        content.subject,
                        content.plainTextContent,
                        notificationType,
                        requestId
                );
            }

            notification.setStatus(NotificationStatus.SENT.name());
            notification.setSentAt(Instant.now());
            log.info("event=lgpd_notification_sent notification_id={} type={} channel={}", notification.getNotificationId(), notificationType, channel);
        } catch (NotificationProvider.NotificationException e) {
            notification.setStatus(NotificationStatus.FAILED.name());
            notification.setFailureReason(e.getMessage());
            notification.setNextRetryAt(Instant.now().plusSeconds(RETRY_DELAY_SECONDS));
            log.warn("event=lgpd_notification_failed type={} channel={} error={}", notificationType, channel, e.getMessage());
        }

        notificationRepository.save(notification);
    }

    private NotificationContent buildRequestCreatedContent(LgpdRequest request, String recipientEmail) {
        String htmlContent = String.format("""
                <h2>Sua solicitação LGPD foi criada</h2>
                <p>Solicitação ID: %s</p>
                <p>Tipo: %s</p>
                <p>Status: %s</p>
                <p>Você receberá atualizações quando a solicitação for processada.</p>
                """, request.requestId(), request.requestType(), request.status());

        String plainText = String.format("Sua solicitação LGPD %s foi criada. Você receberá atualizações sobre o progresso.", request.requestId());

        return new NotificationContent(recipientEmail, "Solicitação LGPD Criada", htmlContent, plainText);
    }

    private NotificationContent buildStatusChangedContent(LgpdRequest request, String oldStatus, String recipientEmail) {
        String htmlContent = String.format("""
                <h2>Status da sua solicitação foi atualizado</h2>
                <p>Solicitação ID: %s</p>
                <p>Status anterior: %s</p>
                <p>Novo status: %s</p>
                <p>Data de atualização: %s</p>
                """, request.requestId(), oldStatus, request.status(), request.updatedAt());

        String plainText = String.format("O status da sua solicitação %s foi atualizado para %s.", request.requestId(), request.status());

        return new NotificationContent(recipientEmail, "Solicitação LGPD Atualizada", htmlContent, plainText);
    }

    private NotificationContent buildResponsibilityAssignedContent(LgpdRequest request, String recipientEmail) {
        String htmlContent = String.format("""
                <h2>Você foi designado para atender uma solicitação LGPD</h2>
                <p>Solicitação ID: %s</p>
                <p>Tipo: %s</p>
                <p>Prazo: %s</p>
                <p>Por favor, revise a solicitação na plataforma Kronos.</p>
                """, request.requestId(), request.requestType(), request.dueAt());

        String plainText = String.format("Você foi designado para atender a solicitação LGPD %s. Prazo: %s", request.requestId(), request.dueAt());

        return new NotificationContent(recipientEmail, "Nova Solicitação LGPD Atribuída", htmlContent, plainText);
    }

    private NotificationContent buildCompletionContent(LgpdRequest request, String recipientEmail) {
        String htmlContent = String.format("""
                <h2>Sua solicitação LGPD foi concluída</h2>
                <p>Solicitação ID: %s</p>
                <p>Data de conclusão: %s</p>
                <p>%s</p>
                """, request.requestId(), request.resolvedAt(), request.publicResolutionNotes() != null ? request.publicResolutionNotes() : "");

        String plainText = String.format("Sua solicitação LGPD %s foi concluída em %s.", request.requestId(), request.resolvedAt());

        return new NotificationContent(recipientEmail, "Solicitação LGPD Concluída", htmlContent, plainText);
    }

    private NotificationContent buildRejectionContent(LgpdRequest request, String recipientEmail) {
        String htmlContent = String.format("""
                <h2>Sua solicitação LGPD foi rejeitada</h2>
                <p>Solicitação ID: %s</p>
                <p>Motivo: %s</p>
                <p>Data de rejeição: %s</p>
                <p>%s</p>
                """, request.requestId(), request.closedReason() != null ? request.closedReason() : "", request.resolvedAt(), request.publicResolutionNotes() != null ? request.publicResolutionNotes() : "");

        String plainText = String.format("Sua solicitação LGPD %s foi rejeitada. Motivo: %s", request.requestId(), request.closedReason());

        return new NotificationContent(recipientEmail, "Solicitação LGPD Rejeitada", htmlContent, plainText);
    }

    private NotificationContent buildComplementRequestContent(LgpdRequest request, String complementMessage, String recipientEmail) {
        String htmlContent = String.format("""
                <h2>Informações adicionais solicitadas para sua solicitação LGPD</h2>
                <p>Solicitação ID: %s</p>
                <p>Mensagem: %s</p>
                <p>Por favor, responda com as informações solicitadas na plataforma Kronos.</p>
                """, request.requestId(), complementMessage);

        String plainText = String.format("Informações adicionais foram solicitadas para sua solicitação LGPD %s: %s", request.requestId(), complementMessage);

        return new NotificationContent(recipientEmail, "Informações Adicionais Solicitadas", htmlContent, plainText);
    }

    private record NotificationContent(
            String recipientEmail,
            String subject,
            String htmlContent,
            String plainTextContent
    ) {}
}
