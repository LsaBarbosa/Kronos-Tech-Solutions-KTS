package com.kts.kronos.adapter.out.persistence.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.GeolocationProvider;
import com.kts.kronos.domain.model.Address;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.kts.kronos.constants.Messages.GEOLOCATION_NOT_FOUND;
import static com.kts.kronos.constants.Messages.GEOLOCATION_SERVICE_UNAVAILABLE;
import static com.kts.kronos.constants.Messages.HERE_API_KEY_NOT_CONFIGURED;

@Slf4j
@Component
public class HereGeolocationClientImpl implements GeolocationProvider {

    private final WebClient webClient;
    private final String apiKey;

    public HereGeolocationClientImpl(
            WebClient.Builder builder,
            @Value("${integration.here.base-url:https://geocode.search.hereapi.com}") String baseUrl,
            @Value("${integration.here.api-key:}") String apiKey
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public Location resolve(Address address) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(HERE_API_KEY_NOT_CONFIGURED);
        }

        String query = String.format(
                "%s, %s, %s, %s, Brasil",
                address.street(),
                address.number(),
                address.city(),
                address.state()
        );

        log.info("Consultando HERE Geocoding. postalCode={}, city={}", address.postalCode(), address.city());

        try {
            HereGeocodeResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/geocode")
                            .queryParam("q", query)
                            .queryParam("apiKey", apiKey)
                            .queryParam("in", "countryCode:BRA")
                            .build())
                    .retrieve()
                    .bodyToMono(HereGeocodeResponse.class)
                    .block();

            HerePosition position = response != null && response.items != null && !response.items.isEmpty()
                    ? response.items.get(0).position
                    : null;

            if (position == null || position.lat == null || position.lng == null) {
                log.warn("Localização não encontrada no HERE. postalCode={}, query={}", address.postalCode(), query);
                throw new ResourceNotFoundException(GEOLOCATION_NOT_FOUND);
            }

            return new Location(round(position.lat), round(position.lng));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("HERE retornou 404 para query={}", query);
            throw new ResourceNotFoundException(GEOLOCATION_NOT_FOUND);
        } catch (WebClientResponseException | WebClientRequestException e) {
            log.error("Falha ao consultar HERE Geocoding. query={}", query, e);
            throw new IllegalStateException(GEOLOCATION_SERVICE_UNAVAILABLE, e);
        }
    }

    private static double round(Double value) {
        return BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Getter
    @Setter
    private static class HereGeocodeResponse {
        @JsonProperty("items")
        private List<HereItem> items;
    }

    @Getter
    @Setter
    private static class HereItem {
        @JsonProperty("position")
        private HerePosition position;
    }

    @Getter
    @Setter
    private static class HerePosition {
        @JsonProperty("lat")
        private Double lat;
        @JsonProperty("lng")
        private Double lng;
    }
}
