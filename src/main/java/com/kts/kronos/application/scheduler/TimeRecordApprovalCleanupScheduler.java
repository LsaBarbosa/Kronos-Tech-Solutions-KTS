package com.kts.kronos.application.scheduler;
import com.kts.kronos.adapter.out.persistence.TimeRecordApprovalRepository;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
public class TimeRecordApprovalCleanupScheduler {
    private static final String SCHEDULER_NAME = "approval_cleanup";

    private final TimeRecordApprovalRepository repository;
    private final KronosMetrics kronosMetrics;
    private static final int DAYS_TO_KEEP = 31;

    @Autowired
    public TimeRecordApprovalCleanupScheduler(
            TimeRecordApprovalRepository repository,
            KronosMetrics kronosMetrics
    ) {
        this.repository = repository;
        this.kronosMetrics = kronosMetrics;
    }

    @Scheduled(cron = "0 30 2 * * ?", zone = "America/Sao_Paulo")
    @Transactional
    public void cleanupOldApprovals() {
        long startedAt = System.nanoTime();
        var threshold = LocalDateTime.now(SAO_PAULO).minusDays(DAYS_TO_KEEP);

        try {
            repository.deleteByCreatedAtBefore(threshold);
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
