package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.domain.model.User;

import java.util.List;
import java.util.UUID;

public interface UserUseCase {
    void createUser(CreateUserRequest req);
    User getUserByUsername(String username);
    User getUserById(UUID userId);
    UserResponse getOwnProfileResponse();
    User getOwnProfile();
    UserListResponse listUsersResponse(Boolean active);
    List<User> listUsers(Boolean enabled);
    void updateUser(UUID userId, UpdateUserRequest req);
    void deleteUser(UUID userId);
    void toggleActivate(UUID userId);
    void changeOwnPassword(ChangePasswordRequest req);
    boolean usernameExists(String username);
}
