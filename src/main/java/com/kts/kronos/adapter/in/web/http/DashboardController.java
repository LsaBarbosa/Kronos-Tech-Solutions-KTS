package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.dashboard.DashboardSummaryResponse;
import com.kts.kronos.application.service.DashboardService;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.infrastructure.redis.RedisCacheNames;
import com.kts.kronos.infrastructure.redis.RedisScopeKeyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired(required = false)
    private CacheProvider cacheProvider;

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(cache(
                RedisCacheNames.DASHBOARD_SUMMARY,
                RedisScopeKeyResolver.authenticatedScope(),
                DashboardSummaryResponse.class,
                dashboardService::getDashboardSummary
        ));
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, Supplier<T> loader) {
        if (cacheProvider == null) {
            return loader.get();
        }
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }
}
