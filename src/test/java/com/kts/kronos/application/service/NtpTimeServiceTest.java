package com.kts.kronos.application.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NtpTimeServiceTest {

    @Test
    void validateSystemTimeThrowsWhenOffsetExceedsLimit() {
        NtpTimeService service = Mockito.spy(new NtpTimeService());
        Mockito.doReturn(20_000L).when(service).getNetworkTimeOffset();

        assertThrows(IllegalStateException.class, () -> service.validateSystemTime(5));
    }

    @Test
    void validateSystemTimeDoesNothingWhenOffsetIsNull() {
        NtpTimeService service = Mockito.spy(new NtpTimeService());
        Mockito.doReturn(null).when(service).getNetworkTimeOffset();

        assertDoesNotThrow(() -> service.validateSystemTime(5));
    }
}
