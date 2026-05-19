package com.kts.kronos.application.scheduler;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCleanupScheduler {
    private static final String SCHEDULER_NAME = "message_cleanup";

    private final MessageProvider messageProvider;
    private final KronosMetrics kronosMetrics;

    public MessageCleanupScheduler(MessageProvider messageProvider) {
        this(messageProvider, new KronosMetrics());
    }

    /**
     * Agenda a exclusão de mensagens antigas.
     * O cron "0 0 1 * * ?" significa: à 1h da manhã, todos os dias.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupOldMessages() {
        long startedAt = System.nanoTime();
        var thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        try {
            messageProvider.deleteByCreationDateBefore(thirtyDaysAgo);
            kronosMetrics.schedulerSuccess(SCHEDULER_NAME);
            kronosMetrics.recordSchedulerDuration(
                    SCHEDULER_NAME,
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt),
                    "success"
            );
            log.info("event=scheduler_execution result=success scheduler={}", SCHEDULER_NAME);
        } catch (RuntimeException e) {
            kronosMetrics.schedulerFailure(SCHEDULER_NAME);
            kronosMetrics.recordSchedulerDuration(
                    SCHEDULER_NAME,
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt),
                    "failure"
            );
            log.error("event=scheduler_execution result=failure scheduler={} reason=unknown exception_type={}",
                    SCHEDULER_NAME,
                    e.getClass().getSimpleName());
        }
    }
}
