package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.NtpTimeService;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeSyncScheduler {
    private static final String SCHEDULER_NAME = "time_sync";

    private final NtpTimeService ntpService;
    private final KronosMetrics kronosMetrics;

    public TimeSyncScheduler(NtpTimeService ntpService) {
        this(ntpService, new KronosMetrics());
    }

    @Value("${kronos.ntp.max-drift-seconds:5}")
    private int maxDriftSeconds;

    // Executa a cada 1 hora (3600000 ms)
    @Scheduled(fixedRate = 3600000) 
    public void checkTimeSynchronization() {
        long startedAt = System.nanoTime();
        var offset = ntpService.getNetworkTimeOffset();

        if (offset == null) {
            kronosMetrics.schedulerFailure(SCHEDULER_NAME);
            kronosMetrics.recordSchedulerDuration(
                    SCHEDULER_NAME,
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt),
                    "failure"
            );
            log.warn("event=scheduler_execution result=failure scheduler={} reason=ntp_unavailable", SCHEDULER_NAME);
            return;
        }

        long absOffset = Math.abs(offset);
        if (absOffset > (maxDriftSeconds * 1000L)) {
            kronosMetrics.schedulerFailure(SCHEDULER_NAME);
            kronosMetrics.recordSchedulerDuration(
                    SCHEDULER_NAME,
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt),
                    "failure"
            );
            log.error("event=scheduler_execution result=failure scheduler={} reason=ntp_drift", SCHEDULER_NAME);
        } else {
            kronosMetrics.schedulerSuccess(SCHEDULER_NAME);
            kronosMetrics.recordSchedulerDuration(
                    SCHEDULER_NAME,
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt),
                    "success"
            );
            log.info("event=scheduler_execution result=success scheduler={}", SCHEDULER_NAME);
        }
    }
}
