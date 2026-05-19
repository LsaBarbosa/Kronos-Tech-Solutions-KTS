package com.kts.kronos.observability.application;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class KronosTracing {

    private final ObservationRegistry observationRegistry;

    public KronosTracing() {
        this(ObservationRegistry.create());
    }

    public void observe(String name, Runnable action) {
        Observation observation = Observation.start(name, observationRegistry);
        try (Observation.Scope ignored = observation.openScope()) {
            action.run();
        } catch (RuntimeException ex) {
            observation.error(ex);
            throw ex;
        } finally {
            observation.stop();
        }
    }

    public <T> T observe(String name, Supplier<T> action) {
        Observation observation = Observation.start(name, observationRegistry);
        try (Observation.Scope ignored = observation.openScope()) {
            return action.get();
        } catch (RuntimeException ex) {
            observation.error(ex);
            throw ex;
        } finally {
            observation.stop();
        }
    }
}
