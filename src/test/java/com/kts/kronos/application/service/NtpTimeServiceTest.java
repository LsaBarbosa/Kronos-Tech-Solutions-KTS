package com.kts.kronos.application.service;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NtpTimeServiceTest {

    @Test
    void getNetworkTimeOffsetShouldReturnOffsetAndCloseClient() throws Exception {
        NtpTimeService service = Mockito.spy(new NtpTimeService());
        NTPUDPClient client = mock(NTPUDPClient.class);
        TimeInfo info = mock(TimeInfo.class);
        InetAddress host = mock(InetAddress.class);

        doReturn(client).when(service).createClient();
        when(client.getTime(host)).thenReturn(info);
        when(info.getOffset()).thenReturn(1234L);
        when(client.isOpen()).thenReturn(true);

        try (MockedStatic<InetAddress> inetAddressMock = mockStatic(InetAddress.class)) {
            inetAddressMock.when(() -> InetAddress.getByName((String) null)).thenReturn(host);

            Long offset = service.getNetworkTimeOffset();

            assertEquals(1234L, offset);
            verify(client).open();
            verify(client).setSoTimeout(any());
            verify(info).computeDetails();
            verify(client).close();
        }
    }

    @Test
    void getNetworkTimeOffsetShouldReturnNullWhenIOExceptionOccursAndCloseClient() throws Exception {
        NtpTimeService service = Mockito.spy(new NtpTimeService());
        NTPUDPClient client = mock(NTPUDPClient.class);
        InetAddress host = mock(InetAddress.class);

        doReturn(client).when(service).createClient();
        when(client.isOpen()).thenReturn(true);

        try (MockedStatic<InetAddress> inetAddressMock = mockStatic(InetAddress.class)) {
            inetAddressMock.when(() -> InetAddress.getByName((String) null)).thenReturn(host);
            doThrow(new IOException("timeout")).when(client).getTime(host);

            Long offset = service.getNetworkTimeOffset();

            assertNull(offset);
            verify(client).open();
            verify(client).close();
        }

    }

    @Test
    void validateSystemTimeThrowsWhenOffsetExceedsLimit() {
        NtpTimeService service = Mockito.spy(new NtpTimeService());
        doReturn(20_000L).when(service).getNetworkTimeOffset();

        assertThrows(IllegalStateException.class, () -> service.validateSystemTime(5));
    }

    @Test
    void validateSystemTimeDoesNothingWhenOffsetIsNull() {
        NtpTimeService service = Mockito.spy(new NtpTimeService());
        doReturn(null).when(service).getNetworkTimeOffset();

        assertDoesNotThrow(() -> service.validateSystemTime(5));
    }

    @Test
    void validateSystemTimeDoesNothingWhenOffsetWithinLimit() {
        NtpTimeService service = Mockito.spy(new NtpTimeService());
        doReturn(3000L).when(service).getNetworkTimeOffset();

        assertDoesNotThrow(() -> service.validateSystemTime(5));
    }
}
