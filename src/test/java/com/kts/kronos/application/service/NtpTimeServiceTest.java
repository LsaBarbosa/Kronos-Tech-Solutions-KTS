package com.kts.kronos.application.service;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.SocketException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NtpTimeServiceTest {

    @Test
    @DisplayName("getNetworkTimeOffset: deve retornar offset e fechar cliente")
    void shouldReturnNetworkOffsetAndCloseClient() throws Exception {
        NTPUDPClient client = mock(NTPUDPClient.class);
        TimeInfo timeInfo = mock(TimeInfo.class);
        TestableNtpTimeService service = serviceWithClient(client);

        when(client.getTime(any(InetAddress.class))).thenReturn(timeInfo);
        when(timeInfo.getOffset()).thenReturn(1500L);
        when(client.isOpen()).thenReturn(true);

        Long offset = service.getNetworkTimeOffset();

        assertEquals(1500L, offset);
        verify(client).setDefaultTimeout(2500);
        verify(client).open();
        verify(timeInfo).computeDetails();
        verify(client).close();
    }

    @Test
    @DisplayName("getNetworkTimeOffset: deve retornar null quando consulta falhar")
    void shouldReturnNullWhenNetworkLookupFails() throws Exception {
        NTPUDPClient client = mock(NTPUDPClient.class);
        TestableNtpTimeService service = serviceWithClient(client);

        doThrow(new SocketException("timeout")).when(client).open();
        when(client.isOpen()).thenReturn(false);

        assertNull(service.getNetworkTimeOffset());
    }

    @Test
    @DisplayName("validateSystemTime: deve permitir offset ausente ou dentro do limite")
    void shouldAllowMissingOrAcceptableOffset() {
        assertDoesNotThrow(() -> new FixedOffsetNtpTimeService(null).validateSystemTime(5));
        assertDoesNotThrow(() -> new FixedOffsetNtpTimeService(4_999L).validateSystemTime(5));
        assertDoesNotThrow(() -> new FixedOffsetNtpTimeService(-4_999L).validateSystemTime(5));
    }

    @Test
    @DisplayName("validateSystemTime: deve falhar quando offset ultrapassar limite")
    void shouldRejectOffsetAboveLimit() {
        assertThrows(IllegalStateException.class, () -> new FixedOffsetNtpTimeService(5_001L).validateSystemTime(5));
        assertThrows(IllegalStateException.class, () -> new FixedOffsetNtpTimeService(-5_001L).validateSystemTime(5));
    }

    @Test
    @DisplayName("createClient: deve criar cliente NTP padrao")
    void shouldCreateDefaultNtpClient() {
        var service = new ExposedNtpTimeService();

        assertNotNull(service.newClient());
    }

    private static TestableNtpTimeService serviceWithClient(NTPUDPClient client) {
        TestableNtpTimeService service = new TestableNtpTimeService(client);
        ReflectionTestUtils.setField(service, "ntpServer", "localhost");
        ReflectionTestUtils.setField(service, "timeout", 2500);
        return service;
    }

    private static class TestableNtpTimeService extends NtpTimeService {
        private final NTPUDPClient client;

        private TestableNtpTimeService(NTPUDPClient client) {
            this.client = client;
        }

        @Override
        protected NTPUDPClient createClient() {
            return client;
        }
    }

    private static class FixedOffsetNtpTimeService extends NtpTimeService {
        private final Long offset;

        private FixedOffsetNtpTimeService(Long offset) {
            this.offset = offset;
        }

        @Override
        public Long getNetworkTimeOffset() {
            return offset;
        }
    }

    private static class ExposedNtpTimeService extends NtpTimeService {
        private NTPUDPClient newClient() {
            return super.createClient();
        }
    }
}
