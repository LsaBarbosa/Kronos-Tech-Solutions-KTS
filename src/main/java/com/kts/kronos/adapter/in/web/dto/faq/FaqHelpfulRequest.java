package com.kts.kronos.adapter.in.web.dto.faq;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /faqs/{faqId}/helpful.
 * Records whether the authenticated user found the article helpful.
 */
public record FaqHelpfulRequest(
        @NotNull(message = "O campo 'helpful' é obrigatório.")
        Boolean helpful
) {
}
