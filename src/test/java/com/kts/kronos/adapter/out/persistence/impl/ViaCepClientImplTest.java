package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ViaCepClientImplTest {

    @Test
    @DisplayName("lookup: deve mapear resposta ViaCEP para Address")
    void shouldMapViaCepResponseToAddress() {
        ViaCepClientImpl client = clientWithResponse(
                HttpStatus.OK,
                """
                {
                  "cep": "01001-000",
                  "logradouro": "Praca da Se",
                  "localidade": "Sao Paulo",
                  "uf": "SP"
                }
                """
        );

        var address = client.lookup("01001000");

        assertEquals("Praca da Se", address.street());
        assertEquals("01001-000", address.postalCode());
        assertEquals("Sao Paulo", address.city());
        assertEquals("SP", address.state());
    }

    @Test
    @DisplayName("lookup: deve traduzir 404 para ResourceNotFoundException")
    void shouldTranslateNotFoundStatus() {
        ViaCepClientImpl client = clientWithResponse(HttpStatus.NOT_FOUND, "");

        assertThrows(ResourceNotFoundException.class, () -> client.lookup("00000000"));
    }

    @Test
    @DisplayName("lookup: deve traduzir erro=true para ResourceNotFoundException")
    void shouldTranslateViaCepErrorFlag() {
        ViaCepClientImpl client = clientWithResponse(HttpStatus.OK, "{\"erro\": true}");

        assertThrows(ResourceNotFoundException.class, () -> client.lookup("00000000"));
    }

    @Test
    @DisplayName("lookup: deve encapsular resposta malformada como Decoding Error")
    void shouldWrapMalformedResponse() {
        ViaCepClientImpl client = clientWithResponse(HttpStatus.OK, "{invalid-json");

        assertThrows(DecodingException.class, () -> client.lookup("01001000"));
    }

    private static ViaCepClientImpl clientWithResponse(HttpStatus status, String body) {
        WebClient.Builder builder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(status)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build()));
        return new ViaCepClientImpl(builder);
    }
}
