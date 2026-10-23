package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.chat.TawkChatConfigResponse;
import com.kts.kronos.adapter.in.web.dto.chat.TawkIdentityResponse;
import com.kts.kronos.application.port.in.usecase.SupportChatUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;

@RestController
@RequestMapping(SUPPORT_CHAT)
@RequiredArgsConstructor
public class SupportChatController {

    private final SupportChatUseCase useCase;

    /**
     * GET /support/chat/config — public, no authentication required.
     * Returns enabled flag and public widget identifiers only.
     * Never exposes secureKey or webhookSecret.
     */
    @GetMapping(SUPPORT_CHAT_CONFIG)
    public ResponseEntity<TawkChatConfigResponse> config() {
        return ResponseEntity.ok(useCase.getConfig());
    }

    /**
     * GET /support/chat/identity — authenticated users only.
     * Returns user info + HMAC hash for Tawk.to Secure Mode.
     */
    @GetMapping(SUPPORT_CHAT_IDENTITY)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<TawkIdentityResponse> identity() {
        return ResponseEntity.ok(useCase.getIdentity());
    }

    /**
     * POST /support/chat/webhook — unauthenticated, called by Tawk.to.
     * Validates HMAC-SHA256 signature before processing.
     */
    @PostMapping(value = SUPPORT_CHAT_WEBHOOK, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> webhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Tawk-Signature", required = false) String signature
    ) {
        useCase.handleWebhook(rawPayload, signature);
        return ResponseEntity.ok().build();
    }
}
