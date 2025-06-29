package com.kts.kronos.adapter.out.persistence.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kts.kronos.app.exceptions.ResourceNotFoundException;
import com.kts.kronos.app.port.out.repository.AddressLookupPort;
import com.kts.kronos.domain.model.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ViaCepClientAdapter implements AddressLookupPort {

    private final WebClient webClient;
    public ViaCepClientAdapter(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://viacep.com.br/ws")
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
                throw new ResourceNotFoundException("CEP não encontrado: " + postalCode);
            }
            return new Address(
                    resp.logradouro,
                    null,
                    resp.cep,
                    resp.localidade,
                    resp.uf
            );
        } catch (WebClientResponseException.NotFound e) {
            throw new ResourceNotFoundException("CEP não encontrado: " + postalCode);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar ViaCEP: " + e.getMessage());
        }
    }

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

        // getters e setters (pode usar Lombok ou IDE para gerar)
        public String getCep() { return cep; }
        public void setCep(String cep) { this.cep = cep; }
        public String getLogradouro() { return logradouro; }
        public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
        public String getLocalidade() { return localidade; }
        public void setLocalidade(String localidade) { this.localidade = localidade; }
        public String getUf() { return uf; }
        public void setUf(String uf) { this.uf = uf; }
        public Boolean getErro() { return erro; }
        public void setErro(Boolean erro) { this.erro = erro; }
    }
}
