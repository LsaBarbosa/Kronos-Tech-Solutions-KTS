package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<Void> registerUser(@Valid @RequestBody CreateUserRequest dto) {
        useCase.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
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
    public ResponseEntity<Void> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest dto
    ) {
        useCase.updateUser(userId, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(TOGGLE_ACTIVATE_USER)
    public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
        useCase.toggleActivate(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping(DELETE_USER)
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID id) {
        useCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
