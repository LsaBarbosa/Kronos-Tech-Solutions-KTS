package com.kts.kronos.adapter.in.web.dto.faq;

import com.kts.kronos.domain.model.FaqCategory;

import java.util.UUID;

public record FaqCategoryResponse(
        UUID id,
        String name
) {
    public static FaqCategoryResponse fromDomain(FaqCategory category) {
        return new FaqCategoryResponse(category.id(), category.name());
    }
}
