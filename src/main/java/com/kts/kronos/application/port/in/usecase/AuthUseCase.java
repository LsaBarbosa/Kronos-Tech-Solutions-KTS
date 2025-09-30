package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;

public interface AuthUseCase {
    String login(String username, String password);
    void recoverPassword(RecoverPasswordRequest request,String originUrl);
    void resetPassword(ResetPasswordRequest request);
}
