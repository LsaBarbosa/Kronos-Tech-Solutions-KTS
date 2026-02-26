package com.kts.kronos.adapter.in.web.dto.security;

import java.util.UUID;

public record AuthContext(UUID targetEmployeeId, UUID loggedEmployeeId, boolean isManagerView) {}