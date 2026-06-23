package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.AccessibleCompanyResponse;

import java.util.List;
import java.util.UUID;

public interface AuthUseCase {
    String login(String username, String password);
    String loginFace(String faceImageBase64, Boolean livenessPassed);
    void recoverPassword(RecoverPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void logout(String rawToken);
    String refreshToken(String expiredToken);
    String switchCompany(UUID userId, UUID targetCompanyId);
    List<AccessibleCompanyResponse> getAccessibleCompanies(UUID userId);
}
