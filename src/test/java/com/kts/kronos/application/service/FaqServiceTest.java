package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.FaqProvider;
import com.kts.kronos.domain.model.FaqArticle;
import com.kts.kronos.domain.model.FaqCategory;
import com.kts.kronos.domain.model.enuns.FaqStatus;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    @InjectMocks
    private FaqService service;

    @Mock
    private FaqProvider faqProvider;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    // -----------------------------------------------------------------------
    // search
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("search: retorna apenas FAQs ativos para a role do usuário autenticado")
    void search_returnsActiveArticlesForAuthenticatedRole() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        var article = activeArticle(List.of(Role.PARTNER));
        var page = new PageImpl<>(List.of(article), PageRequest.of(0, 10), 1);
        when(faqProvider.search(eq("ajuda"), isNull(), eq(Role.PARTNER), any())).thenReturn(page);

        var result = service.search("ajuda", null, 0, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("search: retorna lista vazia quando não há FAQs para a role")
    void search_returnsEmptyWhenNoArticlesForRole() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        var page = new PageImpl<FaqArticle>(List.of(), PageRequest.of(0, 10), 0);
        when(faqProvider.search(anyString(), any(), eq(Role.PARTNER), any())).thenReturn(page);

        var result = service.search("cto admin", null, 0, 10);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    @DisplayName("search: PARTNER não recebe FAQs exclusivos de MANAGER")
    void search_partnerDoesNotReceiveManagerArticles() {
        // Provider is the gate: it only returns articles for the given role.
        // The service trusts the provider. We verify the role passed to provider is PARTNER.
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        var page = new PageImpl<FaqArticle>(List.of(), PageRequest.of(0, 10), 0);
        when(faqProvider.search(anyString(), any(), eq(Role.PARTNER), any())).thenReturn(page);

        var result = service.search("colaborador", null, 0, 10);

        // MANAGER-only articles do not appear because provider is called with PARTNER role
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("search: MANAGER não recebe FAQs exclusivos de CTO")
    void search_managerDoesNotReceiveCtoArticles() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        var page = new PageImpl<FaqArticle>(List.of(), PageRequest.of(0, 10), 0);
        when(faqProvider.search(anyString(), any(), eq(Role.MANAGER), any())).thenReturn(page);

        var result = service.search("criar empresa", null, 0, 10);

        assertThat(result.items()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getContextual
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getContextual: retorna FAQs da tela e role corretas")
    void getContextual_returnsArticlesForScreenAndRole() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        var article = activeArticle(List.of(Role.PARTNER));
        when(faqProvider.findByScreenAndRole("DOCUMENTS", Role.PARTNER, 5))
                .thenReturn(List.of(article));

        var result = service.getContextual("DOCUMENTS", 5);

        assertThat(result.screen()).isEqualTo("DOCUMENTS");
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("getContextual: retorna lista vazia quando não há FAQs para a tela e role")
    void getContextual_returnsEmptyForUnrelatedScreen() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findByScreenAndRole("COMPANIES", Role.PARTNER, 5))
                .thenReturn(List.of());

        var result = service.getContextual("COMPANIES", 5);

        assertThat(result.items()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getById
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getById: retorna FAQ quando existe, está ativo e role tem permissão")
    void getById_returnsArticleWhenActiveAndRoleAllowed() {
        var id = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findActiveById(id))
                .thenReturn(Optional.of(activeArticle(id, List.of(Role.PARTNER))));

        var result = service.getById(id);

        assertThat(result.id()).isEqualTo(id);
    }

    @Test
    @DisplayName("getById: retorna 404 para FAQ inativo")
    void getById_throws404ForInactiveOrMissingFaq() {
        var id = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findActiveById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
    }

    @Test
    @DisplayName("getById: retorna 403 quando role não tem permissão")
    void getById_throws403WhenRoleNotAllowed() {
        var id = UUID.randomUUID();
        // Article is allowed for CTO only
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findActiveById(id))
                .thenReturn(Optional.of(activeArticle(id, List.of(Role.CTO))));

        assertThrows(ForbiddenException.class, () -> service.getById(id));
    }

    @Test
    @DisplayName("getById: PARTNER não acessa FAQ de MANAGER")
    void getById_partnerCannotAccessManagerOnlyFaq() {
        var id = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findActiveById(id))
                .thenReturn(Optional.of(activeArticle(id, List.of(Role.MANAGER))));

        assertThrows(ForbiddenException.class, () -> service.getById(id));
    }

    @Test
    @DisplayName("getById: MANAGER não acessa FAQ de CTO")
    void getById_managerCannotAccessCtoOnlyFaq() {
        var id = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(faqProvider.findActiveById(id))
                .thenReturn(Optional.of(activeArticle(id, List.of(Role.CTO))));

        assertThrows(ForbiddenException.class, () -> service.getById(id));
    }

    @Test
    @DisplayName("getById: CTO acessa FAQ com allowedRoles contendo CTO")
    void getById_ctoCanAccessCtoFaq() {
        var id = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(faqProvider.findActiveById(id))
                .thenReturn(Optional.of(activeArticle(id, List.of(Role.CTO))));

        var result = service.getById(id);
        assertThat(result.id()).isEqualTo(id);
    }

    // -----------------------------------------------------------------------
    // getCategories
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getCategories: retorna categorias com FAQs ativos para a role do usuário autenticado")
    void getCategories_returnsCategoriesForAuthenticatedRole() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        var catId = UUID.randomUUID();
        when(faqProvider.findActiveCategories(Role.PARTNER))
                .thenReturn(List.of(new FaqCategoryWithCountResponse(catId, "Geral", 2L)));

        var result = service.getCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Geral");
        assertThat(result.get(0).faqCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getCategories: retorna lista vazia quando não há categorias para a role")
    void getCategories_returnsEmptyWhenNoCategories() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findActiveCategories(Role.PARTNER)).thenReturn(List.of());

        var result = service.getCategories();

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // markHelpful
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("markHelpful: registra feedback quando article existe, está ativo e role tem permissão")
    void markHelpful_succeedsWhenActiveAndRoleAllowed() {
        var id = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findActiveById(id))
                .thenReturn(Optional.of(activeArticle(id, List.of(Role.PARTNER))));
        doNothing().when(faqProvider).markHelpful(id, true);

        service.markHelpful(id, true);

        verify(faqProvider).markHelpful(id, true);
    }

    @Test
    @DisplayName("markHelpful: lança 404 quando FAQ não existe ou está inativo")
    void markHelpful_throws404ForMissingFaq() {
        var id = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findActiveById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markHelpful(id, true));
    }

    @Test
    @DisplayName("markHelpful: lança 403 quando role não tem permissão")
    void markHelpful_throws403WhenRoleNotAllowed() {
        var id = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(faqProvider.findActiveById(id))
                .thenReturn(Optional.of(activeArticle(id, List.of(Role.CTO))));

        assertThrows(ForbiddenException.class, () -> service.markHelpful(id, true));
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private FaqArticle activeArticle(List<Role> roles) {
        return activeArticle(UUID.randomUUID(), roles);
    }

    private FaqArticle activeArticle(UUID id, List<Role> roles) {
        var category = new FaqCategory(UUID.randomUUID(), "Categoria", null, LocalDateTime.now(), LocalDateTime.now());
        return new FaqArticle(
                id,
                "Título de teste",
                "Resposta curta de teste.",
                "Resposta completa de teste com mais detalhes.",
                FaqStatus.ACTIVE,
                1,
                category,
                roles,
                List.of("DASHBOARD"),
                List.of("ajuda", "teste"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                null
        );
    }
}
