package com.kts.kronos.adapter.in.web.dto.message;

import com.kts.kronos.domain.model.enuns.MessagePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMessageRequest(@NotBlank String messageText,
                                   @NotNull MessagePriority priority
) {
}