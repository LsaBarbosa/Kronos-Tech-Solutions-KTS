package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinRequest;

import java.util.UUID;

public interface PasswordlessTimeRecordUseCase {
    TimeRecordRegistrationResult registerTimeForEmployee(UUID employeeId, FaceCheckinRequest request);
}
