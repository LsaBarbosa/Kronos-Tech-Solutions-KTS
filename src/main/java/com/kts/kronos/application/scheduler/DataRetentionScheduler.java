package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.retention.RetentionExecutionService;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Component
@ConditionalOnProperty(name = "kronos.lgpd.retention.scheduler.enabled", havingValue = "true")
public class DataRetentionScheduler {
    private static final String SCHEDULER_NAME = "data_retention";

    private final RetentionExecutionService retentionExecutionService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;
    private final String schedulerMode;
    private final boolean schedulerApplyConfirmed;
    private final String schedulerJustification;

    @Autowired
    public DataRetentionScheduler(
            RetentionExecutionService retentionExecutionService,
            KronosMetrics kronosMetrics,
            KronosTracing kronosTracing,
            @Value("${kronos.lgpd.retention.scheduler.mode:DRY_RUN}") String schedulerMode,
            @Value("${kronos.lgpd.retention.scheduler.apply-confirmed:false}") boolean schedulerApplyConfirmed,
            @Value("${kronos.lgpd.retention.scheduler.justification:Scheduled LGPD retention batch}") String schedulerJustification
    ) {
        this.retentionExecutionService = retentionExecutionService;
        this.kronosMetrics = kronosMetrics;
        this.kronosTracing = kronosTracing;
        this.schedulerMode = schedulerMode;
        this.schedulerApplyConfirmed = schedulerApplyConfirmed;
        this.schedulerJustification = schedulerJustification;
    }

    public DataRetentionScheduler(
            RetentionExecutionService retentionExecutionService,
            KronosMetrics kronosMetrics,
            String schedulerMode,
            boolean schedulerApplyConfirmed,
            String schedulerJustification
    ) {
        this(
                retentionExecutionService,
                kronosMetrics,
                ObservabilityDefaults.tracing(),
                schedulerMode,
                schedulerApplyConfirmed,
                schedulerJustification
        );
    }

    @Scheduled(cron = "${kronos.lgpd.retention.scheduler.cron:0 15 4 * * ?}", zone = "America/Sao_Paulo")
    @Transactional
    public void executeRetentionPolicies() {
        long startedAt = System.nanoTime();

        try {
            var summary = kronosTracing.observe("kronos.scheduler.retention", () -> retentionExecutionService.executeActivePolicies(
                    resolveMode(),
                    schedulerJustification,
                    schedulerApplyConfirmed,
                    "scheduler"
            ), "scheduler", SCHEDULER_NAME, "result", "success");
            kronosMetrics.schedulerRecordsProcessed(SCHEDULER_NAME, summary.totalPolicies());
            kronosMetrics.schedulerSuccess(SCHEDULER_NAME);
            kronosMetrics.recordSchedulerDuration(
                    SCHEDULER_NAME,
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt),
                    "success"
            );
            log.info(
                    "event=scheduler_execution result=success scheduler={} mode={} processedPolicies={} totalErrors={}",
                    SCHEDULER_NAME,
                    summary.mode(),
                    summary.totalPolicies(),
                    summary.totalErrors()
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

    private RetentionExecutionMode resolveMode() {
        return RetentionExecutionMode.valueOf(schedulerMode.trim().toUpperCase(Locale.ROOT));
    }
}
