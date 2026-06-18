package com.kts.kronos.adapter.in.web.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaForwardControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new SpaForwardController("https://app.kronossolutions.tech/");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldRedirectSingleSegmentHtmlRoutesToFrontend() throws Exception {
        mockMvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://app.kronossolutions.tech/dashboard"));
    }

    @Test
    void shouldRedirectNestedHtmlRoutesToFrontend() throws Exception {
        mockMvc.perform(get("/lgpd/admin/requests").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://app.kronossolutions.tech/lgpd/admin/requests"));
    }

    @Test
    void shouldRedirectRootToFrontend() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://app.kronossolutions.tech/"));
    }

    @Test
    void shouldIgnoreStaticAssetsWithExtension() throws Exception {
        mockMvc.perform(get("/assets/index.js").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());
    }
}
