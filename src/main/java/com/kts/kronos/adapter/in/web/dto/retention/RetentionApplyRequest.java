package com.kts.kronos.adapter.in.web.dto.retention;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record RetentionApplyRequest(
        @NotBlank(message = "justification is required")
        String justification,
        @AssertTrue(message = "confirmed must be true for APPLY")
        boolean confirmed
) {
}
