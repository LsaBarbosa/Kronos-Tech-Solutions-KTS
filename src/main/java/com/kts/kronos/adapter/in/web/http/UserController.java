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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase useCase;

    @PostMapping
    public ResponseEntity<Void> registerUser(@Valid @RequestBody CreateUserRequest dto) {
        useCase.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/search/username/{userName}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userName) {
        var user = useCase.getUserByUsername(userName);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping("/search/{id}")
    public ResponseEntity<UserResponse> getUserId(@PathVariable UUID id) {
        var user = useCase.getUserById(id);
        return ResponseEntity.ok(UserResponse.fromDomain(user));
    }

    @GetMapping("/search")
    public ResponseEntity<UserListResponse> allUsers(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        var users = useCase.listUsers(active);
        return ResponseEntity.ok(new UserListResponse(
                users.stream().map(UserResponse::fromDomain).toList()));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<Void> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest dto
    ) {
        useCase.updateUser(userId, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/deactivate/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        useCase.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/activate/{id}")
    public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
        useCase.activateUser(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID id) {
        useCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
