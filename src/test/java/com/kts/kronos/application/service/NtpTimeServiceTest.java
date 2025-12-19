package com.kts.kronos.application.service;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NtpTimeServiceTest {

    @Spy
    @InjectMocks
    private NtpTimeService ntpTimeService;

    @Mock
    private NTPUDPClient mockClient;

    @Test
    @DisplayName("Deve retornar o offset de tempo quando a conexão NTP for bem-sucedida")
    void shouldReturnOffsetOnSuccess() throws IOException {
        TimeInfo timeInfo = mock(TimeInfo.class);
        when(timeInfo.getOffset()).thenReturn(500L);

        doReturn(timeInfo).when(mockClient).getTime(any(InetAddress.class));
        doReturn(mockClient).when(ntpTimeService).createClient();

        // --- CORREÇÃO AQUI ---
        // Simula que a conexão está aberta, para entrar no IF do finally
        when(mockClient.isOpen()).thenReturn(true);
        // ---------------------

        Long offset = ntpTimeService.getNetworkTimeOffset();

        assertEquals(500L, offset);
        verify(mockClient).open();
        verify(mockClient).close(); // Agora vai passar!
    }

    @Test
    @DisplayName("Deve retornar null e não quebrar a aplicação quando o NTP falhar")
    void shouldReturnNullOnError() throws IOException {
        doThrow(new IOException("Timeout")).when(mockClient).getTime(any(InetAddress.class));
        doReturn(mockClient).when(ntpTimeService).createClient();

        // --- CORREÇÃO AQUI TAMBÉM ---
        when(mockClient.isOpen()).thenReturn(true);
        // ----------------------------

        Long offset = ntpTimeService.getNetworkTimeOffset();

        assertNull(offset);
        verify(mockClient).close(); // Agora vai passar!
    }
}