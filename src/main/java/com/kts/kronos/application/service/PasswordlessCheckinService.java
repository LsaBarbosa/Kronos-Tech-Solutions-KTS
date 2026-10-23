package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinRequest;
import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinResponse;
import com.kts.kronos.application.port.in.usecase.FaceAuthenticationUseCase;
import com.kts.kronos.application.port.in.usecase.PasswordlessCheckinUseCase;
import com.kts.kronos.application.port.in.usecase.PasswordlessTimeRecordUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordlessCheckinService implements PasswordlessCheckinUseCase {

    private static final String LOGIN_MESSAGE = "Login realizado com sucesso.";
    private static final int AUTO_LOGOUT_AFTER_SECONDS = 10;

    private final FaceAuthenticationUseCase faceAuthenticationUseCase;
    private final PasswordlessTimeRecordUseCase passwordlessTimeRecordUseCase;

    @Override
    public FaceCheckinResponse checkinFace(FaceCheckinRequest request) {
        try {
            var authenticatedFace = faceAuthenticationUseCase.authenticateFace(
                    request.faceImageBase64(),
                    request.livenessPassed()
            );
            var registrationResult = passwordlessTimeRecordUseCase.registerTimeForEmployee(
                    authenticatedFace.user().employeeId(),
                    request
            );

            log.info("event=passwordless_checkin result=success action={}", registrationResult.actionType());

            return new FaceCheckinResponse(
                    LOGIN_MESSAGE,
                    registrationResult.message(),
                    registrationResult.actionType(),
                    AUTO_LOGOUT_AFTER_SECONDS,
                    registrationResult.recordedAt()
            );
        } catch (RuntimeException exception) {
            log.warn("event=passwordless_checkin result=failure exception_type={}",
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }
}
