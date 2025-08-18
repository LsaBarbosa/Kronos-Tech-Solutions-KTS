package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;

@RestController
@RequestMapping(USER)
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public void registerUser(@Valid @RequestBody CreateUserRequest dto) {
        useCase.createUser(dto);
    }

    @GetMapping(USER_BY_USERNAME)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userName) {
        var user = useCase.getUserByUsername(userName);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USER_BY_ID)
    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
    public ResponseEntity<UserResponse> getUserId(@RequestParam(required = false) UUID userId) {
        var user = useCase.getUserById(userId);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USERS)
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserListResponse> allUsers(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var users = useCase.listUsers(active);
        return ResponseEntity.ok(new UserListResponse(
                users.stream().map(UserResponse::fromDomain).toList()));
    }

    @PatchMapping(UPDATE_USER)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('MANAGER')")
    public void updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest dto) {
        useCase.updateUser(userId, dto);
    }

    @PatchMapping(TOGGLE_ACTIVATE_USER)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('MANAGER')")
    public void activateUser(@PathVariable UUID id) {
        useCase.toggleActivate(id);
    }

    @DeleteMapping(DELETE_USER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteCompany(@PathVariable UUID id) {
        useCase.deleteUser(id);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
    @PutMapping(PASSWORD)
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        useCase.changeOwnPassword(req);
        return ResponseEntity.noContent().build();
    }
}
