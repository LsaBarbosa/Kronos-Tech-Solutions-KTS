package com.kts.kronos;

import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RekognitionSetupCoverage2Test {

    @Mock private FaceRecognitionProvider faceRecognitionProvider;

    // Covers L23 FALSE branch (profile != "test") + L29-34 (try body: log + ensureCollectionExists)
    @Test
    void initializeRekognitionCollection_withNonTestProfile_callsProvider() {
        var setup = new RekognitionSetup(faceRecognitionProvider);
        ReflectionTestUtils.setField(setup, "activeProfile", "prod");

        setup.initializeRekognitionCollection();

        verify(faceRecognitionProvider).ensureCollectionExists();
    }

    // Covers L34-35 (catch block: provider throws, exception is swallowed)
    @Test
    void initializeRekognitionCollection_whenProviderThrows_doesNotPropagate() {
        var setup = new RekognitionSetup(faceRecognitionProvider);
        ReflectionTestUtils.setField(setup, "activeProfile", "prod");
        doThrow(new RuntimeException("AWS unavailable")).when(faceRecognitionProvider).ensureCollectionExists();

        assertDoesNotThrow(() -> setup.initializeRekognitionCollection());

        verify(faceRecognitionProvider).ensureCollectionExists();
    }

    private void assertDoesNotThrow(ThrowableRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            throw new AssertionError("Expected no exception but got: " + t, t);
        }
    }

    @FunctionalInterface
    interface ThrowableRunnable {
        void run() throws Throwable;
    }
}
