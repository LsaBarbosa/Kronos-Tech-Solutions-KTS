package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.adapter.out.persistence.FaqArticleRepository;
import com.kts.kronos.adapter.out.persistence.entity.FaqArticleEntity;
import com.kts.kronos.domain.model.FaqArticle;
import com.kts.kronos.domain.model.enuns.FaqStatus;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaqProviderImplTest {

    @Mock
    private FaqArticleRepository repository;

    @InjectMocks
    private FaqProviderImpl provider;

    private FaqArticleEntity mockEntityWithDomain(FaqArticle domain) {
        FaqArticleEntity entity = mock(FaqArticleEntity.class);
        when(entity.toDomain()).thenReturn(domain);
        when(entity.getId()).thenReturn(UUID.randomUUID());
        return entity;
    }

    @Test
    void shouldSearchWithScore() {
        FaqArticleEntity entity = mock(FaqArticleEntity.class);
        FaqArticle domain = mock(FaqArticle.class);
        UUID entityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(entity.getId()).thenReturn(entityId);
        when(entity.toDomainWithScore(any())).thenReturn(domain);
        Page<FaqArticleEntity> entityPage = new PageImpl<>(List.of(entity));
        when(repository.searchActiveFullText(eq("test"), eq("dashboard"), eq("MANAGER"),
                eq("ACTIVE"), eq(pageable))).thenReturn(entityPage);
        when(repository.computeRelevanceScore(entityId, "test")).thenReturn(0.9);

        Page<FaqArticle> result = provider.search("test", "dashboard", Role.MANAGER, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldSearchWithNullScreen() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FaqArticleEntity> emptyPage = new PageImpl<>(List.of());
        when(repository.searchActiveFullText(eq("test"), eq(""), eq("MANAGER"),
                eq("ACTIVE"), eq(pageable))).thenReturn(emptyPage);

        Page<FaqArticle> result = provider.search("test", null, Role.MANAGER, pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldSearchWithBlankScreen() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FaqArticleEntity> emptyPage = new PageImpl<>(List.of());
        when(repository.searchActiveFullText(eq("test"), eq(""), eq("MANAGER"),
                eq("ACTIVE"), eq(pageable))).thenReturn(emptyPage);

        Page<FaqArticle> result = provider.search("test", "   ", Role.MANAGER, pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldFindByScreenAndRole() {
        FaqArticle domain = mock(FaqArticle.class);
        FaqArticleEntity entity = mock(FaqArticleEntity.class);
        when(entity.toDomain()).thenReturn(domain);
        Pageable pageRequest = PageRequest.of(0, 5);
        when(repository.findByScreenAndRoleActive(eq("home"), eq(Role.MANAGER),
                eq(FaqStatus.ACTIVE), any())).thenReturn(List.of(entity));

        List<FaqArticle> result = provider.findByScreenAndRole("home", Role.MANAGER, 5);

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindActiveById() {
        UUID id = UUID.randomUUID();
        FaqArticle domain = mock(FaqArticle.class);
        FaqArticleEntity entity = mock(FaqArticleEntity.class);
        when(entity.toDomain()).thenReturn(domain);
        when(repository.findByIdAndStatus(id, FaqStatus.ACTIVE)).thenReturn(Optional.of(entity));

        Optional<FaqArticle> result = provider.findActiveById(id);

        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenArticleNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndStatus(id, FaqStatus.ACTIVE)).thenReturn(Optional.empty());

        Optional<FaqArticle> result = provider.findActiveById(id);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldFindActiveCategories() {
        FaqCategoryWithCountResponse cat = mock(FaqCategoryWithCountResponse.class);
        when(repository.findActiveCategoriesForRole(Role.CTO, FaqStatus.ACTIVE)).thenReturn(List.of(cat));

        List<FaqCategoryWithCountResponse> result = provider.findActiveCategories(Role.CTO);

        assertEquals(1, result.size());
    }

    @Test
    void shouldMarkHelpful() {
        UUID faqId = UUID.randomUUID();
        provider.markHelpful(faqId, true);
        verify(repository).incrementHelpfulCount(faqId);
        verify(repository, never()).incrementNotHelpfulCount(any());
    }

    @Test
    void shouldMarkNotHelpful() {
        UUID faqId = UUID.randomUUID();
        provider.markHelpful(faqId, false);
        verify(repository).incrementNotHelpfulCount(faqId);
        verify(repository, never()).incrementHelpfulCount(any());
    }
}
