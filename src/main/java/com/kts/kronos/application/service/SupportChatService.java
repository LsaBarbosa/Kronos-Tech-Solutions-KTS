package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.chat.TawkChatConfigResponse;
import com.kts.kronos.adapter.in.web.dto.chat.TawkIdentityResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.SupportChatUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.config.chat.TawkChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportChatService implements SupportChatUseCase {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final TawkChatProperties props;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeProvider employeeProvider;

    @Override
    public TawkChatConfigResponse getConfig() {
        if (!props.isEnabled()) {
            return new TawkChatConfigResponse(false, "", "");
        }
        return new TawkChatConfigResponse(true, props.getPropertyId(), props.getWidgetId());
    }

    @Override
    public TawkIdentityResponse getIdentity() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var userId = jwtAuthenticatedUser.getuserId();

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException("Colaborador não encontrado para geração de identidade do chat."));

        var hash = hmacSha256Hex(props.getSecureKey(), userId.toString());

        return new TawkIdentityResponse(
                userId.toString(),
                employee.fullName(),
                employee.email(),
                hash,
                props.getIdentityTtlSeconds()
        );
    }

    @Override
    public void handleWebhook(String rawPayload, String signatureHeader) {
        if (rawPayload == null || rawPayload.length() > props.getMaxWebhookPayloadCharacters()) {
            log.warn("event=tawk_webhook_rejected reason=payload_size_exceeded length={}", rawPayload == null ? 0 : rawPayload.length());
            throw new ForbiddenException("Payload excede o limite permitido.");
        }

        if (!isValidWebhookSignature(rawPayload, signatureHeader)) {
            log.warn("event=tawk_webhook_rejected reason=invalid_signature");
            throw new ForbiddenException("Assinatura do webhook inválida.");
        }

        log.info("event=tawk_webhook_received payload_length={}", rawPayload.length());
    }

    private boolean isValidWebhookSignature(String payload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            // Tawk.to sends: sha256=<hex_digest>
            String prefix = "sha256=";
            String receivedHex = signatureHeader.startsWith(prefix)
                    ? signatureHeader.substring(prefix.length())
                    : signatureHeader;

            String expectedHex = hmacSha256Hex(props.getWebhookSecret(), payload);
            return constantTimeEquals(expectedHex, receivedHex);
        } catch (Exception e) {
            log.warn("event=tawk_webhook_signature_error error={}", e.getMessage());
            return false;
        }
    }

    private String hmacSha256Hex(String key, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC-SHA256.", e);
        }
    }

    // Prevent timing attacks
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
