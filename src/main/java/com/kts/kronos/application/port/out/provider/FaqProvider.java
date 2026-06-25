package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.domain.model.FaqArticle;
import com.kts.kronos.domain.model.enuns.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqProvider {

    /**
     * Full-text search across title, shortAnswer, fullAnswer, category name, tags, screenKeys.
     * Uses pg_trgm similarity + tsvector ranking when available.
     * Only returns ACTIVE articles visible to the given role.
     * When screen is provided, articles linked to that screen are ranked first.
     */
    Page<FaqArticle> search(String query, String screen, Role role, Pageable pageable);

    /**
     * Returns ACTIVE articles for a given screen visible to the given role, ordered by priority.
     */
    List<FaqArticle> findByScreenAndRole(String screen, Role role, int limit);

    /**
     * Finds a single ACTIVE article by ID.
     */
    Optional<FaqArticle> findActiveById(UUID id);

    /**
     * Returns categories that have at least one ACTIVE FAQ accessible to the given role.
     * Each entry includes the count of accessible active FAQs in that category.
     */
    List<FaqCategoryWithCountResponse> findActiveCategories(Role role);

    /**
     * Increments the helpful or not-helpful counter for the given article.
     * Does nothing (silently) if the article does not exist or is inactive.
     *
     * @param faqId   the article ID
     * @param helpful true to increment helpful_count, false to increment not_helpful_count
     */
    void markHelpful(UUID faqId, boolean helpful);
}
