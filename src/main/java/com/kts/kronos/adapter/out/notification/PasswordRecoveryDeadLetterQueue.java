package com.kts.kronos.adapter.out.notification;
import com.kts.kronos.adapter.in.web.dto.event.PasswordRecoveryEmailRequestedEvent;
import com.kts.kronos.adapter.in.web.dto.user.DeadLetterEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class PasswordRecoveryDeadLetterQueue {
    private final ConcurrentLinkedQueue<DeadLetterEmail> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(PasswordRecoveryEmailRequestedEvent event, String reason) {
        var deadLetter = new DeadLetterEmail(event, reason, LocalDateTime.now());
        queue.add(deadLetter);
        log.error("event=PASSWORD_RECOVERY_EMAIL_DLQ userId={} email={} reason={}",
                event.userId(), event.toEmail(), reason);
    }

    public int size() {
        return queue.size();
    }

    public List<DeadLetterEmail> snapshot() {
        return List.copyOf(queue);
    }

}
