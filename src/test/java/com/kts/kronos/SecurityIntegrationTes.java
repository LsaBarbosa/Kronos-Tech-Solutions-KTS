package com.kts.kronos;

import com.jayway.jsonpath.JsonPath;
import com.kts.kronos.adapter.in.rest.AuthController;
import com.kts.kronos.adapter.in.rest.TestController;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.adapter.out.persistence.repository.DataUserRepository;
import com.kts.kronos.adapter.out.security.jwt.JwtTokenAdapter;
import com.kts.kronos.adapter.out.security.loginattempt.GuavaLoginAttemptAdapter;
import com.kts.kronos.adapter.out.security.password.BCryptPasswordEncoderAdapter;
import com.kts.kronos.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTes {
    @Autowired
    private MockMvc mvc;

    @Mock
    private DataUserRepository springUserRepo; // para o UserJpaAdapter

    @BeforeEach
    void setupUser() {
        // Simula um usuário PARTNER com senha "senha"
        var encoder = new BCryptPasswordEncoder();
        var hash = encoder.encode("senha");
        var entity = new UserEntity();
        entity.setUserId(UUID.randomUUID());
        entity.setUsername("partner1");
        entity.setPassword(hash);
        entity.setRole("PARTNER");
        entity.setEnabled(true);
        when(springUserRepo.findByUsername("partner1"))
                .thenReturn(Optional.of(entity));
    }

    @Test
    void unauthenticatedCannotAccessProtected() throws Exception {
        mvc.perform(get("/test/any"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedCanAccessAny() throws Exception {
        // 1) faz login e captura token
        MvcResult res = mvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"partner1\",\"password\":\"senha\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(res.getResponse().getContentAsString(), "$.token");

        // 2) usa o token no header
        mvc.perform(get("/test/any"))
                .andExpect(status().isForbidden());

    }

    @Test
    void onlyPartnerOrAboveCanAccessPartner() throws Exception {
        // login como partner
        MvcResult res = mvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"partner1\",\"password\":\"senha\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(res.getResponse().getContentAsString(), "$.token");

        // partner acessa
        mvc.perform(get("/test/partner")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("OK PARTNER"));
    }
}
