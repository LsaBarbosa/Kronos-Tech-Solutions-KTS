package com.kts.kronos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileScanningStartupValidatorTest {

    @Test
    @DisplayName("shouldFailStartupWhenProductionProfileAndFileScanningDisabled")
    void shouldFailStartupWhenProductionProfileAndFileScanningDisabled() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        var validator = new FileScanningStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "fileScanningEnabled", false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                validator::validate
        );

        String expectedMessage = "File scanning must be enabled in production.";
        assert exception.getMessage().equals(expectedMessage) :
                "Expected message: '" + expectedMessage + "', but got: '" + exception.getMessage() + "'";
    }

    @Test
    @DisplayName("shouldStartWhenProductionProfileAndFileScanningEnabled")
    void shouldStartWhenProductionProfileAndFileScanningEnabled() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        var validator = new FileScanningStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "fileScanningEnabled", true);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    @DisplayName("shouldAllowFileScanningDisabledOutsideProduction")
    void shouldAllowFileScanningDisabledOutsideProduction() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});

        var validator = new FileScanningStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "fileScanningEnabled", false);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    @DisplayName("shouldFailStartupWhenProductionAliasAndFileScanningDisabled")
    void shouldFailStartupWhenProductionAliasAndFileScanningDisabled() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});

        var validator = new FileScanningStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "fileScanningEnabled", false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                validator::validate
        );

        String expectedMessage = "File scanning must be enabled in production.";
        assert exception.getMessage().equals(expectedMessage) :
                "Expected message: '" + expectedMessage + "', but got: '" + exception.getMessage() + "'";
    }

    @Test
    @DisplayName("shouldAllowFileScanningDisabledInTestProfile")
    void shouldAllowFileScanningDisabledInTestProfile() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        var validator = new FileScanningStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "fileScanningEnabled", false);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    @DisplayName("shouldFailStartupWhenMultipleProfilesIncludeProdAndFileScanningDisabled")
    void shouldFailStartupWhenMultipleProfilesIncludeProdAndFileScanningDisabled() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local", "prod"});

        var validator = new FileScanningStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "fileScanningEnabled", false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                validator::validate
        );

        String expectedMessage = "File scanning must be enabled in production.";
        assert exception.getMessage().equals(expectedMessage) :
                "Expected message: '" + expectedMessage + "', but got: '" + exception.getMessage() + "'";
    }
}
