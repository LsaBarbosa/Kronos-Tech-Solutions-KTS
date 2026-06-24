package com.kts.kronos.adapter.faq;

import com.kts.kronos.adapter.in.web.dto.faq.FaqArticleResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqContextualResponse;
import com.kts.kronos.adapter.in.web.dto.faq.FaqSearchResponse;
import com.kts.kronos.adapter.in.web.http.FaqController;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.FaqUseCase;
import com.kts.kronos.constants.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FaqController.class)
class FaqControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FaqUseCase faqUseCase;

    @MockitoBean
    JwtAuthenticatedUser currentUser;

    // -----------------------------------------------------------------------
    // Anonymous user must be rejected
    // -----------------------------------------------------------------------

    @Test
    void search_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(get(ApiPaths.FAQS + ApiPaths.FAQ_SEARCH)
                        .param("query", "ajuda")
                        .with(anonymous()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void contextual_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(get(ApiPaths.FAQS + ApiPaths.FAQ_CONTEXTUAL)
                        .param("screen", "DASHBOARD")
                        .with(anonymous()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getById_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(get(ApiPaths.FAQS + "/" + UUID.randomUUID())
                        .with(anonymous()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void categories_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(get(ApiPaths.FAQS + ApiPaths.FAQ_CATEGORIES)
                        .with(anonymous()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void markHelpful_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(post(ApiPaths.FAQS + "/" + UUID.randomUUID() + "/helpful")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpful\":true}")
                        .with(anonymous())
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    // -----------------------------------------------------------------------
    // Authenticated user (PARTNER) gets correct responses
    // -----------------------------------------------------------------------

    @Test
    void search_shouldReturnOkForAuthenticatedUser() throws Exception {
        when(faqUseCase.search(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(new FaqSearchResponse(List.of(), 0, 10, 0L, 0));

        mockMvc.perform(get(ApiPaths.FAQS + ApiPaths.FAQ_SEARCH)
                        .param("query", "ajuda")
                        .with(user("partner").roles("PARTNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void contextual_shouldReturnOkForAuthenticatedUser() throws Exception {
        when(faqUseCase.getContextual(anyString(), anyInt()))
                .thenReturn(new FaqContextualResponse("DASHBOARD", List.of()));

        mockMvc.perform(get(ApiPaths.FAQS + ApiPaths.FAQ_CONTEXTUAL)
                        .param("screen", "DASHBOARD")
                        .with(user("partner").roles("PARTNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screen").value("DASHBOARD"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void categories_shouldReturnOkForAuthenticatedUser() throws Exception {
        var catId = UUID.randomUUID();
        when(faqUseCase.getCategories())
                .thenReturn(List.of(new FaqCategoryWithCountResponse(catId, "Geral", 3L)));

        mockMvc.perform(get(ApiPaths.FAQS + ApiPaths.FAQ_CATEGORIES)
                        .with(user("partner").roles("PARTNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Geral"))
                .andExpect(jsonPath("$[0].faqCount").value(3));
    }

    @Test
    void getById_shouldReturnOkForAuthenticatedUser() throws Exception {
        var id = UUID.randomUUID();
        when(faqUseCase.getById(any()))
                .thenReturn(new FaqArticleResponse(id, "Título", "Curta", "Completa", null, List.of(), List.of(), null));

        mockMvc.perform(get(ApiPaths.FAQS + "/" + id)
                        .with(user("partner").roles("PARTNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Título"));
    }

    @Test
    void markHelpful_shouldReturn204ForAuthenticatedUser() throws Exception {
        var id = UUID.randomUUID();
        doNothing().when(faqUseCase).markHelpful(any(), anyBoolean());

        mockMvc.perform(post(ApiPaths.FAQS + "/" + id + "/helpful")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"helpful\":true}")
                        .with(user("partner").roles("PARTNER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
