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
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;
import static com.kts.kronos.constants.Messages.MANAGER;

@RestController
@RequestMapping(USER)
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGER)
    public void registerUser(@Valid @RequestBody CreateUserRequest dto) {
        useCase.createUser(dto);
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
    @PreAuthorize(MANAGER)
    public ResponseEntity<UserListResponse> allUsers(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var users = useCase.listUsers(active);
        return ResponseEntity.ok(new UserListResponse(
                users.stream().map(UserResponse::fromDomain).toList()));
    }

    @PatchMapping(UPDATE_USER)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize(MANAGER)
    public void updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest dto) {
        useCase.updateUser(userId, dto);
    }

    @PatchMapping(TOGGLE_ACTIVATE_USER)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize(MANAGER)
    public void activateUser(@PathVariable UUID userId) {
        useCase.toggleActivate(userId);
    }

    @DeleteMapping(DELETE_USER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGER)
    public void deleteUser(@PathVariable UUID userId) {
        useCase.deleteUser(userId);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(OWN_USER_PROFILE)
    public ResponseEntity<UserResponse> getOwnProfile() {
        var user = useCase.getOwnProfile();
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PutMapping(PASSWORD)
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        useCase.changeOwnPassword(req);
        return ResponseEntity.noContent().build();
    }
}