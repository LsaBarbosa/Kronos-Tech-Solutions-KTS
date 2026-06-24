package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.adapter.out.persistence.FaqArticleRepository;
import com.kts.kronos.application.port.out.provider.FaqProvider;
import com.kts.kronos.domain.model.FaqArticle;
import com.kts.kronos.domain.model.enuns.FaqStatus;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FaqProviderImpl implements FaqProvider {

    private final FaqArticleRepository repository;

    /**
     * Full-text search using PostgreSQL pg_trgm + tsvector.
     * Screen-aware ranking is handled inside the native SQL query — no in-memory re-sort needed.
     * Each result is enriched with its relevanceScore computed by the DB engine.
     */
    @Override
    public Page<FaqArticle> search(String query, String screen, Role role, Pageable pageable) {
        var safeScreen = (screen != null && !screen.isBlank()) ? screen : "";

        var resultPage = repository.searchActiveFullText(
                query,
                safeScreen,
                role.name(),
                FaqStatus.ACTIVE.name(),
                pageable
        );

        var items = resultPage.getContent().stream()
                .map(entity -> {
                    Double score = repository.computeRelevanceScore(entity.getId(), query);
                    return entity.toDomainWithScore(score);
                })
                .toList();

        return new PageImpl<>(items, pageable, resultPage.getTotalElements());
    }

    @Override
    public List<FaqArticle> findByScreenAndRole(String screen, Role role, int limit) {
        var pageRequest = PageRequest.of(0, limit);
        return repository
                .findByScreenAndRoleActive(screen, role, FaqStatus.ACTIVE, pageRequest)
                .stream()
                .map(e -> e.toDomain())
                .toList();
    }

    @Override
    public Optional<FaqArticle> findActiveById(UUID id) {
        return repository
                .findByIdAndStatus(id, FaqStatus.ACTIVE)
                .map(e -> e.toDomain());
    }

    @Override
    public List<FaqCategoryWithCountResponse> findActiveCategories(Role role) {
        return repository.findActiveCategoriesForRole(role, FaqStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void markHelpful(UUID faqId, boolean helpful) {
        if (helpful) {
            repository.incrementHelpfulCount(faqId);
        } else {
            repository.incrementNotHelpfulCount(faqId);
        }
    }
}
