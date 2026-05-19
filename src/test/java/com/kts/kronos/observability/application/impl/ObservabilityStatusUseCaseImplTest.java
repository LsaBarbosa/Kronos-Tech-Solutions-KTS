package com.kts.kronos.observability.application.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObservabilityStatusUseCaseImplTest {

    @Test
    void shouldReturnSanitizedStatusPayload() {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.up().withDetail("db", "hidden").build());

        var useCase = new ObservabilityStatusUseCaseImpl(healthEndpoint);
        ReflectionTestUtils.setField(useCase, "applicationName", "kronos-backend");
        ReflectionTestUtils.setField(useCase, "environment", "prod");

        var result = useCase.getStatus();

        assertEquals("kronos-backend", result.application());
        assertEquals("UP", result.status());
        assertEquals("prod", result.environment());
        assertNotNull(result.timestamp());
    }
}
