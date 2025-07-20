package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(USER)
@RequiredArgsConstructor
@Tag(name = USER_API, description = USER_DESCRIPTION_API)
public class UserController {
    private final UserUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = CREATE, description = CREATE_DESCRIPTION)
    public void registerUser(@Valid @RequestBody CreateUserRequest dto) {
        useCase.createUser(dto);
    }

    @GetMapping(USER_BY_USERNAME)
    @Operation(summary = GET_BY_PARAMETER, description = USER_DESCRIPTION)
    public ResponseEntity<UserResponse> getUser(@PathVariable String userName) {
        var user = useCase.getUserByUsername(userName);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USER_BY_ID)
    @Operation(summary = GET_BY_PARAMETER, description = USER_DESCRIPTION_ID)
    public ResponseEntity<UserResponse> getUserId(@PathVariable UUID userId) {
        var user = useCase.getUserById(userId);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USERS)
    @Operation(summary = GET_ALL, description = ALL_OBJECTS_DESCRIPTION)
    public ResponseEntity<UserListResponse> allUsers(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var users = useCase.listUsers(active);
        return ResponseEntity.ok(new UserListResponse(
                users.stream().map(UserResponse::fromDomain).toList()));
    }

    @PatchMapping(UPDATE_USER)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = UPDATE, description = UPDATE_DESCRIPTION)
    public void updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest dto) {
        useCase.updateUser(userId, dto);
    }

    @PatchMapping(TOGGLE_ACTIVATE_USER)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = TOGGLE, description = TOGGLE_DESCRIPTION)
    public void activateUser(@PathVariable UUID id) {
        useCase.toggleActivate(id);
    }
    @DeleteMapping(DELETE_USER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = DELETE, description = DELETE_USER_DESCRIPTION)
    public void deleteCompany(@PathVariable UUID id) {
        useCase.deleteUser(id);
    }

}
