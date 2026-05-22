package com.kts.kronos.adapter.in.web.dto.lgpd;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignLgpdRequestRequest(
        @NotNull(message = "assignedToUserId é obrigatório")
        UUID assignedToUserId
) {}
