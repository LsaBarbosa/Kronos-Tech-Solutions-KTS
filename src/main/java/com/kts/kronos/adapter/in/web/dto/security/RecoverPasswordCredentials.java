package com.kts.kronos.adapter.in.web.dto.security;

import com.kts.kronos.domain.model.User;

import java.util.UUID;

public record RecoverPasswordCredentials(UUID userId, String username, String employeeEmail) {}