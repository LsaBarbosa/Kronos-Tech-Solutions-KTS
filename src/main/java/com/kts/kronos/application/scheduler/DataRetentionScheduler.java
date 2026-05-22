package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.RetentionPolicyService;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "kronos.lgpd.retention.scheduler.enabled", havingValue = "true")
public class DataRetentionScheduler {
    private static final String SCHEDULER_NAME = "data_retention";

    private final RetentionPolicyService retentionPolicyService;
    private final KronosMetrics kronosMetrics;

    @Autowired
    public DataRetentionScheduler(
            RetentionPolicyService retentionPolicyService,
            KronosMetrics kronosMetrics
    ) {
        this.retentionPolicyService = retentionPolicyService;
        this.kronosMetrics = kronosMetrics;
    }

    @Scheduled(cron = "${kronos.lgpd.retention.scheduler.cron:0 15 4 * * ?}", zone = "America/Sao_Paulo")
    @Transactional
    public void executeRetentionPolicies() {
        long startedAt = System.nanoTime();

        try {
            int processedPolicies = retentionPolicyService.executeEnabledPolicies();
            kronosMetrics.schedulerRecordsProcessed(SCHEDULER_NAME, processedPolicies);
            kronosMetrics.schedulerSuccess(SCHEDULER_NAME);
            kronosMetrics.recordSchedulerDuration(
                    SCHEDULER_NAME,
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt),
                    "success"
            );
            log.info(
                    "event=scheduler_execution result=success scheduler={} processedPolicies={}",
                    SCHEDULER_NAME,
                    processedPolicies
            );
        } catch (RuntimeException e) {
            kronosMetrics.schedulerFailure(SCHEDULER_NAME);
            kronosMetrics.recordSchedulerDuration(
                    SCHEDULER_NAME,
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt),
                    "failure"
            );
            log.error(
                    "event=scheduler_execution result=failure scheduler={} reason=unknown exception_type={}",
                    SCHEDULER_NAME,
                    e.getClass().getSimpleName()
            );
        }
    }
}
