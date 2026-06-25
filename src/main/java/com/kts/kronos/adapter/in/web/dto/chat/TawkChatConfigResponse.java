package com.kts.kronos.adapter.in.web.dto.chat;

public record TawkChatConfigResponse(
        boolean enabled,
        String propertyId,
        String widgetId
) {}
