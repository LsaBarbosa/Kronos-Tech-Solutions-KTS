package com.kts.kronos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebClientConfigTest {

    @Test
    @DisplayName("webClientBuilder: deve retornar builder utilizavel")
    void shouldCreateUsableWebClientBuilder() {
        WebClient.Builder builder = new WebClientConfig().webClientBuilder();

        assertNotNull(builder);
        assertNotNull(builder.build());
    }
}
