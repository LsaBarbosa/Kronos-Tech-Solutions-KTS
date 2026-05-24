package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.LgpdRetentionDryRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "kronos.lgpd.retention.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class LgpdRetentionScheduler {
    private final LgpdRetentionDryRunService lgpdRetentionDryRunService;

    @Scheduled(cron = "${kronos.lgpd.retention.scheduler.cron:0 15 4 * * ?}")
    public void executeDryRunRetention() {
        try {
            log.info("event=lgpd_retention_scheduler_started mode=DRY_RUN");
            var results = lgpdRetentionDryRunService.executeDryRun();
            log.info("event=lgpd_retention_scheduler_completed mode=DRY_RUN totalResults={}", results.size());
        } catch (Exception e) {
            log.error("event=lgpd_retention_scheduler_error error={}", e.getMessage(), e);
        }
    }
}
