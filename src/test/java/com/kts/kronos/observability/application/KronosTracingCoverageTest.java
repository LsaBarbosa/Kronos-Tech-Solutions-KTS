package com.kts.kronos.observability.application;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KronosTracingCoverageTest {

    @Test
    void createObservation_oddNumberOfTags_throwsIllegalArgumentException() throws Exception {
        var tracer = new KronosTracing(ObservationRegistry.NOOP);
        Method method = KronosTracing.class.getDeclaredMethod("createObservation", String.class, String[].class);
        method.setAccessible(true);
        var ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(tracer, "test.observation", new String[]{"single_key"}));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }
}
