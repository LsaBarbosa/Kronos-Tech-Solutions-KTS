package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLgpdRequestStatusRequest(
        @NotNull LgpdRequestStatus status,
        String notes
) {
}
