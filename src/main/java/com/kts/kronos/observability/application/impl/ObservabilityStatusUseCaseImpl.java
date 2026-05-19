package com.kts.kronos.observability.application.impl;

import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
import com.kts.kronos.observability.domain.ObservabilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ObservabilityStatusUseCaseImpl implements ObservabilityStatusUseCase {

    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");

    private final HealthEndpoint healthEndpoint;

    @Value("${spring.application.name:kronos-backend}")
    private String applicationName;

    @Value("${APP_ENV:local}")
    private String environment;

    @Override
    public ObservabilityStatus getStatus() {
        HealthComponent health = healthEndpoint.health();

        return new ObservabilityStatus(
                applicationName,
                health.getStatus().getCode(),
                environment,
                OffsetDateTime.now(SAO_PAULO_ZONE)
        );
    }
}
