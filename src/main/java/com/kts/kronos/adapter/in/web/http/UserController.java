package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserSearchItemResponse;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.infrastructure.redis.RedisCacheNames;
import com.kts.kronos.infrastructure.redis.RedisScopeKeyResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.function.Supplier;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.*;

@RestController
@RequestMapping(USER)
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase useCase;
    private final AuthCookieService authCookieService;
    private final AcceptTermsUseCase acceptTermsUseCase;

    @Autowired(required = false)
    private CacheProvider cacheProvider;

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
        return ResponseEntity.ok(cache(
                RedisCacheNames.USER_LIST,
                RedisScopeKeyResolver.authenticatedScope("active=" + active),
                UserListResponse.class,
                () -> buildUserList(active)
        ));
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
        return ResponseEntity.ok(cache(
                RedisCacheNames.USER_OWN_PROFILE,
                RedisScopeKeyResolver.authenticatedScope(),
                UserResponse.class,
                () -> UserResponse.fromDomain(useCase.getOwnProfile())
        ));
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

    private UserListResponse buildUserList(Boolean active) {
        var users = useCase.listUsers(active);
        var items = users.stream()
                .map(user -> {
                    boolean biometricAccepted = user.employeeId() != null
                            && acceptTermsUseCase.hasAcceptedBiometricTerm(user.employeeId());
                    return UserSearchItemResponse.fromDomain(user, biometricAccepted);
                })
                .toList();
        return new UserListResponse(items);
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, Supplier<T> loader) {
        if (cacheProvider == null) {
            return loader.get();
        }
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }
}
