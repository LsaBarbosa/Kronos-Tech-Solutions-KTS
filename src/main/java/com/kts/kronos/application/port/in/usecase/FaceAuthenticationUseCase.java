package com.kts.kronos.application.port.in.usecase;

public interface FaceAuthenticationUseCase {
    FaceAuthenticationResult authenticateFace(String faceImageBase64, Boolean livenessPassed);
}
