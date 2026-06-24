package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.FaqArticleRepository;
import com.kts.kronos.adapter.out.persistence.entity.FaqArticleEntity;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FaqProviderImpl implements FaqProvider {

    private final FaqArticleRepository repository;

    @Override
    public Page<FaqArticle> search(String query, String screen, Role role, Pageable pageable) {
        var resultPage = repository.searchActive(query, role, FaqStatus.ACTIVE, pageable);

        // When screen is provided, re-sort within the page to put screen-matched articles first.
        // The DB already orders by priority/updatedAt; this is a stable secondary sort in memory.
        if (screen != null && !screen.isBlank()) {
            final String screenKey = screen;
            var reordered = resultPage.getContent().stream()
                    .sorted(Comparator
                            .<FaqArticleEntity, Integer>comparing(
                                    a -> a.getScreenKeys() != null && a.getScreenKeys().contains(screenKey) ? 0 : 1)
                            .thenComparingInt(FaqArticleEntity::getPriority)
                            .thenComparing(Comparator.comparing(
                                    a -> a.getUpdatedAt() != null ? a.getUpdatedAt() : java.time.LocalDateTime.MIN,
                                    Comparator.reverseOrder()))
                    )
                    .map(FaqArticleEntity::toDomain)
                    .toList();
            return new PageImpl<>(reordered, pageable, resultPage.getTotalElements());
        }

        return resultPage.map(FaqArticleEntity::toDomain);
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
}
