package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.chat.TawkChatConfigResponse;
import com.kts.kronos.adapter.in.web.dto.chat.TawkIdentityResponse;

public interface SupportChatUseCase {
    TawkChatConfigResponse getConfig();
    TawkIdentityResponse getIdentity();
    void handleWebhook(String rawPayload, String signatureHeader);
}
