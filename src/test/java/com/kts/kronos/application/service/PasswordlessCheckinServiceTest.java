package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinRequest;
import com.kts.kronos.application.port.in.usecase.FaceAuthenticationResult;
import com.kts.kronos.application.port.in.usecase.FaceAuthenticationUseCase;
import com.kts.kronos.application.port.in.usecase.PasswordlessTimeRecordUseCase;
import com.kts.kronos.application.port.in.usecase.TimeRecordRegistrationResult;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordlessCheckinServiceTest {

    @InjectMocks
    private PasswordlessCheckinService service;

    @Mock
    private FaceAuthenticationUseCase faceAuthenticationUseCase;

    @Mock
    private PasswordlessTimeRecordUseCase passwordlessTimeRecordUseCase;

    @Test
    @DisplayName("Deve autenticar a face e registrar o ponto no mesmo fluxo")
    void shouldAuthenticateFaceAndRegisterTime() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        FaceCheckinRequest request = new FaceCheckinRequest("base64-image", -22.9, -43.2, 18.5, true);
        User user = new User(userId, "terminal@kts.com", "hash", Role.MANAGER, true, employeeId);
        var consentStatus = new BiometricConsentStatus(true, "v1", "hash", "v1", "hash", false);
        var recordedAt = OffsetDateTime.parse("2026-06-26T08:01:00-03:00");

        when(faceAuthenticationUseCase.authenticateFace("base64-image", true))
                .thenReturn(new FaceAuthenticationResult(user, consentStatus, companyId));
        when(passwordlessTimeRecordUseCase.registerTimeForEmployee(eq(employeeId), eq(request)))
                .thenReturn(new TimeRecordRegistrationResult(
                        "Entrada às 08:01! (NSR: 123)",
                        "CHECKIN",
                        recordedAt
                ));

        var response = service.checkinFace(request);

        assertEquals("Login realizado com sucesso.", response.loginMessage());
        assertEquals("Entrada às 08:01! (NSR: 123)", response.recordMessage());
        assertEquals("CHECKIN", response.actionType());
        assertEquals(10, response.autoLogoutAfterSeconds());
        assertEquals(recordedAt, response.recordedAt());

        verify(faceAuthenticationUseCase).authenticateFace("base64-image", true);
        verify(passwordlessTimeRecordUseCase).registerTimeForEmployee(employeeId, request);
    }
}
