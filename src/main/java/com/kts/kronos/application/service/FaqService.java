package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.faq.FaqArticleResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqContextualResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqSearchItemResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqSearchResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.FaqUseCase;
import com.kts.kronos.application.port.out.provider.FaqProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService implements FaqUseCase {

    private static final String FAQ_NOT_FOUND = "FAQ não encontrado.";
    private static final String FAQ_ACCESS_DENIED = "Você não tem permissão para acessar este FAQ.";

    private final FaqProvider faqProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @Override
    public FaqSearchResponse search(String query, String screen, int page, int size) {
        // Role is ALWAYS extracted from the authenticated context — never from the caller.
        var role = jwtAuthenticatedUser.getCurrentRole();
        var pageable = PageRequest.of(page, size);
        var result = faqProvider.search(query, screen, role, pageable);

        var items = result.getContent().stream()
                .map(FaqSearchItemResponse::fromDomain)
                .toList();

        return new FaqSearchResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public FaqContextualResponse getContextual(String screen, int limit) {
        // Role is ALWAYS extracted from the authenticated context — never from the caller.
        var role = jwtAuthenticatedUser.getCurrentRole();
        var articles = faqProvider.findByScreenAndRole(screen, role, limit);
        var items = articles.stream()
                .map(FaqSearchItemResponse::fromDomain)
                .toList();
        return new FaqContextualResponse(screen, items);
    }

    @Override
    public FaqArticleResponse getById(UUID faqId) {
        // Role is ALWAYS extracted from the authenticated context — never from the caller.
        var role = jwtAuthenticatedUser.getCurrentRole();

        // Step 1: article must exist and be ACTIVE. Return 404 if not.
        var article = faqProvider.findActiveById(faqId)
                .orElseThrow(() -> new ResourceNotFoundException(FAQ_NOT_FOUND));

        // Step 2: role must be in allowedRoles. Prefer 403 to signal permission issue without
        //         leaking existence (aligned with project pattern of explicit 403 for role mismatch).
        if (!article.allowedRoles().contains(role)) {
            throw new ForbiddenException(FAQ_ACCESS_DENIED);
        }

        return FaqArticleResponse.fromDomain(article);
    }

    @Override
    public List<FaqCategoryWithCountResponse> getCategories() {
        // Role is ALWAYS extracted from the authenticated context — never from the caller.
        var role = jwtAuthenticatedUser.getCurrentRole();
        return faqProvider.findActiveCategories(role);
    }

    @Override
    @Transactional
    public void markHelpful(UUID faqId, boolean helpful) {
        // Role is ALWAYS extracted from the authenticated context — never from the caller.
        var role = jwtAuthenticatedUser.getCurrentRole();

        // Validate: article must exist and be ACTIVE, and role must have permission.
        var article = faqProvider.findActiveById(faqId)
                .orElseThrow(() -> new ResourceNotFoundException(FAQ_NOT_FOUND));

        if (!article.allowedRoles().contains(role)) {
            throw new ForbiddenException(FAQ_ACCESS_DENIED);
        }

        faqProvider.markHelpful(faqId, helpful);
    }
}
