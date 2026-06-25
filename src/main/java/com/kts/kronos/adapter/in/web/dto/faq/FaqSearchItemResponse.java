package com.kts.kronos.adapter.in.web.dto.faq;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.FaqArticle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FaqSearchItemResponse(
        UUID id,
        String title,
        String shortAnswer,
        FaqCategoryResponse category,
        List<String> tags,
        List<String> relatedScreens,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,
        /**
         * Relevance score from pg_trgm / ts_rank. Present only in search results;
         * null for contextual and detail responses.
         */
        Double relevanceScore
) {
    public static FaqSearchItemResponse fromDomain(FaqArticle article) {
        return new FaqSearchItemResponse(
                article.id(),
                article.title(),
                article.shortAnswer(),
                article.category() != null ? FaqCategoryResponse.fromDomain(article.category()) : null,
                article.tags(),
                article.screenKeys(),
                article.updatedAt(),
                article.relevanceScore()
        );
    }
}
