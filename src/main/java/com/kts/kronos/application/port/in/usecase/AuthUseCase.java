package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;

public interface AuthUseCase {
    String login(String username, String password);
    String loginFace(String faceImageBase64);
    void recoverPassword(RecoverPasswordRequest request,String originUrl);
    void resetPassword(ResetPasswordRequest request);
}
