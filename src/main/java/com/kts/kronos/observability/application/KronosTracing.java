package com.kts.kronos.observability.application;

import com.kts.kronos.observability.support.ObservabilityTagSanitizer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class KronosTracing {

    private final ObservationRegistry observationRegistry;
    private final ObservabilityTagSanitizer tagSanitizer;

    public KronosTracing(ObservationRegistry observationRegistry) {
        this(observationRegistry, new ObservabilityTagSanitizer());
    }

    public KronosTracing(
            ObservationRegistry observationRegistry,
            ObservabilityTagSanitizer tagSanitizer
    ) {
        this.observationRegistry = observationRegistry;
        this.tagSanitizer = tagSanitizer;
    }

    public void observe(String name, Runnable action, String... lowCardinalityTags) {
        var observation = createObservation(name, lowCardinalityTags);
        try (Observation.Scope ignored = observation.openScope()) {
            action.run();
        } catch (RuntimeException ex) {
            observation.error(ex);
            throw ex;
        } finally {
            observation.stop();
        }
    }

    public void observe(String name, Runnable action) {
        observe(name, action, new String[0]);
    }

    public <T> T observe(String name, Supplier<T> action, String... lowCardinalityTags) {
        var observation = createObservation(name, lowCardinalityTags);
        try (Observation.Scope ignored = observation.openScope()) {
            return action.get();
        } catch (RuntimeException ex) {
            observation.error(ex);
            throw ex;
        } finally {
            observation.stop();
        }
    }

    public <T> T observe(String name, Supplier<T> action) {
        return observe(name, action, new String[0]);
    }

    private Observation createObservation(String name, String... lowCardinalityTags) {
        Observation observation = Observation.createNotStarted(name, observationRegistry);
        if (lowCardinalityTags.length % 2 != 0) {
            throw new IllegalArgumentException("Observation tag key/value arguments must be even");
        }

        for (int i = 0; i < lowCardinalityTags.length; i += 2) {
            observation.lowCardinalityKeyValue(
                    tagSanitizer.sanitizeTagKey(lowCardinalityTags[i]),
                    tagSanitizer.sanitizeTagValue(lowCardinalityTags[i], lowCardinalityTags[i + 1])
            );
        }

        return observation.start();
    }
}
