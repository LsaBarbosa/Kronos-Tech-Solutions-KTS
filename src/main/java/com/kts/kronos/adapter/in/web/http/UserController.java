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
import org.springframework.web.bind.annotation.*;

import static com.kts.kronos.constants.ApiPaths.USER;
import static com.kts.kronos.constants.ApiPaths.USERS;
import static com.kts.kronos.constants.ApiPaths.USER_BY_USERNAME;
import static com.kts.kronos.constants.ApiPaths.USER_BY_ID;
import static com.kts.kronos.constants.ApiPaths.UPDATE_USER;
import static com.kts.kronos.constants.ApiPaths.TOGGLE_ACTIVATE_USER;
import static com.kts.kronos.constants.ApiPaths.DELETE_USER;


import java.util.UUID;

@RestController
@RequestMapping(USER)
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void registerUser(@Valid @RequestBody CreateUserRequest dto) {
        useCase.createUser(dto);
    }

    @GetMapping(USER_BY_USERNAME)
    public ResponseEntity<UserResponse> getUser(@PathVariable String userName) {
        var user = useCase.getUserByUsername(userName);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USER_BY_ID)
    public ResponseEntity<UserResponse> getUserId(@PathVariable UUID userId) {
        var user = useCase.getUserById(userId);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USERS)
    public ResponseEntity<UserListResponse> allUsers(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var users = useCase.listUsers(active);
        return ResponseEntity.ok(new UserListResponse(
                users.stream().map(UserResponse::fromDomain).toList()));
    }

    @PatchMapping(UPDATE_USER)
    @ResponseStatus(HttpStatus.OK)
    public void updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest dto) {
        useCase.updateUser(userId, dto);
    }

    @PatchMapping(TOGGLE_ACTIVATE_USER)
    @ResponseStatus(HttpStatus.OK)
    public void activateUser(@PathVariable UUID id) {
        useCase.toggleActivate(id);
    }
    @DeleteMapping(DELETE_USER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(@PathVariable UUID id) {
        useCase.deleteUser(id);
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(@PathVariable UUID userId,
                                               @RequestBody ChangePasswordRequest req) {
        useCase.changeOwnPassword(userId, req);
        return ResponseEntity.noContent().build();
    }
}
