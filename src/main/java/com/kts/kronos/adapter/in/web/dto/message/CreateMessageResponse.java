package com.kts.kronos.adapter.in.web.dto.message;

import com.kts.kronos.domain.model.enuns.MessageScope;

import java.util.UUID;

public record CreateMessageResponse(
        UUID messageId,
        MessageScope scope,
        int deliveredCount
) {
}
