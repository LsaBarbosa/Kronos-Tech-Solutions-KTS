package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(USER)
@RequiredArgsConstructor
@Tag(name = SWAGGER_USER_TAG, description = SWAGGER_USER_DESC)
public class UserController {
    private final UserUseCase useCase;

    @PostMapping
    @PreAuthorize(ADMINISTRATOR)
    @Operation(summary = CREATE_USER_SUMMARY, description = CREATE_USER_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = CREATE_USER_SUCCESS),
            @ApiResponse(responseCode = "400", description = CREATE_USER_400),
            @ApiResponse(responseCode = "404", description = CREATE_USER_404),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public void registerUser(@Valid @RequestBody CreateUserRequest dto) {
        useCase.createUser(dto);
    }

    @GetMapping(USER_BY_USERNAME)
    @PreAuthorize(MANAGER)
    @Operation(summary = GET_USER_NAME_SUMMARY, description = GET_USER_NAME_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = GET_USER_SUCCESS),
            @ApiResponse(responseCode = "404", description = GET_USER_404),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String userName) {
        var user = useCase.getUserByUsername(userName);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USER_BY_ID)
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = GET_USER_ID_SUMMARY, description = GET_USER_ID_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = GET_USER_SUCCESS),
            @ApiResponse(responseCode = "404", description = USER_NOT_FOUND),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        var user = useCase.getUserById(userId);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping(USERS)
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = LIST_USERS_SUMMARY, description = LIST_USERS_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = LIST_SUCCESS),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED),
            @ApiResponse(responseCode = "404", description = LIST_USERS_404)
    })
    public ResponseEntity<UserListResponse> allUsers(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var users = useCase.listUsers(active);
        return ResponseEntity.ok(new UserListResponse(
                users.stream().map(UserResponse::fromDomain).toList()));
    }

    @PatchMapping(UPDATE_USER)
    @Operation(summary = UPDATE_USER_SUMMARY, description = UPDATE_USER_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = UPDATE_USER_SUCCESS),
            @ApiResponse(responseCode = "400", description = UPDATE_USER_400),
            @ApiResponse(responseCode = "404", description = USER_NOT_FOUND),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public void updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest dto) {
        useCase.updateUser(userId, dto);
    }

    @PatchMapping(TOGGLE_ACTIVATE_USER)
    @PreAuthorize(MANAGER)
    @Operation(summary = ACTIVATE_USER_SUMMARY, description = ACTIVATE_USER_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = TOGGLE_COMPANY_SUCCESS),
            @ApiResponse(responseCode = "404", description = USER_NOT_FOUND),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public void activateUser(@PathVariable UUID userId) {
        useCase.toggleActivate(userId);
    }

    @DeleteMapping(DELETE_USER)
    @PreAuthorize(MANAGER)
    @Operation(summary = DELETE_USER_SUMMARY, description = DELETE_USER_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = DELETE_USER_SUCCESS),
            @ApiResponse(responseCode = "404", description = USER_NOT_FOUND),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public void deleteUser(@PathVariable UUID userId) {
        useCase.deleteUser(userId);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(OWN_USER_PROFILE)
    @Operation(summary = OWN_USER_PROFILE_SUMMARY, description = OWN_USER_PROFILE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = OWN_USER_PROFILE_SUCCESS),
            @ApiResponse(responseCode = "404", description = OWN_USER_PROFILE_404)
    })
    public ResponseEntity<UserResponse> getOwnProfile() {
        var user = useCase.getOwnProfile();
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PutMapping(PASSWORD)
    @Operation(summary = CHANGE_PASS_SUMMARY, description = CHANGE_PASS_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = CHANGE_PASS_SUCCESS),
            @ApiResponse(responseCode = "400", description = CHANGE_PASS_400),
            @ApiResponse(responseCode = "404", description = USER_NOT_FOUND)
    })
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        useCase.changeOwnPassword(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(CHECK_USERNAME)
    @Operation(summary = CHECK_USER_SUMMARY, description = CHECK_USER_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = CHECK_USER_200),
            @ApiResponse(responseCode = "404", description = CHECK_USER_404)
    })
    public ResponseEntity<Void> checkUsernameAvailability(@RequestParam String username) {
        if (useCase.usernameExists(username)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}