package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.geolocation.GeolocationResolveRequest;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.application.port.in.usecase.GeolocationUseCase;
import com.kts.kronos.infrastructure.redis.RedisCacheNames;
import com.kts.kronos.infrastructure.redis.RedisScopeKeyResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

import static com.kts.kronos.constants.ApiPaths.GEOLOCATION;
import static com.kts.kronos.constants.ApiPaths.RESOLVE;
import static com.kts.kronos.constants.Messages.KRONOS;

@RestController
@RequestMapping(GEOLOCATION)
@RequiredArgsConstructor
@Tag(name = "Geolocalização", description = "Resolução segura de coordenadas via backend")
public class GeolocationController {

    private final GeolocationUseCase useCase;

    @Autowired(required = false)
    private CacheProvider cacheProvider;

    @PostMapping(RESOLVE)
    @PreAuthorize(KRONOS)
    @Operation(
            summary = "Resolver latitude e longitude por CEP",
            description = "Consulta ViaCEP e HERE no backend para retornar coordenadas sem expor chaves no front-end."
    )
    public ResponseEntity<Location> resolve(@Valid @RequestBody GeolocationResolveRequest request) {
        return ResponseEntity.ok(cache(
                RedisCacheNames.GEOLOCATION_RESOLVE,
                RedisScopeKeyResolver.authenticatedScope(
                        "postalCode=" + request.postalCode(),
                        "number=" + request.number()
                ),
                Location.class,
                () -> useCase.resolve(request)
        ));
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, Supplier<T> loader) {
        if (cacheProvider == null) {
            return loader.get();
        }
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }
}
