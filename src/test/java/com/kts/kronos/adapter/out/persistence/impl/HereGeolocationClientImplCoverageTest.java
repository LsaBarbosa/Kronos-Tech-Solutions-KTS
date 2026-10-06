package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.domain.model.Address;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class HereGeolocationClientImplCoverageTest {

    // ── L91: response.items != null && !isEmpty() = FALSE (empty list) ───────────

    @Test
    void resolve_whenResponseItemsIsEmptyList_throwsResourceNotFoundException() {
        // items != null but isEmpty() = TRUE → position = null → throws
        HereGeolocationClientImpl client = clientWithResponse(
            HttpStatus.OK, "{\"items\":[]}", "test-key");

        assertThrows(ResourceNotFoundException.class,
            () -> client.resolve(new Address("Rua", "123", "01001000", "São Paulo", "SP")));
    }

    // ── L110-116: catch(WebClientResponseException.NotFound) ────────────────────

    @Test
    void resolve_when404Response_throwsResourceNotFoundException() {
        HereGeolocationClientImpl client = clientWithResponse(HttpStatus.NOT_FOUND, "", "test-key");

        assertThrows(ResourceNotFoundException.class,
            () -> client.resolve(new Address("Rua", "123", "01001000", "São Paulo", "SP")));
    }

    // ── L91: response.items == null → position = null → throws ──────────────────

    @Test
    void resolve_whenResponseItemsIsNull_throwsResourceNotFoundException() {
        HereGeolocationClientImpl client = clientWithResponse(HttpStatus.OK, "{}", "test-key");

        assertThrows(ResourceNotFoundException.class,
            () -> client.resolve(new Address("Rua", "123", "01001000", "São Paulo", "SP")));
    }

    // ── L95: position.lat == null → throws ──────────────────────────────────────

    @Test
    void resolve_whenPositionLatIsNull_throwsResourceNotFoundException() {
        HereGeolocationClientImpl client = clientWithResponse(HttpStatus.OK,
            "{\"items\":[{\"position\":{\"lng\":-43.1}}]}", "test-key");

        assertThrows(ResourceNotFoundException.class,
            () -> client.resolve(new Address("Rua", "123", "01001000", "São Paulo", "SP")));
    }

    // ── L95: position.lng == null → throws ──────────────────────────────────────

    @Test
    void resolve_whenPositionLngIsNull_throwsResourceNotFoundException() {
        HereGeolocationClientImpl client = clientWithResponse(HttpStatus.OK,
            "{\"items\":[{\"position\":{\"lat\":-22.5}}]}", "test-key");

        assertThrows(ResourceNotFoundException.class,
            () -> client.resolve(new Address("Rua", "123", "01001000", "São Paulo", "SP")));
    }

    private static HereGeolocationClientImpl clientWithResponse(HttpStatus status, String body, String apiKey) {
        WebClient.Builder builder = WebClient.builder()
            .exchangeFunction(request -> Mono.just(ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build()));

        return new HereGeolocationClientImpl(builder, "https://geocode.search.hereapi.com", apiKey);
    }
    // ── L91 branch 1: response == null ──────────────────────────────────────────

    @Test
    void resolve_whenResponseIsNull_throwsResourceNotFoundException() {
        // JSON "null" → bodyToMono returns null → kronosTracing.observe returns null
        // → response == null → position = null → throws ResourceNotFoundException
        HereGeolocationClientImpl client = clientWithResponse(HttpStatus.OK, "null", "test-key");

        assertThrows(ResourceNotFoundException.class,
            () -> client.resolve(new Address("Rua", "123", "01001000", "São Paulo", "SP")));
    }

}
