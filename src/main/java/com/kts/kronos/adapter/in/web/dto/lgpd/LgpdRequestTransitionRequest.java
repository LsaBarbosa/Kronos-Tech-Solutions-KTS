package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import jakarta.validation.constraints.NotNull;

public record LgpdRequestTransitionRequest(
        @NotNull(message = "Status é obrigatório")
        LgpdRequestStatus newStatus,

        String publicNotes,

        String internalNotes,

        String closedReason
) {}
