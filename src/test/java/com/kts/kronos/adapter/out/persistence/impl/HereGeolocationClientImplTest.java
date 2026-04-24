package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.domain.model.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HereGeolocationClientImplTest {

    @Test
    @DisplayName("resolve: deve mapear resposta HERE para Location")
    void shouldMapHereResponseToLocation() {
        HereGeolocationClientImpl client = clientWithResponse(
                HttpStatus.OK,
                """
                {
                  "items": [
                    {
                      "position": {
                        "lat": -22.5098039,
                        "lng": -43.1775437
                      }
                    }
                  ]
                }
                """,
                "test-key"
        );

        var location = client.resolve(new Address("Rua Central", "123", "25900000", "Petropolis", "RJ"));

        assertEquals(-22.509804, location.latitude());
        assertEquals(-43.177544, location.longitude());
    }

    @Test
    @DisplayName("resolve: deve traduzir lista vazia para ResourceNotFoundException")
    void shouldTranslateEmptyItemsToNotFound() {
        HereGeolocationClientImpl client = clientWithResponse(
                HttpStatus.OK,
                "{\"items\":[]}",
                "test-key"
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> client.resolve(new Address("Rua Central", "123", "25900000", "Petropolis", "RJ"))
        );
    }

    @Test
    @DisplayName("resolve: deve falhar quando a chave HERE não estiver configurada")
    void shouldFailWhenApiKeyIsMissing() {
        HereGeolocationClientImpl client = clientWithResponse(HttpStatus.OK, "{\"items\":[]}", "");

        assertThrows(
                IllegalStateException.class,
                () -> client.resolve(new Address("Rua Central", "123", "25900000", "Petropolis", "RJ"))
        );
    }

    @Test
    @DisplayName("resolve: encapsula falha de conexão como erro interno")
    void shouldWrapRequestFailure() {
        WebClient.Builder builder = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new WebClientRequestException(
                        new IOException("network"),
                        HttpMethod.GET,
                        URI.create("https://geocode.search.hereapi.com/v1/geocode"),
                        HttpHeaders.EMPTY
                )));
        HereGeolocationClientImpl client = new HereGeolocationClientImpl(
                builder,
                "https://geocode.search.hereapi.com",
                "test-key"
        );

        assertThrows(
                IllegalStateException.class,
                () -> client.resolve(new Address("Rua Central", "123", "25900000", "Petropolis", "RJ"))
        );
    }

    private static HereGeolocationClientImpl clientWithResponse(HttpStatus status, String body, String apiKey) {
        WebClient.Builder builder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(status)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build()));

        return new HereGeolocationClientImpl(builder, "https://geocode.search.hereapi.com", apiKey);
    }
}
