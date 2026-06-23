package com.kts.kronos.adapter.demo;

import com.kts.kronos.adapter.in.web.http.CtoDemoController;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.service.demo.DemoSandboxService;
import com.kts.kronos.constants.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CtoDemoController.class)
class CtoDemoControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DemoSandboxService demoService;

    @MockitoBean
    JwtAuthenticatedUser currentUser;

    @Test
    void createEndpoint_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(post(ApiPaths.CTO_DEMO + ApiPaths.CTO_DEMO_CREATE)
                        .with(anonymous()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteEndpoint_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(delete(ApiPaths.CTO_DEMO)
                        .with(anonymous()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void statusEndpoint_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(get(ApiPaths.CTO_DEMO + ApiPaths.CTO_DEMO_STATUS)
                        .with(anonymous()))
                .andExpect(status().is4xxClientError());
    }
}
