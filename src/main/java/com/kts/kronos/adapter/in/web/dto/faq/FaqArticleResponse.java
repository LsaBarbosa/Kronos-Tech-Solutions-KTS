package com.kts.kronos.adapter.in.web.dto.faq;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.FaqArticle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FaqArticleResponse(
        UUID id,
        String title,
        String shortAnswer,
        String fullAnswer,
        FaqCategoryResponse category,
        List<String> tags,
        List<String> relatedScreens,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static FaqArticleResponse fromDomain(FaqArticle article) {
        return new FaqArticleResponse(
                article.id(),
                article.title(),
                article.shortAnswer(),
                article.fullAnswer(),
                article.category() != null ? FaqCategoryResponse.fromDomain(article.category()) : null,
                article.tags(),
                article.screenKeys(),
                article.updatedAt()
        );
    }
}
