package com.kts.kronos.config.demo;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class DemoSandboxPropertiesTest {

    @Test
    void gettersAndSetters_shouldRoundTrip() {
        DemoSandboxProperties props = new DemoSandboxProperties();

        props.setEnabled(true);
        assertThat(props.isEnabled()).isTrue();

        props.setKillSwitch(true);
        assertThat(props.isKillSwitch()).isTrue();

        props.setSandboxKey("TEST_KEY");
        assertThat(props.getSandboxKey()).isEqualTo("TEST_KEY");

        props.setCompanyName("Test Company");
        assertThat(props.getCompanyName()).isEqualTo("Test Company");

        props.setUsername("test_user");
        assertThat(props.getUsername()).isEqualTo("test_user");

        props.setInitialPassword("pass123#");
        assertThat(props.getInitialPassword()).isEqualTo("pass123#");

        props.setLocalStorageRoot("/tmp/test-sandbox");
        assertThat(props.getLocalStorageRoot()).isEqualTo("/tmp/test-sandbox");

        props.setLockTimeout(Duration.ofMinutes(10));
        assertThat(props.getLockTimeout()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void defaults_shouldMatchConfiguredValues() {
        DemoSandboxProperties props = new DemoSandboxProperties();

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.isKillSwitch()).isFalse();
        assertThat(props.getSandboxKey()).isEqualTo("KRONOS_TESTE");
        assertThat(props.getCompanyName()).isEqualTo("Kronos Teste");
        assertThat(props.getUsername()).isEqualTo("kronos_teste");
        assertThat(props.getLocalStorageRoot()).isEqualTo("/opt/kronos/sandbox/kronos-teste");
        assertThat(props.getLockTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void logConfig_shouldNotThrow() {
        DemoSandboxProperties props = new DemoSandboxProperties();
        assertThatCode(props::logConfig).doesNotThrowAnyException();
    }
}
