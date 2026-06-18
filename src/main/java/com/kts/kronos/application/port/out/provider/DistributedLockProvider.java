package com.kts.kronos.application.port.out.provider;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DistributedLockProvider {
    Optional<String> acquireCheckinLock(UUID employeeId, LocalDate date);

    boolean releaseCheckinLock(UUID employeeId, LocalDate date, String ownerToken);
}
