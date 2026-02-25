package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.out.notification.PasswordRecoveryDeadLetterQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordRecoveryDeadLetterMonitorScheduler {

    private final PasswordRecoveryDeadLetterQueue deadLetterQueue;

    @Scheduled(cron = "0 */10 * * * *", zone = "America/Sao_Paulo")
    public void logQueueSize() {
        var queueSize = deadLetterQueue.size();
        if (queueSize > 0) {
            log.warn("event=PASSWORD_RECOVERY_EMAIL_DLQ_MONITOR queueSize={}", queueSize);
        }
    }
}
