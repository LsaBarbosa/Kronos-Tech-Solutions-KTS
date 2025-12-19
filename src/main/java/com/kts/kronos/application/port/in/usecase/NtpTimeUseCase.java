package com.kts.kronos.application.port.in.usecase;

public interface NtpTimeUseCase {
    Long getNetworkTimeOffset();
    void validateSystemTime(int maxDriftSeconds);
}
