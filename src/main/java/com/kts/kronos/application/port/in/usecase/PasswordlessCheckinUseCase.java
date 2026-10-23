package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinRequest;
import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinResponse;

public interface PasswordlessCheckinUseCase {
    FaceCheckinResponse checkinFace(FaceCheckinRequest request);
}
