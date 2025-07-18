package com.kts.kronos.adapter.out.persistence.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.domain.model.Address;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static com.kts.kronos.constants.ApiPaths.API_VIA_CEP;
import static com.kts.kronos.constants.Messages.INTERNAL_SERVER_ERROR;
import static com.kts.kronos.constants.Messages.ZIPCODE_NOT_FOUND;

@Component
public class ViaCepClientImpl implements AddressLookupProvider {

    private final WebClient webClient;
    public ViaCepClientImpl(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl(API_VIA_CEP)
                .build();
    }

    @Override
    public Address lookup(String postalCode) {
        try {
            ViaCepResponse resp = webClient.get()
                    .uri("/{cep}/json", postalCode)
                    .retrieve()
                    .bodyToMono(ViaCepResponse.class)
                    .block();

            if (resp == null || Boolean.TRUE.equals(resp.erro)) {
                throw new ResourceNotFoundException(ZIPCODE_NOT_FOUND + postalCode);
            }
            return new Address(
                    resp.logradouro,
                    null,
                    resp.cep,
                    resp.localidade,
                    resp.uf
            );
        } catch (WebClientResponseException.NotFound e) {
            throw new ResourceNotFoundException(ZIPCODE_NOT_FOUND + postalCode);
        } catch (Exception e) {
            throw new InternalError(INTERNAL_SERVER_ERROR+ e.getMessage());
        }
    }

    @Getter @Setter
    private static class ViaCepResponse {
        @JsonProperty("cep")
        private String cep;
        @JsonProperty("logradouro")
        private String logradouro;
        @JsonProperty("localidade")
        private String localidade;
        @JsonProperty("uf")
        private String uf;
        @JsonProperty("erro")
        private Boolean erro;
    }
}
