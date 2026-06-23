package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.AccessibleCompanyResponse;
import com.kts.kronos.adapter.in.web.dto.user.AddCompanyAccessRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.*;

@RestController
@RequestMapping(USER)
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase useCase;
    private final AuthCookieService authCookieService;
    private final AuthUseCase authUseCase;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @PostMapping
    @PreAuthorize(ADMINISTRATOR)
    public ResponseEntity<Void> registerUser(@Valid @RequestBody CreateUserRequest dto) {
        useCase.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping(USER_BY_USERNAME)
    @PreAuthorize(MANAGER)
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String userName) {
        var user = useCase.getUserByUsername(userName);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USER_BY_ID)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        var user = useCase.getUserById(userId);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USERS)
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO') or (hasRole('PARTNER') and #active == true)")
    public ResponseEntity<UserListResponse> allUsers(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        return ResponseEntity.ok(useCase.listUsersResponse(active));
    }

    @PatchMapping(UPDATE_USER)
    @PreAuthorize(MANAGER)
    public ResponseEntity<Void> updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest dto) {
        useCase.updateUser(userId, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(TOGGLE_ACTIVATE_USER)
    @PreAuthorize(MANAGER)
    public ResponseEntity<Void> activateUser(@PathVariable UUID userId) {
        useCase.toggleActivate(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(DELETE_USER)

    @PreAuthorize(MANAGER)
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        useCase.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(OWN_USER_PROFILE)
    public ResponseEntity<UserResponse> getOwnProfile() {
        return ResponseEntity.ok(useCase.getOwnProfileResponse());
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PutMapping(PASSWORD)
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        useCase.changeOwnPassword(req);
        // UserService.changeOwnPassword incrementa sessionVersion, invalidando o
        // JWT atual. Sem este Set-Cookie, o cliente fica com cookie HttpOnly morto
        // e qualquer request seguinte recebe 401 (não há como apagar via JS).
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.expireAccessTokenCookie().toString())
                .build();
    }

    @GetMapping(CHECK_USERNAME)
    @PreAuthorize(ADMINISTRATOR)
    public ResponseEntity<Void> checkUsernameAvailability(@RequestParam String username) {
        if (useCase.usernameExists(username)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(ME_COMPANIES)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<List<AccessibleCompanyResponse>> getMyCompanies() {
        UUID userId = jwtAuthenticatedUser.getuserId();
        List<AccessibleCompanyResponse> companies = authUseCase.getAccessibleCompanies(userId);
        return ResponseEntity.ok(companies);
    }

    @PostMapping(USER_COMPANY_ACCESS)
    @PreAuthorize(ADMINISTRATOR)
    public ResponseEntity<Void> addCompanyAccess(
            @PathVariable UUID userId,
            @Valid @RequestBody AddCompanyAccessRequest req
    ) {
        useCase.addCompanyAccess(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
