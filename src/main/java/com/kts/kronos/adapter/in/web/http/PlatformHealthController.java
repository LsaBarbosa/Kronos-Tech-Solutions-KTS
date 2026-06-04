package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.platform.PlatformHealthResponse;
import com.kts.kronos.application.service.PlatformHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kts.kronos.constants.ApiPaths.ADMIN_PLATFORM;
import static com.kts.kronos.constants.ApiPaths.HEALTH;
import static com.kts.kronos.constants.Messages.KRONOS;

@RestController
@RequestMapping(ADMIN_PLATFORM)
@RequiredArgsConstructor
public class PlatformHealthController {

    private final PlatformHealthService platformHealthService;

    @PreAuthorize(KRONOS)
    @GetMapping(HEALTH)
    public ResponseEntity<PlatformHealthResponse> getPlatformHealth() {
        return ResponseEntity.ok(platformHealthService.getPlatformHealth());
    }
}
