package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.faq.FaqArticleResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqContextualResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqSearchResponse;

import java.util.List;
import java.util.UUID;

public interface FaqUseCase {

    /**
     * Searches FAQ articles by query string, filtered by the authenticated user's role.
     * Optionally prioritizes results from a specific screen.
     * Role is NEVER accepted as a parameter; it is extracted from the security context.
     */
    FaqSearchResponse search(String query, String screen, int page, int size);

    /**
     * Returns contextual FAQ articles for a given screen, filtered by the authenticated user's role.
     * Role is NEVER accepted as a parameter; it is extracted from the security context.
     */
    FaqContextualResponse getContextual(String screen, int limit);

    /**
     * Returns a single active FAQ article by ID, validating that the authenticated user's role
     * has permission to view it.
     */
    FaqArticleResponse getById(UUID faqId);

    /**
     * Returns categories that contain at least one active FAQ accessible to the authenticated user's role.
     * Role is NEVER accepted as a parameter; it is extracted from the security context.
     */
    List<FaqCategoryWithCountResponse> getCategories();

    /**
     * Records whether the authenticated user found the given article helpful.
     * Validates: article exists, is ACTIVE, and the user's role has permission.
     *
     * @param faqId   the article ID
     * @param helpful true if helpful, false if not helpful
     */
    void markHelpful(UUID faqId, boolean helpful);
}
