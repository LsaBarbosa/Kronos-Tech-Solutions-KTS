package com.kts.kronos.adapter.out.notification;
import com.kts.kronos.adapter.in.web.dto.event.PasswordRecoveryEmailRequestedEvent;
import com.kts.kronos.adapter.in.web.dto.user.DeadLetterEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.kts.kronos.constants.Logs.LOG_PASSWORD_RECOVERY_DLQ_ENQUEUED;
import static com.kts.kronos.constants.Messages.MASKED_EMAIL_UNAVAILABLE;
import static com.kts.kronos.constants.Messages.MASKED_VALUE;

@Slf4j
@Component
public class PasswordRecoveryDeadLetterQueue {
    private final ConcurrentLinkedQueue<DeadLetterEmail> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(PasswordRecoveryEmailRequestedEvent event, String reason) {
        var deadLetter = new DeadLetterEmail(event, reason, LocalDateTime.now());
        queue.add(deadLetter);
        log.error(LOG_PASSWORD_RECOVERY_DLQ_ENQUEUED,
                event.userId(), maskEmail(event.toEmail()), reason);
    }

    public int size() {
        return queue.size();
    }

    public List<DeadLetterEmail> snapshot() {
        return List.copyOf(queue);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return MASKED_EMAIL_UNAVAILABLE;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return MASKED_VALUE;
        }

        return email.charAt(0) + MASKED_VALUE + email.substring(atIndex);
    }

}
