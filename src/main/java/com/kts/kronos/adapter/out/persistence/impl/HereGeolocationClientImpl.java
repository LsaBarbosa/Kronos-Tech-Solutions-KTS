package com.kts.kronos.adapter.out.persistence.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.GeolocationProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Autowired
    public HereGeolocationClientImpl(
            WebClient.Builder builder,
            @Value("${integration.here.base-url:https://geocode.search.hereapi.com}") String baseUrl,
            @Value("${integration.here.api-key:}") String apiKey,
            KronosMetrics kronosMetrics,
            KronosTracing kronosTracing
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.kronosMetrics = kronosMetrics;
        this.kronosTracing = kronosTracing;
    }

    public HereGeolocationClientImpl(
            WebClient.Builder builder,
            String baseUrl,
            String apiKey
    ) {
        this(builder, baseUrl, apiKey, ObservabilityDefaults.metrics(), ObservabilityDefaults.tracing());
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

        log.info("event=here_geocode_lookup postalCode={} city={} state={}",
                address.postalCode(), address.city(), address.state());
        long startedAt = System.nanoTime();

        try {
            HereGeocodeResponse response = kronosTracing.observe("kronos.external.here", () -> webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/geocode")
                            .queryParam("q", query)
                            .queryParam("apiKey", apiKey)
                            .queryParam("in", "countryCode:BRA")
                            .build())
                    .retrieve()
                    .bodyToMono(HereGeocodeResponse.class)
                    .block(java.time.Duration.ofSeconds(45)), "provider", "here", "operation", "geocode");

            HerePosition position = response != null && response.items != null && !response.items.isEmpty()
                    ? response.items.get(0).position
                    : null;

            if (position == null || position.lat == null || position.lng == null) {
                kronosMetrics.recordExternalProviderRequest("here", "geocode", "failure", "not_found");
                kronosMetrics.recordExternalProviderRequestDuration("here", "geocode",
                        java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
                log.warn("event=here_geocode_lookup result=failure reason=not_found postalCode={} city={} state={}",
                        address.postalCode(), address.city(), address.state());
                throw new ResourceNotFoundException(GEOLOCATION_NOT_FOUND);
            }

            kronosMetrics.recordExternalProviderRequest("here", "geocode", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("here", "geocode",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
            return new Location(round(position.lat), round(position.lng));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (WebClientResponseException.NotFound e) {
            kronosMetrics.recordExternalProviderRequest("here", "geocode", "failure", "not_found");
            kronosMetrics.recordExternalProviderRequestDuration("here", "geocode",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.warn("event=here_geocode_lookup result=failure reason=provider_not_found postalCode={} city={} state={}",
                    address.postalCode(), address.city(), address.state());
            throw new ResourceNotFoundException(GEOLOCATION_NOT_FOUND);
        } catch (WebClientResponseException | WebClientRequestException e) {
            kronosMetrics.recordExternalProviderRequest("here", "geocode", "failure", "provider_error");
            kronosMetrics.recordExternalProviderRequestDuration("here", "geocode",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=here_geocode_lookup result=failure reason=provider_error postalCode={} city={} state={} exception_type={}",
                    address.postalCode(), address.city(), address.state(), e.getClass().getSimpleName(), e);
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
