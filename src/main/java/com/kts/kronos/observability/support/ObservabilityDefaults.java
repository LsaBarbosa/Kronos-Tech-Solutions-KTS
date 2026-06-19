package com.kts.kronos.observability.support;

import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.observation.ObservationRegistry;

public final class ObservabilityDefaults {

    private static final ObservabilityTagSanitizer TAG_SANITIZER = new ObservabilityTagSanitizer();

    private ObservabilityDefaults() {
    }

    public static KronosMetrics metrics() {
        return new KronosMetrics(Metrics.globalRegistry, TAG_SANITIZER);
    }

    public static KronosTracing tracing() {
        return new KronosTracing(ObservationRegistry.NOOP, TAG_SANITIZER);
    }
}
