package com.kts.kronos.adapter.in.web.dto.message;

import com.kts.kronos.domain.model.enuns.MessagePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateMessageRequest(@NotBlank String messageText,
                                   @NotBlank String title,
                                   @NotNull MessagePriority priority,
                                   List<UUID> recipientEmployeeIds
) {
}