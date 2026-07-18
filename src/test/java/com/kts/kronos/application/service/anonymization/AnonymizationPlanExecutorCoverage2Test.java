package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.port.out.provider.AnonymizationExecutionLogProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnonymizationPlanExecutorCoverage2Test {

    @Mock
    private AnonymizationExecutionLogProvider executionLogProvider;

    private final PrivacyLogReferenceService privacyLogReferenceService =
            new PrivacyLogReferenceService("test-lgpd-log-secret");

    // L103-104: private executeProcessor() method — calls executeProcessorWithResult and discards result
    @Test
    void executeProcessor_viaReflection_discardedResult() throws Exception {
        var executor = new AnonymizationPlanExecutor(
                List.of(), executionLogProvider, privacyLogReferenceService);

        Method method = AnonymizationPlanExecutor.class.getDeclaredMethod(
                "executeProcessor",
                Map.class,
                AnonymizationResourceType.class,
                AnonymizationPlan.class,
                String.class
        );
        method.setAccessible(true);

        var plan = new AnonymizationPlan(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "test reason",
                false, false, false, false, false, false
        );

        // Call the private method — no processor registered → result is logged and returned (discarded)
        assertDoesNotThrow(() ->
                method.invoke(executor, Map.of(), AnonymizationResourceType.EMPLOYEE, plan, "DRY_RUN"));
    }
}
