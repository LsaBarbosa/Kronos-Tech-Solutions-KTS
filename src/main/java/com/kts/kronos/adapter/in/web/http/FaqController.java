package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.faq.FaqArticleResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqContextualResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqHelpfulRequest;
import com.kts.kronos.adapter.in.web.dto.faq.FaqSearchResponse;
import com.kts.kronos.application.port.in.usecase.FaqUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;

@RestController
@RequestMapping(FAQS)
@RequiredArgsConstructor
public class FaqController {

    private final FaqUseCase useCase;

    /**
     * GET /faqs/search?query=&screen=&page=&size=
     *
     * Searches FAQ articles accessible to the authenticated user.
     * Role is extracted from the JWT — NEVER accepted as a parameter.
     */
    @GetMapping(FAQ_SEARCH)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<FaqSearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String screen,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var safePage = Math.max(0, page);
        var safeSize = Math.min(Math.max(1, size), 50);
        return ResponseEntity.ok(useCase.search(query, screen, safePage, safeSize));
    }

    /**
     * GET /faqs/contextual?screen=&limit=
     *
     * Returns FAQ articles contextually related to the given screen,
     * filtered by the authenticated user's role.
     */
    @GetMapping(FAQ_CONTEXTUAL)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<FaqContextualResponse> contextual(
            @RequestParam String screen,
            @RequestParam(defaultValue = "5") int limit
    ) {
        var safeLimit = Math.min(Math.max(1, limit), 20);
        return ResponseEntity.ok(useCase.getContextual(screen, safeLimit));
    }

    /**
     * GET /faqs/categories
     *
     * Returns categories that have at least one active FAQ accessible to the authenticated user's role.
     * Role is extracted from the JWT — NEVER accepted as a parameter.
     */
    @GetMapping(FAQ_CATEGORIES)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<List<FaqCategoryWithCountResponse>> getCategories() {
        return ResponseEntity.ok(useCase.getCategories());
    }

    /**
     * GET /faqs/{faqId}
     *
     * Returns a single FAQ article by ID.
     * Validates: exists, ACTIVE, and role has permission.
     */
    @GetMapping(FAQ_BY_ID)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<FaqArticleResponse> getById(@PathVariable UUID faqId) {
        return ResponseEntity.ok(useCase.getById(faqId));
    }

    /**
     * POST /faqs/{faqId}/helpful
     *
     * Records whether the authenticated user found the article helpful.
     * Validates: article exists, is ACTIVE, and role has permission.
     * Returns 204 No Content on success.
     */
    @PostMapping(FAQ_HELPFUL)
    @PreAuthorize(ANY_EMPLOYEE)
    public ResponseEntity<Void> markHelpful(
            @PathVariable UUID faqId,
            @Valid @RequestBody FaqHelpfulRequest request
    ) {
        useCase.markHelpful(faqId, request.helpful());
        return ResponseEntity.noContent().build();
    }
}
