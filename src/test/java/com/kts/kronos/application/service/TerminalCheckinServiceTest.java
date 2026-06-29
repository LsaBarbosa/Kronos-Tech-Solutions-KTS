package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.ActionResponse;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.TerminalCheckinUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.UserCompanyAccessProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TerminalCheckinServiceTest {

    @Mock private BiometricProtectionService biometricProtectionService;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;
    @Mock private UserProvider userProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private AcceptTermsUseCase acceptTermsUseCase;
    @Mock private TimeRecordService timeRecordService;
    @Mock private JwtUtils jwtUtils;
    @Mock private UserCompanyAccessProvider userCompanyAccessProvider;
    @Mock private AuditService auditService;
    @Mock private AuditRequestContextService auditRequestContextService;

    @InjectMocks
    private TerminalCheckinService service;

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String VALID_BASE64 = Base64.getEncoder().encodeToString("fakeImageBytes".getBytes());
    private static final double LAT = -23.5505;
    private static final double LON = -46.6333;

    private User activeUser;
    private BiometricConsentStatus acceptedConsent;
    private TerminalCheckinRequest validRequest;

    @BeforeEach
    void setUp() {
        activeUser = new User(USER_ID, "joao.silva", "hash", Role.PARTNER, true, EMPLOYEE_ID);
        acceptedConsent = new BiometricConsentStatus(true, "1.0", "hash", "1.0", "hash", false);
        validRequest = new TerminalCheckinRequest(VALID_BASE64, false, LAT, LON);

        when(auditRequestContextService.extractContext())
                .thenReturn(new AuditRequestContextService.AuditRequestContext("127.0.0.1", "TestAgent", "DIRECT", true));
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(userCompanyAccessProvider.findActiveByUserId(USER_ID))
                .thenReturn(List.of());
        when(jwtUtils.generateToken(any(UUID.class), any(String.class), any(String.class),
                any(UUID.class), any(BiometricConsentStatus.class), anyLong(), any()))
                .thenReturn("mocked-jwt-token");
    }

    @Test
    @DisplayName("Checkin por terminal — sucesso CHECKIN")
    void checkinByFace_success_checkin() {
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(activeUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(acceptedConsent);
        when(timeRecordService.registerTimeForEmployee(any(UUID.class), any()))
                .thenReturn(new ActionResponse("Entrada às 08:00!", "CHECKIN"));

        var result = service.checkinByFace(validRequest);

        assertNotNull(result);
        assertEquals("CHECKIN", result.checkinResponse().actionType());
        assertEquals("mocked-jwt-token", result.jwtToken());

        // Garante que Rekognition foi chamado exatamente UMA vez
        verify(faceRecognitionProvider, times(1)).searchFaceByImage(any(InputStream.class));
    }

    @Test
    @DisplayName("Checkin por terminal — sucesso CHECKOUT")
    void checkinByFace_success_checkout() {
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(activeUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(acceptedConsent);
        when(timeRecordService.registerTimeForEmployee(any(UUID.class), any()))
                .thenReturn(new ActionResponse("Saída às 17:00!", "CHECKOUT"));

        var result = service.checkinByFace(validRequest);

        assertEquals("CHECKOUT", result.checkinResponse().actionType());
        verify(faceRecognitionProvider, times(1)).searchFaceByImage(any(InputStream.class));
    }

    @Test
    @DisplayName("Face não reconhecida — lança ForbiddenException")
    void checkinByFace_faceNotRecognized_throwsForbidden() {
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(null);

        assertThrows(ForbiddenException.class, () -> service.checkinByFace(validRequest));

        verify(userProvider, never()).findByEmployeeId(any());
        verify(timeRecordService, never()).registerTimeForEmployee(any(), any());
    }

    @Test
    @DisplayName("Usuário inativo — lança BadRequestException")
    void checkinByFace_inactiveUser_throwsBadRequest() {
        var inactiveUser = new User(USER_ID, "joao.silva", "hash", Role.PARTNER, false, EMPLOYEE_ID);
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(inactiveUser));

        assertThrows(BadRequestException.class, () -> service.checkinByFace(validRequest));

        verify(timeRecordService, never()).registerTimeForEmployee(any(), any());
    }

    @Test
    @DisplayName("Sem usuário vinculado ao colaborador — lança ResourceNotFoundException")
    void checkinByFace_noUserLinked_throwsResourceNotFound() {
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.checkinByFace(validRequest));
    }

    @Test
    @DisplayName("Consentimento biométrico ausente — lança TermsNotAcceptedException")
    void checkinByFace_noConsent_throwsTermsNotAccepted() {
        var noConsent = new BiometricConsentStatus(false, null, null, null, null, true);
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(activeUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(noConsent);

        assertThrows(TermsNotAcceptedException.class, () -> service.checkinByFace(validRequest));

        verify(timeRecordService, never()).registerTimeForEmployee(any(), any());
    }

    @Test
    @DisplayName("Rate limit excedido — lança TooManyRequestsException propagada")
    void checkinByFace_rateLimitExceeded_propagates() {
        doThrow(new com.kts.kronos.application.exceptions.TooManyRequestsException("Rate limit"))
                .when(biometricProtectionService).protectTerminalCheckin(any(), any());

        assertThrows(com.kts.kronos.application.exceptions.TooManyRequestsException.class,
                () -> service.checkinByFace(validRequest));

        verify(faceRecognitionProvider, never()).searchFaceByImage(any());
    }

    @Test
    @DisplayName("Base64 inválido — lança BadRequestException")
    void checkinByFace_invalidBase64_throwsBadRequest() {
        var badRequest = new TerminalCheckinRequest("NOT_VALID_BASE64!!!", false, LAT, LON);
        doNothing().when(biometricProtectionService).protectTerminalCheckin(any(), any());

        assertThrows(BadRequestException.class, () -> service.checkinByFace(badRequest));

        verify(faceRecognitionProvider, never()).searchFaceByImage(any());
    }

    @Test
    @DisplayName("Rekognition chamado exatamente 1 vez — não duplica chamada")
    void checkinByFace_rekognitionCalledOnce() {
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(activeUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(acceptedConsent);
        when(timeRecordService.registerTimeForEmployee(any(UUID.class), any()))
                .thenReturn(new ActionResponse("Entrada às 08:00!", "CHECKIN"));

        service.checkinByFace(validRequest);

        verify(faceRecognitionProvider, times(1)).searchFaceByImage(any(InputStream.class));
    }
}
