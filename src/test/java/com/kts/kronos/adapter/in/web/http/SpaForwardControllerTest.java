package com.kts.kronos.adapter.in.web.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaForwardControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SpaForwardController()).build();
    }

    @Test
    void shouldForwardSingleSegmentHtmlRoutesToRoot() throws Exception {
        mockMvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/"));
    }

    @Test
    void shouldForwardNestedHtmlRoutesToRoot() throws Exception {
        mockMvc.perform(get("/lgpd/admin/requests").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/"));
    }

    @Test
    void shouldIgnoreStaticAssetsWithExtension() throws Exception {
        mockMvc.perform(get("/assets/index.js").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());
    }
}
