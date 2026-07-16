package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.AddCompanyAccessRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceCoverage2Test {

    @InjectMocks private UserService service;
    @Mock private UserProvider userProvider;
    @Mock private DocumentProvider documentProvider;
    @Mock private TimeRecordProvider timeRecordProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private EmployeeUseCase employeeUseCase;
    @Mock private DomainAuthorizationService domainAuthorizationService;
    @Mock private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private AuditService auditService;
    @Mock private CacheProvider cacheProvider;
    @Mock private ClientIpResolver clientIpResolver;
    @Mock private UserCompanyAccessProvider userCompanyAccessProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private AcceptTermsUseCase acceptTermsUseCase;

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID EMP_ID    = UUID.randomUUID();
    private static final UUID COMP_ID   = UUID.randomUUID();

    private User existingUser;
    private Company activeCompany;
    private Employee employee;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        existingUser = new User(USER_ID, "john", "hashed-pw", Role.MANAGER, true, EMP_ID);
        activeCompany = new Company(COMP_ID, "Acme", "00.000.000/0001-00", "a@acme.com", true, null, null, 1, 0);
        employee = mock(Employee.class);
        lenient().when(employee.employeeId()).thenReturn(EMP_ID);
        lenient().when(employee.companyId()).thenReturn(COMP_ID);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ── updateUser L201: role==CTO AND callerRole==CTO → allowed (B4) ────────
    @Test
    void updateUser_withCtoRoleAndCallerIsCto_updatesSuccessfully() {
        when(domainAuthorizationService.authorizeUserAccess(USER_ID)).thenReturn(existingUser);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(USER_ID);

        service.updateUser(USER_ID, new UpdateUserRequest(null, null, "CTO", null));

        verify(userProvider).save(any(User.class));
    }

    // ── changeOwnPassword L323 B2T: non-null but mismatched passwords ────────
    @Test
    void changeOwnPassword_withMismatchedPasswords_throwsBadRequest() {
        when(jwtAuthenticatedUser.getuserId()).thenReturn(USER_ID);
        when(domainAuthorizationService.authorizeUserAccess(USER_ID)).thenReturn(existingUser);
        when(passwordEncoder.matches("current-pw", "hashed-pw")).thenReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest("current-pw", "NewPass1", "DifferentPass1");
        assertThrows(BadRequestException.class, () -> service.changeOwnPassword(req));
    }

    // ── addCompanyAccess L418-419: catch when audit throws ───────────────────
    @Test
    void addCompanyAccess_withAuditThrows_doesNotPropagate() {
        stubAddCompanyAccessSuccess();
        when(jwtAuthenticatedUser.getuserId()).thenReturn(USER_ID);
        // Disambiguate with anyString() for 8th param (String overload)
        doThrow(new RuntimeException("audit fail")).when(auditService).registerSecurity(
            any(AuditAction.class), nullable(UUID.class), nullable(UUID.class),
            anyString(), anyString(), nullable(String.class), anyString(), anyString(), anyString()
        );

        assertDoesNotThrow(() -> service.addCompanyAccess(USER_ID, buildAccessRequest()));
    }

    // ── currentUserIdOrNull L447-448: catch when getuserId throws ────────────
    @Test
    void addCompanyAccess_whenGetUserIdThrows_currentUserIdReturnsNull() {
        stubAddCompanyAccessSuccess();
        when(jwtAuthenticatedUser.getuserId()).thenThrow(new RuntimeException("no auth"));

        assertDoesNotThrow(() -> service.addCompanyAccess(USER_ID, buildAccessRequest()));
        verify(userCompanyAccessProvider).save(any());
    }

    // ── extractIpAndUserAgent L457+L461-462: ServletRequestAttributes + null UA
    @Test
    void addCompanyAccess_withNullUserAgentInRequest_coversExtractIpAndUA() {
        stubAddCompanyAccessSuccess();
        when(jwtAuthenticatedUser.getuserId()).thenReturn(USER_ID);
        when(clientIpResolver.resolve(any())).thenReturn("10.0.0.1");

        MockHttpServletRequest mockReq = new MockHttpServletRequest();
        // No User-Agent set → getHeader("User-Agent") returns null
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockReq));

        assertDoesNotThrow(() -> service.addCompanyAccess(USER_ID, buildAccessRequest()));
    }

    // ── listUsersScope L495+L497: MANAGER role → tenant scope uses companyId ─
    @Test
    void listUsersResponse_withManagerRole_coversNonCtoScopePath() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeCompanyAccess(null)).thenReturn(COMP_ID);
        when(cacheProvider.getOrLoad(any(), any(), eq(UserListResponse.class), any()))
            .thenReturn(new UserListResponse(List.of()));

        UserListResponse result = service.listUsersResponse(null);

        assertNotNull(result);
        verify(domainAuthorizationService).authorizeCompanyAccess(null);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubAddCompanyAccessSuccess() {
        when(userProvider.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(clientIpResolver.resolve(any())).thenReturn("test-ip");
        when(companyProvider.findById(COMP_ID)).thenReturn(Optional.of(activeCompany));
        when(employeeProvider.findById(EMP_ID)).thenReturn(Optional.of(employee));
        when(userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(USER_ID, COMP_ID)).thenReturn(false);
    }

    private AddCompanyAccessRequest buildAccessRequest() {
        return new AddCompanyAccessRequest(COMP_ID, EMP_ID, "MANAGER", false);
    }

    // ── extractIpAndUserAgent L461-462 FALSE: non-null User-Agent header ─────
    @Test
    void addCompanyAccess_withNonNullUserAgentInRequest_coversExtractIpUAFalseBranch() {
        stubAddCompanyAccessSuccess();
        when(jwtAuthenticatedUser.getuserId()).thenReturn(USER_ID);
        when(clientIpResolver.resolve(any())).thenReturn("10.0.0.1");

        MockHttpServletRequest mockReq = new MockHttpServletRequest();
        mockReq.addHeader("User-Agent", "JUnit-Test/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockReq));

        assertDoesNotThrow(() -> service.addCompanyAccess(USER_ID, buildAccessRequest()));
    }

}