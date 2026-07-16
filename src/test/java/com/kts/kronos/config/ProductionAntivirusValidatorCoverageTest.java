package com.kts.kronos.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductionAntivirusValidatorCoverageTest {

    // ── L28 FALSE: isProduction=true → !isProduction=FALSE → continues to L32 ──
    // ── L32 TRUE: antivirusEnabled=false → log warning block (L33-52) ───────────

    @Test
    void validateAntivirusConfiguration_productionWithDisabledAntivirus_logsWarning() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionAntivirusValidator validator = new ProductionAntivirusValidator(env);
        ReflectionTestUtils.setField(validator, "antivirusEnabled", false);

        assertDoesNotThrow(validator::validateAntivirusConfiguration);
    }

    // ── L32 FALSE: antivirusEnabled=true → log info branch ──────────────────────

    @Test
    void validateAntivirusConfiguration_productionWithEnabledAntivirus_logsInfo() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionAntivirusValidator validator = new ProductionAntivirusValidator(env);
        ReflectionTestUtils.setField(validator, "antivirusEnabled", true);

        assertDoesNotThrow(validator::validateAntivirusConfiguration);
    }
}
