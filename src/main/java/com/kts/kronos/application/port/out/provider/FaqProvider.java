package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.FaqArticle;
import com.kts.kronos.domain.model.enuns.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqProvider {

    /**
     * Full-text/ILIKE search across title, shortAnswer, fullAnswer, category name, tags, screenKeys.
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
}
