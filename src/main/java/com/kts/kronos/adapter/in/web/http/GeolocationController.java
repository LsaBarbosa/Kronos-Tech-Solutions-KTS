package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.geolocation.GeolocationResolveRequest;
import com.kts.kronos.application.port.in.usecase.GeolocationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kts.kronos.constants.ApiPaths.GEOLOCATION;
import static com.kts.kronos.constants.ApiPaths.RESOLVE;
import static com.kts.kronos.constants.Messages.KRONOS;

@RestController
@RequestMapping(GEOLOCATION)
@RequiredArgsConstructor
@Tag(name = "Geolocalização", description = "Resolução segura de coordenadas via backend")
public class GeolocationController {

    private final GeolocationUseCase useCase;

    @PostMapping(RESOLVE)
    @PreAuthorize(KRONOS)
    @Operation(
            summary = "Resolver latitude e longitude por CEP",
            description = "Consulta ViaCEP e HERE no backend para retornar coordenadas sem expor chaves no front-end."
    )
    public ResponseEntity<Location> resolve(@Valid @RequestBody GeolocationResolveRequest request) {
        return ResponseEntity.ok(useCase.resolve(request));
    }
}
