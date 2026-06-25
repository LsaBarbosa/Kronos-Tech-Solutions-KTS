package com.kts.kronos.adapter.in.web.dto.faq;

import java.util.UUID;

/**
 * Response DTO for GET /faqs/categories.
 * Returns categories that have at least one active FAQ accessible to the authenticated user's role.
 */
public record FaqCategoryWithCountResponse(
        UUID id,
        String name,
        long faqCount
) {
}
